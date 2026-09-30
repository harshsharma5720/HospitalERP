import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the appointments feature: booking (patients), a doctor's appointment lists and
// the front desk. The login token is added by app/setupAxios.

// ---------- patient: booking and own appointments ----------
export const getMyAppointments = () => axios.get(`${API_BASE_URL}/appointment/getPatientAppointments`);

export const getPatientPendingAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/appointment/patientPendingAppointments/${userId}`);

export const getPatientCompletedAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/appointment/patientCompletedAppointments/${userId}`);

export const getAvailableSlots = (doctorId, date, shift) =>
  axios.get(`${API_BASE_URL}/api/slots/available/${doctorId}`, { params: { date, shift } });

export const bookAppointment = (booking) => axios.post(`${API_BASE_URL}/appointment/NewAppointment`, booking);

export const rescheduleAppointment = (appointmentId, change) =>
  axios.put(`${API_BASE_URL}/appointment/update/${appointmentId}`, change);

export const cancelAppointment = (appointmentId) =>
  axios.delete(`${API_BASE_URL}/appointment/CancelAppointment/${appointmentId}`);

// ---------- doctor: own appointments ----------
export const getDoctorAppointments = () => axios.get(`${API_BASE_URL}/appointment/getDoctorAppointments`);

export const getDoctorPendingAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/api/doctor/doctorPendingAppointments/${userId}`);

export const getDoctorCompletedAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/api/doctor/doctorCompletedAppointments/${userId}`);

export const completeAppointment = (appointmentId) =>
  axios.put(`${API_BASE_URL}/api/doctor/complete/${appointmentId}`);

// ---------- front desk (receptionist) ----------
export const getFrontDeskAppointments = () => axios.get(`${API_BASE_URL}/api/receptionist/getAppointments`);

export const getFrontDeskAppointmentsByDoctorName = (doctorName) =>
  axios.get(`${API_BASE_URL}/api/receptionist/getAppointmentByDoctor/${doctorName}`);

export const getFrontDeskDoctorPendingAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/api/receptionist/doctorPendingAppointments/${userId}`);

export const getFrontDeskDoctorCompletedAppointments = (userId) =>
  axios.get(`${API_BASE_URL}/api/receptionist/doctorCompletedAppointments/${userId}`);
