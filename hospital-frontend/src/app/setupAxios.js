import axios from "axios";
import { API_BASE_URL } from "./config";
import useAuthStore from "../features/auth/useAuthStore";

// Global axios behaviour, installed once from index.js (docs/SAFER_LOGIN_TOKENS_PLAN.md):
//  - attaches the access token to every request to our API (if the caller didn't set one)
//  - on 401 the access token has run out: one refresh (shared by requests failing at the same time), then the request
//    is tried once more; when the login itself has ended, the user is sent to the login page
axios.interceptors.request.use((config) => {
  const { token } = useAuthStore.getState();
  const url = config.url || "";
  const isOurApi = url.startsWith(API_BASE_URL) || url.startsWith("/");
  if (token && isOurApi && !config.headers?.Authorization) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const config = error?.config;
    const url = config?.url || "";
    if (status === 401 && config && !url.includes("/api/auth/") && !config.retriedAfterRefresh) {
      const token = await useAuthStore.getState().refresh();
      if (token) {
        config.retriedAfterRefresh = true;
        config.headers = { ...config.headers, Authorization: `Bearer ${token}` };
        return axios(config);
      }
      if (!window.location.pathname.startsWith("/login")) {
        window.location.assign("/login?expired=1");
      }
    }
    return Promise.reject(error);
  }
);
