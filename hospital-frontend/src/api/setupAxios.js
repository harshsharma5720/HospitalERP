import axios from "axios";
import { API_BASE_URL } from "../config";
import useAuthStore from "../Store/useAuthStore";

// Global axios behaviour, installed once from index.js:
//  - attaches the JWT to every request to our API (if the caller didn't set one)
//  - on 401 (expired/invalid session) logs the user out and sends them to the login page
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
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || "";
    if (status === 401 && !url.includes("/api/auth/")) {
      useAuthStore.getState().logout();
      if (!window.location.pathname.startsWith("/login")) {
        window.location.assign("/login?expired=1");
      }
    }
    return Promise.reject(error);
  }
);
