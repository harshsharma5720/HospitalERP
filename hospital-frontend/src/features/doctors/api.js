import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the doctors feature: the public doctor directory, a doctor's own profile,
// weekly schedule and leave requests. The login token is added by app/setupAxios.

const MULTIPART = { headers: { "Content-Type": "multipart/form-data" } };

// ---------- public directory ----------
export const getAllDoctors = () => axios.get(`${API_BASE_URL}/api/patient/getAllDoctors`);

export const getDoctorsBySpecialization = (specialization) =>
  axios.get(`${API_BASE_URL}/api/patient/getAllBySpecialization?specialization=${specialization}`);

export const getPublicDoctor = (doctorId) => axios.get(`${API_BASE_URL}/api/doctor/getDoctor/${doctorId}`);

// ---------- a doctor's profile (by the doctor's user id) ----------
export const getDoctor = (userId) => axios.get(`${API_BASE_URL}/api/doctor/get/${userId}`);

export const updateDoctor = (userId, formData) =>
  axios.put(`${API_BASE_URL}/api/doctor/update/${userId}`, formData, MULTIPART);

// ---------- weekly schedule ----------
export const getSchedule = (userId) => axios.get(`${API_BASE_URL}/api/doctor/${userId}/schedule`);

export const saveSchedule = (userId, entries) => axios.put(`${API_BASE_URL}/api/doctor/${userId}/schedule`, entries);

// ---------- leave requests ----------
// status: "pending" or "approved"
export const getMyLeaves = (userId, status) => axios.get(`${API_BASE_URL}/api/leaves/${status}/${userId}`);

export const applyForLeave = (leave) => axios.post(`${API_BASE_URL}/api/leaves/apply`, leave);
