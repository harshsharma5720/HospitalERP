import { cleanup, render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import MyAccount from "./MyAccount";
import useAuthStore from "../auth/useAuthStore";
import * as patientsApi from "./api";
import { getMyAppointments } from "../appointments/api";
import { confirmOnlinePayment, getMyBills, getMyReceiptPdf, startOnlinePayment } from "../billing/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api");
jest.mock("../appointments/api");
jest.mock("../billing/api");

const ACCOUNT = {
  patientId: 41, patientName: "Asha Verma", userName: "asha", email: "asha@example.com", contactNo: "9876543210",
  gender: "FEMALE", dob: "1990-04-12", patientAadharNo: "123456789012", patientAddress: "12 MG Road", profileImage: null,
};
const day = (n) => toLocalISODate(addDays(new Date(), n));

// jsdom's Blob has no text(); FileReader reads it
const readBlob = (blob) =>
  new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.readAsText(blob);
  });

const renderPage = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter>
          <MyAccount />
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_PATIENT", userId: 7 });
  patientsApi.getPatientAccount.mockResolvedValue({ data: ACCOUNT });
  patientsApi.getRelatives.mockResolvedValue({
    data: [
      { id: 5, name: "Ravi Verma", relationship: "FATHER", gender: "MALE", dob: "1958-01-01" },
      { id: 6, name: "Mira Verma", relationship: "DAUGHTER", gender: "FEMALE", dob: "2018-06-01" },
    ],
  });
  getMyAppointments.mockResolvedValue({
    data: [
      { appointmentID: 1, date: day(6), status: "SCHEDULED", startTime: "09:00:00", endTime: "09:30:00", doctorName: "Sunil Iyer", patientName: "Asha Verma" },
      { appointmentID: 2, date: day(2), status: "CONFIRMED", startTime: "10:00:00", endTime: "10:30:00", doctorName: "Anita Rao", patientName: "Mira Verma" },
      { appointmentID: 3, date: day(-5), status: "COMPLETED", startTime: "10:00:00", endTime: "10:30:00", doctorName: "Anita Rao", patientName: "Asha Verma" },
    ],
  });
  getMyBills.mockResolvedValue({ data: [] });
});
afterEach(() => jest.resetAllMocks());

test("shows the patient's details (the Aadhaar number masked), family members and the next visit", async () => {
  renderPage();
  expect(await screen.findByRole("heading", { level: 1, name: "Hello, Asha" })).toBeInTheDocument();
  expect(patientsApi.getPatientAccount).toHaveBeenCalledWith(7);
  const details = screen.getByRole("region", { name: "Your details" });
  expect(details).toHaveTextContent("XXXX XXXX 9012");
  expect(details).not.toHaveTextContent("123456789012");
  expect(details).toHaveTextContent("#41");
  const family = await screen.findByRole("region", { name: "Family members" });
  expect(within(family).getByText("Ravi Verma")).toBeInTheDocument();
  expect(within(family).getByText(/Father · \d+ y · Male/)).toBeInTheDocument();
  const appointments = screen.getByRole("region", { name: "Your appointments" });
  expect(await within(appointments).findByText(/Dr\. Anita Rao · for Mira Verma/)).toBeInTheDocument();
  expect(appointments).toHaveTextContent("and 1 more upcoming");
});

test("editing the details sends them as the profile endpoint expects; an empty Aadhaar number is sent as none", async () => {
  patientsApi.updatePatientAccount.mockImplementation(async (userId, form) => ({ data: { ...ACCOUNT, ...JSON.parse(await readBlob(form.get("ptInfoDTO"))) } }));
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  fireEvent.change(screen.getByLabelText("Phone number"), { target: { value: "9000011111" } });
  fireEvent.change(screen.getByLabelText("Aadhaar number"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

  expect(await screen.findByRole("status")).toHaveTextContent("Your details are saved.");
  const [userId, form] = patientsApi.updatePatientAccount.mock.calls[0];
  expect(userId).toBe(7);
  const sent = JSON.parse(await readBlob(form.get("ptInfoDTO")));
  expect(sent).toMatchObject({ contactNo: "9000011111", patientAadharNo: null, patientName: "Asha Verma" });
  expect(sent).not.toHaveProperty("profileImage");
  expect(form.get("profileImage")).toBeNull();
  expect(screen.queryByLabelText("Phone number")).not.toBeInTheDocument();
  expect(screen.getByRole("region", { name: "Your details" })).toHaveTextContent("9000011111");
});

test("a refused change keeps the form open; Cancel drops the changes", async () => {
  patientsApi.updatePatientAccount.mockRejectedValue({ response: { data: { message: "Email already in use" } } });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "taken@example.com" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Email already in use");
  expect(screen.getByLabelText("Email")).toHaveValue("taken@example.com");
  fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
  expect(screen.getByRole("region", { name: "Your details" })).toHaveTextContent("asha@example.com");
});

test("removing a family member asks first", async () => {
  patientsApi.deleteRelative.mockResolvedValue({ data: {} });
  renderPage();
  const family = await screen.findByRole("region", { name: "Family members" });
  fireEvent.click(await within(family).findByRole("button", { name: "Remove Mira Verma" }));
  const dialog = screen.getByRole("dialog", { name: "Remove Mira Verma?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(patientsApi.deleteRelative).not.toHaveBeenCalled();

  fireEvent.click(within(family).getByRole("button", { name: "Remove Mira Verma" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Remove" }));
  await waitFor(() => expect(within(family).queryByText("Mira Verma")).not.toBeInTheDocument());
  expect(patientsApi.deleteRelative).toHaveBeenCalledWith(6);
  expect(within(family).getByRole("link", { name: "Edit Ravi Verma" })).toHaveAttribute("href", "/edit-relative");
});

// ---- Bills (docs/PATIENT_BILLS_PLAN.md)

const bill = (id, status, total, extra = {}) => ({
  id, billNumber: `B-2026-0000${id}`, appointmentId: id + 100, patientName: "Asha Verma", doctorName: "Anita Rao", visitDate: day(-id), status,
  items: [{ description: "Consultation — Dr. Anita Rao", amount: total }], subtotal: total, discount: 0, total,
  paidAt: status === "UNPAID" ? null : `${day(-id)}T10:00:00`, paymentMethod: status === "UNPAID" ? null : "CASH", paymentReference: null,
  refundedAt: null, refundMethod: null, refundReference: null, ...extra,
});
const BILLS = [
  bill(11, "UNPAID", 800, { patientName: "Mira Verma" }),
  bill(12, "UNPAID", 300.5),
  bill(13, "PAID", 600),
  bill(14, "REFUND_DUE", 450),
  bill(15, "REFUNDED", 450),
  bill(16, "PAID", 500),
  bill(17, "PAID", 700),
];

test("the bills, newest first: what is still to pay, View opens one, Receipt downloads it, Show all shows the rest", async () => {
  URL.createObjectURL = jest.fn(() => "blob:receipt");
  URL.revokeObjectURL = jest.fn();
  const click = jest.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  getMyBills.mockResolvedValue({ data: BILLS });
  getMyReceiptPdf.mockResolvedValue({ data: "%PDF-" });
  renderPage();
  const bills = await screen.findByRole("region", { name: "Bills" });
  expect(await within(bills).findByText("To pay at the front desk: ₹1,100.50 (2 bills)")).toBeInTheDocument();
  let rows = within(bills).getAllByRole("listitem");
  expect(rows).toHaveLength(5);
  expect(rows[0]).toHaveTextContent("B-2026-000011 · for Mira Verma");
  expect(rows[0]).toHaveTextContent("₹800.00Unpaid");
  expect(within(rows[0]).queryByRole("button", { name: /Receipt/ })).not.toBeInTheDocument();
  expect(within(rows[3]).getByRole("button", { name: "Receipt B-2026-000014" })).toBeInTheDocument();
  expect(within(rows[4]).getByRole("button", { name: "Receipt B-2026-000015" })).toBeInTheDocument();

  fireEvent.click(within(bills).getByRole("button", { name: "Show all 7 bills" }));
  rows = within(bills).getAllByRole("listitem");
  expect(rows).toHaveLength(7);
  expect(rows[6]).toHaveTextContent("B-2026-000017");
  fireEvent.click(within(bills).getByRole("button", { name: "Show fewer" }));
  expect(within(bills).getAllByRole("listitem")).toHaveLength(5);

  fireEvent.click(within(bills).getByRole("button", { name: "Receipt B-2026-000013" }));
  await waitFor(() => expect(click).toHaveBeenCalled());
  expect(getMyReceiptPdf).toHaveBeenCalledWith(13);
  expect(click.mock.instances[0].download).toBe("receipt-B-2026-000013.pdf");

  fireEvent.click(within(bills).getByRole("button", { name: "View bill B-2026-000011" }));
  const dialog = screen.getByRole("dialog", { name: /Bill B-2026-000011/ });
  expect(dialog).toHaveTextContent("For Mira Verma");
  expect(dialog).toHaveTextContent("Please pay ₹800.00 at the front desk.");
  fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  click.mockRestore();
});

test("one unpaid bill and no Show all for a few bills; no bills yet; bills that can't be loaded", async () => {
  getMyBills.mockResolvedValueOnce({ data: [bill(12, "UNPAID", 300), bill(13, "PAID", 600)] });
  const { unmount } = renderPage();
  let bills = await screen.findByRole("region", { name: "Bills" });
  expect(await within(bills).findByText("To pay at the front desk: ₹300.00")).toBeInTheDocument();
  expect(within(bills).queryByRole("button", { name: /Show all/ })).not.toBeInTheDocument();
  unmount();

  renderPage();
  bills = await screen.findByRole("region", { name: "Bills" });
  expect(await within(bills).findByText("No bills yet")).toBeInTheDocument();
  expect(bills).not.toHaveTextContent("To pay");
  cleanup();

  getMyBills.mockRejectedValue(new Error("offline"));
  renderPage();
  bills = await screen.findByRole("region", { name: "Bills" });
  expect(await within(bills).findByText(/Your bills couldn't be loaded/)).toBeInTheDocument();
  expect(screen.getByRole("region", { name: "Your details" })).toHaveTextContent("asha@example.com");
});

test("Pay online from the list opens the bill and pays it; a payment service that can't be reached says so", async () => {
  const unpaid = bill(11, "UNPAID", 800, { payOnline: true });
  getMyBills.mockResolvedValue({ data: [unpaid, bill(12, "UNPAID", 300), bill(13, "PAID", 600)] });
  startOnlinePayment.mockRejectedValueOnce({ response: { status: 502, data: { message: "The payment service couldn't be reached. Please try again in a moment." } } });
  startOnlinePayment.mockResolvedValue({ data: { keyId: "rzp_test_x", orderId: "order_7", amount: 80000, currency: "INR", name: "Shreya Hospital", description: "Bill B-2026-000011", prefill: {} } });
  confirmOnlinePayment.mockResolvedValue({ data: { ...unpaid, status: "PAID", paymentMethod: "ONLINE", paymentReference: "pay_7", paidAt: `${day(-11)}T12:00:00`, payOnline: false } });
  window.Razorpay = jest.fn(function Razorpay(options) {
    this.open = () => options.handler({ razorpay_payment_id: "pay_7", razorpay_order_id: "order_7", razorpay_signature: "sig7" });
  });
  renderPage();
  const bills = await screen.findByRole("region", { name: "Bills" });
  expect(await within(bills).findByText("Still to pay: ₹1,100.00 (2 bills). Pay online here, or at the front desk.")).toBeInTheDocument();
  const rows = within(bills).getAllByRole("listitem");
  expect(within(rows[1]).queryByRole("button", { name: /Pay bill/ })).not.toBeInTheDocument();

  fireEvent.click(within(rows[0]).getByRole("button", { name: "Pay bill B-2026-000011 online" }));
  const dialog = screen.getByRole("dialog", { name: /Bill B-2026-000011/ });
  expect(await within(dialog).findByRole("alert")).toHaveTextContent("The payment service couldn't be reached.");
  expect(window.Razorpay).not.toHaveBeenCalled();

  fireEvent.click(within(dialog).getByRole("button", { name: "Pay ₹800.00 online" }));
  await waitFor(() => expect(dialog).toHaveTextContent("Paid ₹800.00 by online payment (ref. pay_7)"));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(confirmOnlinePayment).toHaveBeenCalledWith(11, { orderId: "order_7", paymentId: "pay_7", signature: "sig7" });
  expect(within(bills).getAllByRole("listitem")[0]).toHaveTextContent("₹800.00Paid");
  expect(within(bills).queryByRole("button", { name: /Pay bill/ })).not.toBeInTheDocument();
  delete window.Razorpay;
});
