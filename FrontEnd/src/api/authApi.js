import { request, sessionForm, ApiError } from "./client.js";
export const authApi = {
  me: () => request("/users/me"),
  register: (body) => request("/users/register", { method: "POST", body }),
  login: async (username, password) => {
    await sessionForm("login", { username, password });
    try {
      return await request("/users/me");
    } catch (error) {
      if (error.status === 401)
        throw new ApiError("Incorrect username or password.", 401);
      throw error;
    }
  },
  logout: () => sessionForm("logout"),
};
