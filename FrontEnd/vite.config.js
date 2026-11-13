import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target = env.BACKEND_ORIGIN || "http://localhost:8080";
  const configure = (proxyServer) =>
    proxyServer.on("proxyRes", (response) => {
      // Spring may send absolute redirects after session login. Keep these on the
      // frontend origin so credentials never depend on cross-origin redirects.
      const location = response.headers.location;
      if (!location) return;
      try {
        const redirect = new URL(location, target);
        if (redirect.origin === new URL(target).origin)
          response.headers.location = `${redirect.pathname}${redirect.search}${redirect.hash}`;
      } catch {
        /* Leave an invalid upstream header for the browser to reject. */
      }
    });
  const proxy = {
    "/api": { target, changeOrigin: true, configure },
    "/auth": {
      target,
      changeOrigin: true,
      configure,
      rewrite: (path) => path.replace(/^\/auth/, ""),
    },
  };
  return {
    plugins: [react(), tailwindcss()],
    server: { host: "127.0.0.1", port: 5173, strictPort: true, proxy },
    preview: { host: "127.0.0.1", port: 4173, strictPort: true, proxy },
  };
});
