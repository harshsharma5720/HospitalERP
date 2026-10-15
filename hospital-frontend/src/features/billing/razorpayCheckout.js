// Razorpay Checkout (docs/ONLINE_PAYMENT_PLAN.md): Razorpay's own payment window, so card and UPI details never reach
// our server. Its script is loaded once, when a patient first pays.
const SCRIPT = "https://checkout.razorpay.com/v1/checkout.js";
let loading = null;

export const loadCheckout = () => {
  if (window.Razorpay) return Promise.resolve(window.Razorpay);
  if (!loading) {
    loading = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = SCRIPT;
      script.async = true;
      script.onload = () => (window.Razorpay ? resolve(window.Razorpay) : reject(new Error("Checkout didn't load")));
      script.onerror = () => {
        loading = null; // try again next time
        script.remove();
        reject(new Error("Checkout couldn't be loaded"));
      };
      document.body.appendChild(script);
    });
  }
  return loading;
};

// Opens Checkout for the server's order. Resolves with what the server needs to check the payment
// ({ orderId, paymentId, signature }), or null when the patient closes it without paying. A failed attempt is shown
// by Checkout itself, where the patient can try again.
export const openCheckout = async (order) => {
  const Razorpay = await loadCheckout();
  return new Promise((resolve) => {
    const checkout = new Razorpay({
      key: order.keyId,
      amount: order.amount,
      currency: order.currency,
      order_id: order.orderId,
      name: order.name,
      description: order.description,
      prefill: { name: order.prefill?.name ?? "", email: order.prefill?.email ?? "", contact: order.prefill?.contact ?? "" },
      theme: { color: "#2563eb" },
      handler: (response) =>
        resolve({ orderId: order.orderId, paymentId: response.razorpay_payment_id, signature: response.razorpay_signature }),
      modal: { ondismiss: () => resolve(null) },
    });
    checkout.open();
  });
};
