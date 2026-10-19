import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the auth feature (all public: /api/auth/**)

export const isOtpRequired = () => axios.get(`${API_BASE_URL}/api/auth/otp-required`);

export const sendOtp = (phone) => axios.post(`${API_BASE_URL}/api/auth/send-otp`, { phone });

export const verifyOtp = (phone, otp) => axios.post(`${API_BASE_URL}/api/auth/verify-otp`, { phone, otp });

// Login and register answer the access token and set the refresh cookie (docs/SAFER_LOGIN_TOKENS_PLAN.md)
export const register = (account) => axios.post(`${API_BASE_URL}/api/auth/register`, account, { withCredentials: true });

export const login = (username, password) =>
  axios.post(`${API_BASE_URL}/api/auth/login`, { username, password }, { withCredentials: true });

export const forgotPassword = (identifier) =>
  axios.post(`${API_BASE_URL}/api/auth/forgot-password`, { identifier });

export const resetPassword = (identifier, code, newPassword) =>
  axios.post(`${API_BASE_URL}/api/auth/reset-password`, { identifier, code, newPassword });
