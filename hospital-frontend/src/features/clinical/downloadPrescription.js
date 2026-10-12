import { getErrorMessage } from "../../shared/utils/apiError";
import { getPrescriptionPdf } from "./api";

// Downloads the prescription PDF (needs the auth header, so a plain <a href> won't work). onError gets the message
// when it fails (the pages pass a toast).
export const downloadPrescription = async (appointmentId, onError) => {
  try {
    const res = await getPrescriptionPdf(appointmentId);
    const url = URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `prescription-${appointmentId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (err) {
    onError(getErrorMessage(err, "Could not download the prescription."));
  }
};
