import { getErrorMessage } from "../../shared/utils/apiError";

// Saves a PDF from the API (it needs the login token, so a plain <a href> won't do). fetchPdf: the API call;
// onError gets the message when it fails.
export const downloadPdf = async (fetchPdf, fileName, onError, failure = "Could not download the PDF.") => {
  try {
    const res = await fetchPdf();
    const url = URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (err) {
    onError(getErrorMessage(err, failure));
  }
};

// A bill's receipt: the front desk's or the patient's
export const downloadReceipt = (fetchPdf, billNumber, onError) =>
  downloadPdf(fetchPdf, `receipt-${billNumber}.pdf`, onError, "Could not download the receipt.");
