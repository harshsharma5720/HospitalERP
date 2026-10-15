import { useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import { useToast } from "../../shared/ui";
import { confirmOnlinePayment, startOnlinePayment } from "./api";
import { openCheckout } from "./razorpayCheckout";

// Paying a bill online (docs/ONLINE_PAYMENT_PLAN.md): the server's order, Razorpay Checkout, then the server checks and
// records the payment. onPaid(bill) gets the paid bill. When the payment couldn't be applied (409: the bill was paid or
// changed meanwhile, and the payment is refunded) onStale() lets the page reload its bills. The error stays with the
// bill (errorFor) rather than in a toast that vanishes: it may say a refund is on its way.
export default function useOnlinePayment({ onPaid, onStale }) {
  const toast = useToast();
  const [paying, setPaying] = useState(null); // the bill's id while its payment runs
  const [error, setError] = useState({ billId: null, message: "" });

  const pay = async (bill) => {
    setPaying(bill.id);
    setError({ billId: null, message: "" });
    try {
      const { data: order } = await startOnlinePayment(bill.id);
      const answer = await openCheckout(order);
      if (!answer) return; // closed without paying
      const { data: paid } = await confirmOnlinePayment(bill.id, answer);
      toast.success(`Paid ${formatRupees(paid.total)} online. Thank you!`);
      onPaid(paid);
    } catch (err) {
      setError({
        billId: bill.id,
        message: getErrorMessage(err, "The payment couldn't be completed. Please try again, or pay at the front desk."),
      });
      if (err?.response?.status === 409) onStale();
    } finally {
      setPaying(null);
    }
  };

  return { pay, paying, errorFor: (billId) => (error.billId === billId ? error.message : "") };
}
