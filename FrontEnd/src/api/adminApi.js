import { request } from "./client.js";
export const adminApi = {
  list: () => request("/admins"),
  get: (id) => request(`/admins/${id}`),
  create: (body) => request("/admins", { method: "POST", body }),
  update: (id, body) => request(`/admins/${id}`, { method: "PUT", body }),
  delete: (id) => request(`/admins/${id}`, { method: "DELETE" }),
};
