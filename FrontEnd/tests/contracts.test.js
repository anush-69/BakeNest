import test, { afterEach } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { request } from "../src/api/client.js";
import { authApi } from "../src/api/authApi.js";
import { productApi } from "../src/api/productApi.js";
import { orderApi } from "../src/api/orderApi.js";
import { reviewApi } from "../src/api/reviewApi.js";
import { adminApi } from "../src/api/adminApi.js";
import { userApi } from "../src/api/userApi.js";
import {
  canCancel,
  nextStatus,
  sanitizeBasket,
  validPassword,
  hasRole,
} from "../src/lib/format.js";

const originalFetch = globalThis.fetch;
const originalWindow = globalThis.window;
afterEach(() => {
  globalThis.fetch = originalFetch;
  globalThis.window = originalWindow;
});
const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
function mockFetch(responses) {
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url, ...options });
    const response = responses.shift();
    if (response instanceof Error) throw response;
    assert.ok(response, `Unexpected request to ${url}`);
    return response;
  };
  return calls;
}
const csrf = (token) => json({ headerName: "X-CSRF-TOKEN", token });

test("failed logout is never reported as a successful sign-out", async () => {
  mockFetch([csrf("x"), json({}, 404)]);
  await assert.rejects(authApi.logout(), { status: 404 });
});

test("account input patterns compile under HTML's Unicode v mode", () => {
  const source = readFileSync(
    new URL("../src/components/forms/AccountFields.jsx", import.meta.url),
    "utf8",
  );
  const patterns = [...source.matchAll(/pattern="([^"]+)"/g)].map(
    (match) => new RegExp(`^(?:${match[1]})$`, "v"),
  );
  assert.equal(patterns.length, 2);
  assert.ok(patterns[0].test("+94 (77) 123-4567"));
  assert.equal(patterns[0].test("phone number"), false);
  assert.ok(patterns[1].test("bake_nest.1"));
  assert.equal(patterns[1].test("space name"), false);
});

test("product search uses actual name and categoryId parameters and includes session credentials", async () => {
  const calls = mockFetch([json([])]);
  await productApi.list({ name: "cake & bread", categoryId: "4" });
  assert.equal(calls[0].url, "/api/products?name=cake+%26+bread&categoryId=4");
  assert.equal(calls[0].credentials, "include");
  assert.equal(calls[0].method, "GET");
});
test("orders fetch CSRF first and submit quantities without client prices or customer identity", async () => {
  const calls = mockFetch([
    csrf("token-1"),
    json({ id: 81, total: 2600 }, 201),
  ]);
  const order = await orderApi.create({
    items: [{ productId: 4, quantity: 2 }],
    customerNote: "Thank you",
  });
  assert.equal(calls[0].url, "/api/csrf");
  assert.equal(calls[1].url, "/api/orders");
  assert.equal(calls[1].headers["X-CSRF-TOKEN"], "token-1");
  assert.deepEqual(JSON.parse(calls[1].body), {
    items: [{ productId: 4, quantity: 2 }],
    customerNote: "Thank you",
  });
  assert.equal(order.total, 2600);
});
test("each mutation retrieves a fresh CSRF token and never retries an unsafe write", async () => {
  const calls = mockFetch([
    csrf("a"),
    json({ message: "Invalid order status transition." }, 409),
    csrf("b"),
    json({ status: "CONFIRMED" }),
  ]);
  await assert.rejects(orderApi.status(1, "COMPLETED"), {
    status: 409,
    message: "Invalid order status transition.",
  });
  await orderApi.status(1, "CONFIRMED");
  assert.equal(calls.length, 4);
  assert.equal(calls[3].headers["X-CSRF-TOKEN"], "b");
});
test("session login is form encoded and identity is confirmed with /users/me", async () => {
  const redirect = new Response("<html>signed in</html>", {
    headers: { "Content-Type": "text/html" },
  });
  Object.defineProperty(redirect, "url", { value: "http://127.0.0.1:5173/" });
  const account = { id: 1, username: "customer", roles: ["CUSTOMER"] };
  const calls = mockFetch([csrf("session-token"), redirect, json(account)]);
  assert.deepEqual(await authApi.login("customer", "test+password"), account);
  assert.equal(calls[1].url, "/auth/login");
  assert.equal(
    calls[1].headers["Content-Type"],
    "application/x-www-form-urlencoded",
  );
  assert.equal(
    calls[1].body.toString(),
    "username=customer&password=test%2Bpassword",
  );
  assert.equal(calls[2].url, "/api/users/me");
});
test("default Spring login error redirect is reported as incorrect credentials", async () => {
  const response = new Response("<html>login</html>");
  Object.defineProperty(response, "url", {
    value: "http://127.0.0.1:5173/login?error",
  });
  const calls = mockFetch([csrf("x"), response]);
  await assert.rejects(authApi.login("invalid", "invalid"), {
    status: 401,
    message: "Incorrect username or password.",
  });
  assert.equal(calls.length, 2);
});
test("a redirect alone does not authenticate a customer", async () => {
  const response = new Response("<html/>");
  Object.defineProperty(response, "url", { value: "http://127.0.0.1:5173/" });
  mockFetch([csrf("x"), response, json({}, 401)]);
  await assert.rejects(authApi.login("customer", "wrong-password"), {
    status: 401,
  });
});
test("logout uses CSRF-protected POST and tolerates Spring HTML redirect", async () => {
  const calls = mockFetch([csrf("x"), new Response("<html/>")]);
  await authApi.logout();
  assert.equal(calls[1].url, "/auth/logout");
  assert.equal(calls[1].method, "POST");
  assert.equal(calls[1].headers["X-CSRF-TOKEN"], "x");
});
test("protected 401 clears the frontend session through an expiration event", async () => {
  globalThis.window = new EventTarget();
  let expired = false;
  window.addEventListener("bakenest:unauthorized", () => {
    expired = true;
  });
  mockFetch([json({}, 401)]);
  await assert.rejects(orderApi.mine(), { status: 401 });
  assert.ok(expired);
});
test("network failure, non-JSON API responses, and CSRF rejection produce actionable errors", async () => {
  mockFetch([new TypeError("Failed to fetch")]);
  await assert.rejects(productApi.list(), /couldn’t reach the bakery/);
  mockFetch([new Response("<html>Login</html>")]);
  await assert.rejects(request("/products"), /unexpected response/);
  mockFetch([csrf("expired"), json({}, 403)]);
  await assert.rejects(orderApi.cancel(1), { status: 403 });
});
test("review moderation uses status and note, and edits only rating/comment", async () => {
  const calls = mockFetch([
    csrf("a"),
    json({ status: "APPROVED" }),
    csrf("b"),
    json({ status: "PENDING" }),
  ]);
  await reviewApi.moderate(6, { status: "APPROVED", note: "Checked" });
  await reviewApi.update(6, { rating: 4, comment: "Lovely" });
  assert.equal(calls[1].url, "/api/reviews/6/moderation");
  assert.equal(calls[1].method, "PATCH");
  assert.deepEqual(JSON.parse(calls[1].body), {
    status: "APPROVED",
    note: "Checked",
  });
  assert.equal(calls[3].method, "PUT");
});
test("customer and administrator delete endpoints accept successful 204 responses", async () => {
  const calls = mockFetch([
    csrf("a"),
    new Response(null, { status: 204 }),
    csrf("b"),
    new Response(null, { status: 204 }),
  ]);
  assert.equal(await userApi.delete(3), null);
  assert.equal(await adminApi.delete(4), null);
  assert.equal(calls[1].url, "/api/users/3");
  assert.equal(calls[3].url, "/api/admins/4");
});
test("order transitions match the backend state machine", () => {
  assert.ok(canCancel("PENDING"));
  assert.ok(canCancel("CONFIRMED"));
  assert.equal(canCancel("PREPARING"), false);
  assert.equal(nextStatus("PENDING"), "CONFIRMED");
  assert.equal(nextStatus("READY"), "COMPLETED");
  assert.equal(nextStatus("CANCELLED"), undefined);
  assert.equal(nextStatus("COMPLETED"), undefined);
});
test("stored basket rejects invalid IDs, duplicate items, out-of-range quantities and extra fields", () => {
  assert.deepEqual(
    sanitizeBasket([
      { productId: 2, quantity: 3, price: 1 },
      { productId: 2, quantity: 1 },
      { productId: -1, quantity: 2 },
      { productId: 3, quantity: 10001 },
      { productId: 4, quantity: 1.5 },
    ]),
    [{ productId: 2, quantity: 3 }],
  );
  assert.deepEqual(sanitizeBasket({}), []);
});
test("password byte limits and backend role names are respected", () => {
  assert.equal(validPassword("12345678"), true);
  assert.equal(validPassword("short"), false);
  assert.equal(validPassword("🎂".repeat(19)), false);
  assert.equal(hasRole({ roles: ["ADMIN"] }, "ADMIN"), true);
  assert.equal(hasRole({ roles: ["CUSTOMER"] }, "ADMIN"), false);
});
