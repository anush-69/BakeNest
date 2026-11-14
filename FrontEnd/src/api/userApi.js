import { request } from "./client.js";
export const userApi = {
  list: () => request("/users"),
  get: (id) => request(`/users/${id}`),
  update: (id, body) => request(`/users/${id}`, { method: "PUT", body }),
  password: (body) => request("/users/me/password", { method: "PUT", body }),
  delete: (id) => request(`/users/${id}`, { method: "DELETE" }),
};
