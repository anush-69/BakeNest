export const API_BASE = (import.meta.env?.VITE_API_BASE_URL || "/api").replace(
  /\/$/,
  "",
);
export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}
async function transport(url, options = {}) {
  try {
    return await fetch(url, {
      credentials: "include",
      ...options,
      signal: options.signal || AbortSignal.timeout(15000),
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new ApiError(
      "We couldn’t reach the bakery. Please try again in a moment.",
    );
  }
}
async function parse(response) {
  const json = response.headers
    .get("content-type")
    ?.includes("application/json");
  const body = json ? await response.json() : null;
  if (!response.ok) {
    const fallback =
      response.status === 401
        ? "Please sign in to continue."
        : response.status === 403
          ? "This action is not permitted, or your security token has expired. Refresh and try again."
          : response.status === 404
            ? "This record could not be found."
            : response.status >= 500
              ? "The bakery service is unavailable. Please try again shortly."
              : "We couldn’t complete this request.";
    throw new ApiError(body?.message || fallback, response.status);
  }
  if (response.status !== 204 && !json)
    throw new ApiError(
      "The bakery returned an unexpected response. Please sign in again.",
      502,
    );
  return body;
}
export async function csrfHeaders() {
  const token = await parse(await transport(`${API_BASE}/csrf`));
  if (!token?.headerName || !token?.token)
    throw new ApiError("Unable to establish a secure session.");
  return { [token.headerName]: token.token };
}
export async function request(path, { method = "GET", body, signal } = {}) {
  const headers = { Accept: "application/json" };
  if (method !== "GET") Object.assign(headers, await csrfHeaders());
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await transport(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });
  if (response.status === 401 && path !== "/users/me")
    window.dispatchEvent(new Event("bakenest:unauthorized"));
  return parse(response);
}
export async function sessionForm(path, values = {}) {
  const response = await transport(`/auth/${path}`, {
    method: "POST",
    headers: {
      ...(await csrfHeaders()),
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams(values),
    redirect: "follow",
  });
  // Spring's default form login and logout redirect to HTML. /users/me verifies authentication.
  if (response.status === 403)
    throw new ApiError("Your secure session expired. Please try again.", 403);
  if (response.status >= 500)
    throw new ApiError("The bakery service is unavailable.", response.status);
  if (!response.ok)
    throw new ApiError(
      "The session request could not be completed. Please try again.",
      response.status,
    );
  if (path === "login" && new URL(response.url).searchParams.has("error"))
    throw new ApiError("Incorrect username or password.", 401);
}
