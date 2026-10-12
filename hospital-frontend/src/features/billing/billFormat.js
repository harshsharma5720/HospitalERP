// Bill states as the front desk reads them; the badge always carries its label, the color only supports it
export const BILL_STATUS = {
  UNPAID: { label: "Unpaid", style: "bg-amber-100 text-amber-800" },
  PAID: { label: "Paid", style: "bg-green-100 text-green-800" },
  REFUND_DUE: { label: "Refund due", style: "bg-red-100 text-red-800" },
  REFUNDED: { label: "Refunded", style: "bg-gray-200 text-gray-700" },
  CANCELLED: { label: "Cancelled", style: "bg-gray-200 text-gray-700" },
};

export const PAYMENT_METHODS = [
  ["CASH", "Cash"],
  ["CARD", "Card"],
  ["UPI", "UPI"],
];

export const methodLabel = (method) => PAYMENT_METHODS.find(([value]) => value === method)?.[1] ?? method;

export function BillStatusBadge({ status }) {
  const { label, style } = BILL_STATUS[status] ?? { label: status, style: "" };
  return <span className={`px-2 py-1 rounded-full text-xs font-semibold ${style}`}>{label}</span>;
}
