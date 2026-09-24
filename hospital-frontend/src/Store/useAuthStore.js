import { create } from "zustand";
import { getRoleFromToken, getUserIdFromToken, isTokenExpired } from "../utils/jwtUtils";

const TOKEN_KEY = "jwtToken";

const readSession = () => {
  let token = null;
  try {
    token = localStorage.getItem(TOKEN_KEY);
  } catch (e) {
    token = null;
  }
  if (!token || isTokenExpired(token)) {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch (e) {
      // storage unavailable (private mode) — nothing to clear
    }
    return { token: null, role: null, userId: null };
  }
  return { token, role: getRoleFromToken(token), userId: getUserIdFromToken(token) };
};

// Single source of truth for the logged-in user. Components subscribe to it,
// so logging in or out updates routes, navbars and the footer immediately.
const useAuthStore = create((set) => ({
  ...readSession(),

  login: (token) => {
    localStorage.setItem(TOKEN_KEY, token);
    set({ token, role: getRoleFromToken(token), userId: getUserIdFromToken(token) });
  },

  logout: () => {
    localStorage.removeItem(TOKEN_KEY);
    set({ token: null, role: null, userId: null });
    // Keeps other tabs / legacy listeners in sync
    window.dispatchEvent(new Event("storage"));
  },
}));

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
