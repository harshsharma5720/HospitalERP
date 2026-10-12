import { Badge } from "../../shared/ui";

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
