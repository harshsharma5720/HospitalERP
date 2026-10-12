import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import BillPage from "./BillPage";
import * as billingApi from "./api";

jest.mock("./api");

const CONSULTATION = { id: 11, description: "Consultation — Dr. Anita Rao", amount: 500, consultation: true };
const DRESSING = { id: 12, description: "Dressing", amount: 150, consultation: false };

const bill = (extra = {}) => ({
  id: 7, billNumber: "B-2026-000007", appointmentId: 107, patientId: 41, patientName: "Asha Rao", doctorName: "Anita Rao",
  visitDate: "2026-10-12", status: "UNPAID", items: [CONSULTATION], subtotal: 500, discount: 0, total: 500,
  createdAt: "2026-10-12T09:50:00", paidAt: null, paymentMethod: null, paymentReference: null, paidBy: null,
  refundedAt: null, refundMethod: null, refundReference: null, refundedBy: null, ...extra,
});

const renderAt = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/bills/:billId" element={<BillPage />} />
        <Route path="/bills/visit/:appointmentId" element={<BillPage />} />
        <Route path="/admin/bills/:billId" element={<BillPage />} />
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => billingApi.getBill.mockResolvedValue({ data: bill() }));
afterEach(() => jest.resetAllMocks());

test("an unpaid bill: items added and removed, a discount, then the payment", async () => {
  billingApi.addBillItem.mockResolvedValue({ data: bill({ items: [CONSULTATION, DRESSING], subtotal: 650, total: 650 }) });
  billingApi.setBillDiscount.mockResolvedValue({
    data: bill({ items: [CONSULTATION, DRESSING], subtotal: 650, discount: 50, total: 600 }),
  });
  billingApi.payBill.mockResolvedValue({
    data: bill({
      items: [CONSULTATION, DRESSING], subtotal: 650, discount: 50, total: 600, status: "PAID",
      paidAt: "2026-10-12T10:05:31", paymentMethod: "UPI", paymentReference: "UPI-1234", paidBy: "deskmeena",
    }),
  });
  renderAt("/bills/7");

  expect(await screen.findByRole("heading", { name: /Bill B-2026-000007/ })).toHaveTextContent("Unpaid");
  expect(screen.getByRole("link", { name: "Asha Rao" })).toHaveAttribute("href", "/patients/41");
  expect(screen.queryByRole("button", { name: /Remove Consultation/ })).not.toBeInTheDocument(); // the fee stays
  expect(screen.queryByRole("button", { name: "Download receipt" })).not.toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Item"), { target: { value: "Dressing" } });
  fireEvent.change(screen.getByLabelText("Amount (₹)"), { target: { value: "150" } });
  fireEvent.click(screen.getByRole("button", { name: "Add" }));
  await waitFor(() => expect(billingApi.addBillItem).toHaveBeenCalledWith(7, { description: "Dressing", amount: 150 }));
  expect(await screen.findByRole("status")).toHaveTextContent("Dressing added.");
  expect(screen.getByLabelText("Item")).toHaveValue("");
  expect(screen.getByRole("button", { name: "Remove Dressing" })).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Discount (₹)"), { target: { value: "50" } });
  fireEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(billingApi.setBillDiscount).toHaveBeenCalledWith(7, 50));
  expect(await screen.findByLabelText("Total")).toHaveTextContent("₹600.00");

  fireEvent.click(screen.getByRole("radio", { name: "UPI" }));
  fireEvent.change(screen.getByLabelText("Reference (optional)"), { target: { value: " UPI-1234 " } });
  fireEvent.click(screen.getByRole("button", { name: "Record payment of ₹600.00" }));
  await waitFor(() => expect(billingApi.payBill).toHaveBeenCalledWith(7, { method: "UPI", reference: "UPI-1234" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Paid: ₹600.00 by UPI.");
  const payment = screen.getByRole("region", { name: "Payment" });
  expect(payment).toHaveTextContent("Paid ₹600.00 by UPI (ref. UPI-1234) on Mon 12 Oct 2026, 10:05, received by deskmeena.");
  expect(screen.queryByRole("button", { name: "Add" })).not.toBeInTheDocument(); // a paid bill doesn't change
  expect(screen.getByRole("button", { name: "Download receipt" })).toBeInTheDocument();
});

test("a removed item, and the server's message when a change is refused", async () => {
  billingApi.getBill.mockResolvedValue({ data: bill({ items: [CONSULTATION, DRESSING], subtotal: 650, total: 650 }) });
  billingApi.removeBillItem.mockResolvedValue({ data: bill() });
  billingApi.setBillDiscount.mockRejectedValue({
    response: { status: 400, data: { message: "The discount can't be more than the bill (Rs. 500.00)" } },
  });
  renderAt("/bills/7");

  fireEvent.click(await screen.findByRole("button", { name: "Remove Dressing" }));
  await waitFor(() => expect(billingApi.removeBillItem).toHaveBeenCalledWith(7, 12));
  expect(await screen.findByRole("status")).toHaveTextContent("Dressing removed.");

  fireEvent.change(screen.getByLabelText("Discount (₹)"), { target: { value: "900" } });
  fireEvent.click(screen.getByRole("button", { name: "Apply" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("The discount can't be more than the bill");
});

test("a cancelled paid visit: the refund is recorded", async () => {
  const paid = {
    status: "REFUND_DUE", paidAt: "2026-10-12T10:05:31", paymentMethod: "CARD", paidBy: "deskmeena",
  };
  billingApi.getBill.mockResolvedValue({ data: bill(paid) });
  billingApi.refundBill.mockResolvedValue({
    data: bill({ ...paid, status: "REFUNDED", refundedAt: "2026-10-13T09:30:00", refundMethod: "CASH", refundedBy: "deskravi" }),
  });
  renderAt("/admin/bills/7");

  expect(await screen.findByText("Refund due: ₹500.00. The visit was cancelled.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /Record payment/ })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Record refund of ₹500.00" }));

  await waitFor(() => expect(billingApi.refundBill).toHaveBeenCalledWith(7, { method: "CASH", reference: null }));
  const payment = screen.getByRole("region", { name: "Payment" });
  expect(await within(payment).findByText(/Refunded ₹500.00 by Cash on Tue 13 Oct 2026, 09:30, by deskravi/)).toBeInTheDocument();
  expect(payment).toHaveTextContent("Paid ₹500.00 by Card");
  expect(screen.getByRole("link", { name: "← Bills" })).toHaveAttribute("href", "/admin/bills");
});

test("opened from the appointment list: the visit's bill, or why there is none", async () => {
  billingApi.getBillForAppointment.mockResolvedValueOnce({ data: bill() });
  const { unmount } = renderAt("/bills/visit/107");
  expect(await screen.findByRole("heading", { name: /Bill B-2026-000007/ })).toBeInTheDocument();
  expect(billingApi.getBillForAppointment).toHaveBeenCalledWith("107");
  unmount();

  billingApi.getBillForAppointment.mockRejectedValueOnce({ response: { status: 404, data: { message: "Bill not found" } } });
  renderAt("/bills/visit/108");
  expect(await screen.findByRole("alert")).toHaveTextContent("This visit has no bill yet");
});
