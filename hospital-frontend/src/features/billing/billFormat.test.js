import { render, screen } from "@testing-library/react";
import { BILL_STATUS, BillStatusBadge, methodLabel } from "./billFormat";

test("each bill state has its label and badge tone", () => {
  const tones = Object.fromEntries(Object.entries(BILL_STATUS).map(([status, { tone }]) => [status, tone]));
  expect(tones).toEqual({ UNPAID: "warning", PAID: "success", REFUND_DUE: "danger", REFUNDED: "neutral", CANCELLED: "neutral" });
  render(<BillStatusBadge status="REFUND_DUE" />);
  expect(screen.getByText("Refund due")).toHaveClass("text-danger-ink");
});

test("an unknown state shows as it is, in the neutral tone", () => {
  render(<BillStatusBadge status="WRITTEN_OFF" />);
  expect(screen.getByText("WRITTEN_OFF")).toHaveClass("text-ink-muted");
});

test("payment methods by name", () => {
  expect(methodLabel("CASH")).toBe("Cash");
  expect(methodLabel("UPI")).toBe("UPI");
  expect(methodLabel("CHEQUE")).toBe("CHEQUE");
});
