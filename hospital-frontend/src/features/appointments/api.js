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

// ---------- doctor: today's queue (docs/TODAYS_QUEUE_PLAN.md) ----------
// { date, withDoctor, waiting (by time), notArrived, done } - lists of appointments, each with tokenNumber,
// checkedInAt, calledAt
export const getMyQueue = () => axios.get(`${API_BASE_URL}/api/doctor/queue`);

// The first waiting patient by appointment time; 409 when nobody is waiting
export const callNextPatient = () => axios.post(`${API_BASE_URL}/api/doctor/queue/next`);

export const callPatient = (appointmentId) => axios.post(`${API_BASE_URL}/api/doctor/queue/${appointmentId}/call`);

// Called in but not there: back to waiting, with the same token
export const patientNotHere = (appointmentId) =>
  axios.post(`${API_BASE_URL}/api/doctor/queue/${appointmentId}/not-here`);

// ---------- front desk (receptionist) ----------
export const getFrontDeskAppointments = () => axios.get(`${API_BASE_URL}/api/receptionist/getAppointments`);

// One page of appointments (docs/PATIENT_SEARCH_PLAN.md). params, all optional: from, to (yyyy-MM-dd, both included),
// doctorId, status (upcoming, completed, cancelled, all), patient (part of the name), patientId,
// order (asc: oldest first, desc: newest first), page (from 0), size (max 100).
// Answers { entries, page, size, totalEntries, totalPages }.
export const getFrontDeskAppointmentPage = (params) =>
  axios.get(`${API_BASE_URL}/api/receptionist/appointments`, { params });

// Today's queue (docs/TODAYS_QUEUE_PLAN.md): the patient has arrived - today's appointment gets its token.
// Checking in twice keeps the first token. Answers the appointment.
export const checkInAppointment = (appointmentId) =>
  axios.post(`${API_BASE_URL}/api/receptionist/appointments/${appointmentId}/check-in`);

// Undo a check-in made by mistake; 409 once the doctor has called the patient in
export const undoCheckIn = (appointmentId) =>
  axios.delete(`${API_BASE_URL}/api/receptionist/appointments/${appointmentId}/check-in`);

// ---------- front desk: walk-in patients (docs/WALK_IN_REGISTRATION_PLAN.md) ----------
// Patients with this phone number (at least 10 digits; a family may share one). Recorded in the audit log.
export const findPatientsByPhone = (phone) =>
  axios.get(`${API_BASE_URL}/api/receptionist/patients`, { params: { phone } });

// A doctor's next free slots from now on, in time order (limit 1-20)
export const getNextFreeSlots = (doctorId, limit) =>
  axios.get(`${API_BASE_URL}/api/receptionist/doctors/${doctorId}/next-free-slots`, { params: { limit } });

// { patientId } or { newPatient: { name, phone, gender, dob, email } }, plus slotId, age, message.
// One step on the server: if the slot is gone (409), no new patient record is kept either.
export const bookWalkIn = (booking) => axios.post(`${API_BASE_URL}/api/receptionist/walk-in`, booking);
