import { request } from "./client.js";
export const productApi = {
  list: (filters = {}, signal) => {
    const query = new URLSearchParams(
      Object.entries(filters).filter(
        ([, value]) => value !== "" && value != null,
      ),
    );
    return request(`/products?${query}`, { signal });
  },
  get: (id) => request(`/products/${id}`),
  all: () => request("/products/all"),
  categories: () => request("/categories"),
  createCategory: (body) => request("/categories", { method: "POST", body }),
  create: (body) => request("/products", { method: "POST", body }),
  update: (id, body) => request(`/products/${id}`, { method: "PUT", body }),
  delete: (id) => request(`/products/${id}`, { method: "DELETE" }),
};
