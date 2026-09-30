import axios from "axios";
import { API_BASE_URL } from "../app/config";
import { getDoctor, updateDoctor } from "../features/doctors/api";
import { getPatientAccount, updatePatientAccount } from "../features/patients/api";

// The logged-in user's own profile, whatever their role (navbar profile panel, edit-profile page).
// The login token is added by app/setupAxios.

export const getReceptionist = (userId) =>
  axios.get(`${API_BASE_URL}/api/receptionist/getReceptionist/${userId}`);

export const updateReceptionist = (userId, formData) =>
  axios.put(`${API_BASE_URL}/api/receptionist/${userId}`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

const PROFILE_BY_ROLE = {
  ROLE_PATIENT: { get: getPatientAccount, update: updatePatientAccount },
  ROLE_DOCTOR: { get: getDoctor, update: updateDoctor },
  ROLE_RECEPTIONIST: { get: getReceptionist, update: updateReceptionist },
};

// True for the roles that have a profile (admins have none)
export const hasProfile = (role) => Boolean(PROFILE_BY_ROLE[role]);

export const getOwnProfile = (role, userId) => PROFILE_BY_ROLE[role].get(userId);

export const updateOwnProfile = (role, userId, formData) => PROFILE_BY_ROLE[role].update(userId, formData);
