import axios from "axios";

/**
 * Central API client. Attaches the JWT access token (Section 19 Security -
 * JWT Authentication) and forwards a correlation id so responses can be
 * traced against backend logs (Section 20 Logging).
 */
const client = axios.create({
  baseURL: "/api/v1"
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function redirectToLogin() {
  localStorage.clear();
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

// A 401 from these means "bad credentials/token", not "your session expired" - there's no
// session yet to refresh, so the refresh-and-retry dance below must not run for them. Without
// this, a wrong-password attempt on /auth/login hits the no-refresh-token branch, which redirects
// (a no-op, already on /login) and returns a promise that never resolves - the caller's await
// hangs forever and the error never reaches the UI.
const PUBLIC_AUTH_PATHS = ["/auth/login", "/auth/register", "/auth/otp/request"];

client.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const original = error.config;
    const isPublicAuthCall = PUBLIC_AUTH_PATHS.some((path) => original.url?.startsWith(path));
    if (error.response?.status === 401 && !original._retry && !isPublicAuthCall) {
      original._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const { data } = await axios.post("/api/v1/auth/refresh", { refreshToken });
          localStorage.setItem("accessToken", data.data.accessToken);
          localStorage.setItem("refreshToken", data.data.refreshToken);
          original.headers.Authorization = `Bearer ${data.data.accessToken}`;
          return client(original);
        } catch (e) {
          redirectToLogin();
          return new Promise(() => {}); // navigation is in flight; don't let callers race it
        }
      }
      // No refresh token at all - session is unrecoverable, bounce to login instead of hanging.
      redirectToLogin();
      return new Promise(() => {});
    }
    return Promise.reject(error.response?.data ?? error);
  }
);

export default client;
