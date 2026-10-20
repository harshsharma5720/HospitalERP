import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the patients feature: the patient account and relatives.
// The login token is added by app/setupAxios.

export const getPatientAccount = (userId) => axios.get(`${API_BASE_URL}/api/patient/getAccount/${userId}`);

export const updatePatientAccount = (userId, formData) =>
  axios.put(`${API_BASE_URL}/api/patient/updateAccount/${userId}`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const getRelatives = (patientId) => axios.get(`${API_BASE_URL}/api/patient/relative/patient/${patientId}`);

export const addRelative = (relative) => axios.post(`${API_BASE_URL}/api/patient/relative/add`, relative);

export const updateRelative = (id, relative) => axios.put(`${API_BASE_URL}/api/patient/relative/update/${id}`, relative);

export const deleteRelative = (id) => axios.delete(`${API_BASE_URL}/api/patient/relative/delete/${id}`);

// ---------- front desk: patient search (docs/PATIENT_SEARCH_PLAN.md) ----------
// q: part of a name, a phone number or a patient ID ("#41"); empty lists everyone. page counts from 0.
// Answers { entries, page, size, totalEntries, totalPages }. Recorded in the audit log.
export const searchPatients = (q, page, size) =>
  axios.get(`${API_BASE_URL}/api/receptionist/patients/search`, { params: { q, page, size } });

// One patient's details, by patient id (recorded as a profile view)
export const getFrontDeskPatient = (patientId) => axios.get(`${API_BASE_URL}/api/receptionist/patients/${patientId}`);

// ---------- front desk: ABHA numbers (docs/ABHA_NUMBER_PLAN.md) ----------
// abhaNumber: 14 digits (dashes and spaces allowed), or "" to remove it. 409 when it is on another record.
export const setPatientAbha = (patientId, abhaNumber) =>
  axios.put(`${API_BASE_URL}/api/receptionist/patients/${patientId}/abha`, { abhaNumber });

// The patient's family members, each with their ABHA number
export const getPatientRelatives = (patientId) => axios.get(`${API_BASE_URL}/api/receptionist/patients/${patientId}/relatives`);

export const setRelativeAbha = (patientId, relativeId, abhaNumber) =>
  axios.put(`${API_BASE_URL}/api/receptionist/patients/${patientId}/relatives/${relativeId}/abha`, { abhaNumber });
