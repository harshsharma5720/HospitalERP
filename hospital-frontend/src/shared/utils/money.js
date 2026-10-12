// Rupees as the hospital writes them: "₹1,25,000.00" (Indian digit grouping, always 2 decimals).
const RUPEES = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2 });

export const formatRupees = (amount) => (amount === null || amount === undefined ? "—" : RUPEES.format(Number(amount)));
