import { request } from "./client.js";
export const orderApi = {
  list: () => request("/orders"),
  mine: () => request("/orders/mine"),
  get: (id) => request(`/orders/${id}`),
  create: (body) => request("/orders", { method: "POST", body }),
  update: (id, body) => request(`/orders/${id}`, { method: "PUT", body }),
  quantity: (id, itemId, quantity) =>
    request(`/orders/${id}/items/${itemId}`, {
      method: "PATCH",
      body: { quantity },
    }),
  status: (id, status) =>
    request(`/orders/${id}/status`, { method: "PATCH", body: { status } }),
  cancel: (id) => request(`/orders/${id}/cancel`, { method: "POST" }),
  remove: (id) => request(`/orders/${id}`, { method: "DELETE" }),
};
