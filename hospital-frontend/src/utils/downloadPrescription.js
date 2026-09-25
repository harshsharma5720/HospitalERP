import axios from "axios";
import { API_BASE_URL } from "../config";
import { getErrorMessage } from "./apiError";

// Downloads the prescription PDF (needs the auth header, so a plain <a href> won't work)
export const downloadPrescription = async (appointmentId) => {
  try {
    const res = await axios.get(
      `${API_BASE_URL}/api/consultations/appointment/${appointmentId}/prescription.pdf`,
      { responseType: "blob" }
    );
    const url = URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `prescription-${appointmentId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (err) {
    alert(getErrorMessage(err, "Could not download the prescription."));
  }
};
