import { Badge } from "../../shared/ui";
import { formatDate } from "../appointments/appointmentFormat";

// Bill states as the front desk reads them; the badge always carries its label, the colour only supports it
export const BILL_STATUS = {
  UNPAID: { label: "Unpaid", tone: "warning" },
  PAID: { label: "Paid", tone: "success" },
  REFUND_DUE: { label: "Refund due", tone: "danger" },
  REFUNDED: { label: "Refunded", tone: "neutral" },
  CANCELLED: { label: "Cancelled", tone: "neutral" },
};

export const PAYMENT_METHODS = [
  ["CASH", "Cash"],
  ["CARD", "Card"],
  ["UPI", "UPI"],
];

export const methodLabel = (method) => PAYMENT_METHODS.find(([value]) => value === method)?.[1] ?? method;

export function BillStatusBadge({ status }) {
  const { label, tone } = BILL_STATUS[status] ?? { label: status, tone: "neutral" };
  return <Badge tone={tone}>{label}</Badge>;
}

// A receipt exists once the bill is paid (also when the visit was then cancelled), as the server decides
export const hasReceipt = (status) => ["PAID", "REFUND_DUE", "REFUNDED"].includes(status);

// "2026-10-12T10:05:31" -> "Mon 12 Oct 2026, 10:05"
export const formatDateTime = (time) => (time ? `${formatDate(time.slice(0, 10), true)}, ${time.slice(11, 16)}` : "");

// " (ref. UPI-1234)", or nothing without a reference
export const referenceNote = (reference) => (reference ? ` (ref. ${reference})` : "");
