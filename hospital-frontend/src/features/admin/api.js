import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the admin portal. The login token is added by app/setupAxios.

// ---------- users ----------
export const getAllUsers = () => axios.get(`${API_BASE_URL}/api/admin/allUsers`);

export const createUser = (user) => axios.post(`${API_BASE_URL}/api/admin/users`, user);

// ---------- account status (docs/ACCOUNT_DEACTIVATION_PLAN.md) ----------
// Deactivated: no login, hidden from directories and booking, upcoming appointments cancelled; history kept.
export const deactivateUser = (userId) => axios.put(`${API_BASE_URL}/api/admin/users/${userId}/deactivate`);

export const reactivateUser = (userId) => axios.put(`${API_BASE_URL}/api/admin/users/${userId}/reactivate`);

// Only for accounts without any appointment; otherwise the server answers 409 "Deactivate it instead"
export const deleteUserPermanently = (userId) => axios.delete(`${API_BASE_URL}/api/admin/users/${userId}`);

// By doctor id; also handles old doctor rows without a login account
export const deactivateDoctor = (doctorId) => axios.delete(`${API_BASE_URL}/api/doctor/delete/${doctorId}`);

// ---------- leave requests ----------
export const getPendingLeaves = () => axios.get(`${API_BASE_URL}/api/admin/allPending`);

export const getApprovedLeaves = () => axios.get(`${API_BASE_URL}/api/admin/allApproved`);

export const getRejectedLeaves = () => axios.get(`${API_BASE_URL}/api/admin/allRejected`);

// decision: "approve" or "reject"
export const decideLeave = (leaveId, decision) => axios.put(`${API_BASE_URL}/api/admin/${decision}/${leaveId}`);

// ---------- a doctor's appointments ----------
export const getDoctorAppointmentCount = (doctorId) =>
  axios.get(`${API_BASE_URL}/api/admin/doctorAppointmentCount/${doctorId}`);

export const getDoctorPendingAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/api/admin/doctorPendingAppointments/${userId}`);

export const getDoctorCompletedAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/api/admin/doctorCompletedAppointments/${userId}`);
