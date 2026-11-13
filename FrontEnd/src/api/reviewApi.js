import { request } from "./client.js";
export const reviewApi = {
  list: () => request("/reviews"),
  all: () => request("/reviews/all"),
  get: (id) => request(`/reviews/${id}`),
  product: (id) => request(`/reviews/product/${id}`),
  average: (id) => request(`/reviews/product/${id}/average`),
  customer: (id) => request(`/reviews/customer/${id}`),
  create: (body) => request("/reviews", { method: "POST", body }),
  update: (id, body) => request(`/reviews/${id}`, { method: "PUT", body }),
  moderate: (id, body) =>
    request(`/reviews/${id}/moderation`, { method: "PATCH", body }),
  delete: (id) => request(`/reviews/${id}`, { method: "DELETE" }),
};
