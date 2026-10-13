import { getErrorMessage } from "../../shared/utils/apiError";

// Saves a bill's receipt PDF (it needs the login token, so a plain <a href> won't do). fetchPdf: the API call for
// the front desk's or the patient's receipt; onError gets the message when it fails.
export const downloadReceipt = async (fetchPdf, billNumber, onError) => {
  try {
    const res = await fetchPdf();
    const url = URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `receipt-${billNumber}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (err) {
    onError(getErrorMessage(err, "Could not download the receipt."));
  }
};
