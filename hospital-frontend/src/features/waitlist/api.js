import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// The waitlist (docs/WAITLIST_PLAN.md). A wait: { id, patientId, relativeId, name (who waits), patientName (the patient
// record), phone, hasLogin, doctorId, doctorName, date, shift (MORNING, EVENING or null: either), status (WAITING,
// BOOKED, ENDED), joinedAt, joinedBy, toldAt }. The login token is added by app/setupAxios.

// ---------- patients: their own waits and their family members' ----------
// { doctorId, date, shift, relativeId }: refused (409) while there are free times, or when already booked
export const joinWaitlist = (wait) => axios.post(`${API_BASE_URL}/api/waitlist`, wait);

// From today on, soonest first
export const getMyWaits = () => axios.get(`${API_BASE_URL}/api/waitlist/my`);

export const leaveWaitlist = (waitId) => axios.delete(`${API_BASE_URL}/api/waitlist/${waitId}`);

// ---------- front desk and admins ----------
// A day's waits (today when no date; all doctors when no doctorId), by doctor and in the order they joined
export const getDeskWaitlist = (params) => axios.get(`${API_BASE_URL}/api/receptionist/waitlist`, { params });

// { patientId, relativeId, doctorId, date, shift }
export const addToWaitlist = (wait) => axios.post(`${API_BASE_URL}/api/receptionist/waitlist`, wait);

export const removeFromWaitlist = (waitId) => axios.delete(`${API_BASE_URL}/api/receptionist/waitlist/${waitId}`);
