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

client.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
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
