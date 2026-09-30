import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the clinical feature: consultations, prescriptions and medical history.
// The login token is added by app/setupAxios.

export const getConsultation = (appointmentId) =>
  axios.get(`${API_BASE_URL}/api/consultations/appointment/${appointmentId}`);

export const saveConsultation = (appointmentId, consultation) =>
  axios.put(`${API_BASE_URL}/api/consultations/appointment/${appointmentId}`, consultation);

export const getPrescriptionPdf = (appointmentId) =>
  axios.get(`${API_BASE_URL}/api/consultations/appointment/${appointmentId}/prescription.pdf`, {
    responseType: "blob",
  });

// A patient's own history
export const getMyHistory = () => axios.get(`${API_BASE_URL}/api/consultations/my`);

// A patient's history for their doctor / an admin
export const getPatientHistory = (patientId) => axios.get(`${API_BASE_URL}/api/consultations/patient/${patientId}`);
