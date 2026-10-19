import axios from "axios";
import { create } from "zustand";
import { API_BASE_URL } from "../../app/config";
import { getExpiryMs, getRoleFromToken, getUserIdFromToken } from "../../shared/utils/jwtUtils";

// The logged-in user (docs/SAFER_LOGIN_TOKENS_PLAN.md). The short access token lives in memory only, never in
// localStorage; the login itself is kept by the server's refresh token, in an httpOnly cookie no script can read.
// On start the app asks for a new access token (restoreSession); "ready" tells it the answer is in. Tabs stay in step
// through a BroadcastChannel. Components subscribe, so logging in or out updates routes and headers at once.

const AUTH = `${API_BASE_URL}/api/auth`;
// The cookie must travel (also across ports in development), and refresh / logout must say they come from the app
export const FROM_THE_APP = { withCredentials: true, headers: { "X-Requested-With": "XMLHttpRequest" } };
const REFRESH_EARLY_MS = 60 * 1000; // a minute before the access token runs out
const LONGEST_TIMER_MS = 2147483647; // setTimeout fires at once for anything longer (about 24.8 days)

const signedIn = (token) => ({ token, role: getRoleFromToken(token), userId: getUserIdFromToken(token) });
const signedOut = { token: null, role: null, userId: null };

let refreshing = null; // the refresh in flight, shared by everyone who needs one
let timer = null;
let channel = null;
try {
  channel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("hospital-auth");
} catch (e) {
  channel = null;
}

const schedule = (token) => {
  clearTimeout(timer);
  const expiry = getExpiryMs(token);
  if (!expiry) return;
  const delay = Math.min(Math.max(expiry - Date.now() - REFRESH_EARLY_MS, 5000), LONGEST_TIMER_MS);
  timer = setTimeout(() => useAuthStore.getState().refresh(), delay);
};

const useAuthStore = create((set) => ({
  ...signedOut,
  ready: false,

  // After logging in or registering: the access token from the answer (the server has set the cookie)
  login: (token) => {
    set({ ...signedIn(token), ready: true });
    schedule(token);
    channel?.postMessage("login");
  },

  // A new access token from the refresh cookie; resolves with it, or with null when the login has ended
  refresh: () => {
    if (!refreshing) {
      refreshing = axios
        .post(`${AUTH}/refresh`, null, FROM_THE_APP)
        .then((res) => {
          const { token } = res.data;
          set({ ...signedIn(token), ready: true });
          schedule(token);
          return token;
        })
        .catch(() => {
          clearTimeout(timer);
          set({ ...signedOut, ready: true });
          return null;
        })
        .finally(() => {
          refreshing = null;
        });
    }
    return refreshing;
  },

  // Logout on this device: at once here and in the other tabs; the server ends the login and clears the cookie
  logout: () => {
    clearTimeout(timer);
    set({ ...signedOut, ready: true });
    channel?.postMessage("logout");
    return axios.post(`${AUTH}/logout`, null, FROM_THE_APP).catch(() => {});
  },

  // The other tabs' news: a logout drops this tab's token, a login fetches this tab's own
  heard: (message) => {
    if (message === "logout") {
      clearTimeout(timer);
      set({ ...signedOut, ready: true });
    } else if (message === "login") {
      useAuthStore.getState().refresh();
    }
  },
}));

if (channel) {
  channel.onmessage = (event) => useAuthStore.getState().heard(event.data);
}

// Once on start: the login, if the cookie still holds one. The token of older versions in localStorage is dropped.
export const restoreSession = () => {
  try {
    localStorage.removeItem("jwtToken");
  } catch (e) {
    // storage unavailable (private mode) - nothing to drop
  }
  return useAuthStore.getState().refresh();
};

export const homePathForRole = (role) => {
  switch (role) {
    case "ROLE_ADMIN":
      return "/admin/dashboard";
    case "ROLE_DOCTOR":
      return "/doctor/dashboard";
    case "ROLE_RECEPTIONIST":
      return "/receptionist-appointments";
    default:
      return "/";
  }
};

export default useAuthStore;
