import axios from "axios";
import { API_BASE_URL } from "../../../app/config";

// A doctor's analytics (docs/DOCTOR_ANALYTICS_PLAN.md) for days = 7, 30 or 90: the visits (appointments module) and
// the earnings (billing). Without a doctorId the logged-in doctor's; with one any doctor's, for admins.
const url = (what, doctorId) =>
  doctorId ? `${API_BASE_URL}/api/admin/doctors/${doctorId}/${what}` : `${API_BASE_URL}/api/doctor/${what}`;

export const getAnalytics = (days, doctorId) => axios.get(url("analytics", doctorId), { params: { days } });

export const getEarnings = (days, doctorId) => axios.get(url("earnings", doctorId), { params: { days } });
