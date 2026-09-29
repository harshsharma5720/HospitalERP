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
