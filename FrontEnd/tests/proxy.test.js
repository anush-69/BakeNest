import test from "node:test";
import assert from "node:assert/strict";
import viteConfig from "../vite.config.js";

test("development and preview proxy verified backend API and form endpoints", () => {
  const config = viteConfig({ mode: "test" });
  assert.equal(config.server.port, 5173);
  assert.equal(config.server.strictPort, true);
  assert.equal(config.server.proxy["/api"].target, "http://localhost:8080");
  assert.equal(config.server.proxy["/auth"].rewrite("/auth/login"), "/login");
  assert.equal(config.server.proxy["/auth"].rewrite("/auth/logout"), "/logout");
  assert.equal(config.preview.proxy, config.server.proxy);
});
test("absolute Spring session redirects stay on the frontend origin", () => {
  const config = viteConfig({ mode: "test" });
  let onResponse;
  config.server.proxy["/auth"].configure({
    on(event, callback) {
      assert.equal(event, "proxyRes");
      onResponse = callback;
    },
  });
  const response = {
    headers: { location: "http://localhost:8080/api/users/me?continue" },
  };
  onResponse(response);
  assert.equal(response.headers.location, "/api/users/me?continue");
  const error = { headers: { location: "http://localhost:8080/login?error" } };
  onResponse(error);
  assert.equal(error.headers.location, "/login?error");
});
