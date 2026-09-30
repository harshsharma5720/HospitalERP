import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the auth feature (all public: /api/auth/**)

export const isOtpRequired = () => axios.get(`${API_BASE_URL}/api/auth/otp-required`);

export const sendOtp = (phone) => axios.post(`${API_BASE_URL}/api/auth/send-otp`, { phone });

export const verifyOtp = (phone, otp) => axios.post(`${API_BASE_URL}/api/auth/verify-otp`, { phone, otp });

export const register = (account) => axios.post(`${API_BASE_URL}/api/auth/register`, account);

export const login = (username, password) => axios.post(`${API_BASE_URL}/api/auth/login`, { username, password });

export const forgotPassword = (identifier) =>
  axios.post(`${API_BASE_URL}/api/auth/forgot-password`, { identifier });

export const resetPassword = (identifier, code, newPassword) =>
  axios.post(`${API_BASE_URL}/api/auth/reset-password`, { identifier, code, newPassword });
