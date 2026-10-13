import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import AppointmentDetails from "./AppointmentDetails";
import * as appointmentsApi from "./api";
import { getMyHistory, getPrescriptionPdf } from "../clinical/api";
import { getMyBills, getMyReceiptPdf } from "../billing/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api");
jest.mock("../clinical/api");
jest.mock("../billing/api");

const day = (n) => toLocalISODate(addDays(new Date(), n));
const visit = (id, date, status, extra = {}) => ({
  appointmentID: id, date, status, startTime: "10:00:00", endTime: "10:30:00", patientName: "Asha Verma",
  doctorName: "Anita Rao", doctorId: 3, message: null, ...extra,
});
const APPOINTMENTS = [
  visit(1, day(5), "SCHEDULED"),
  visit(2, day(1), "CONFIRMED", { patientName: "Ravi Verma", message: "Knee pain" }),
  visit(3, day(-10), "COMPLETED"),
  visit(4, day(-3), "SCHEDULED"), // never completed: missed
  visit(5, day(-1), "CANCELLED_BY_DOCTOR"),
];

function Where() {
  const location = useLocation();
  return <p data-testid="where">{JSON.stringify({ path: location.pathname, state: location.state })}</p>;
}

const renderPage = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter initialEntries={["/appointment-details"]}>
          <Routes>
            <Route path="/appointment-details" element={<AppointmentDetails />} />
            <Route path="/appointments" element={<Where />} />
          </Routes>
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );

const cards = () => screen.getAllByRole("article");

beforeEach(() => {
  appointmentsApi.getMyAppointments.mockResolvedValue({ data: APPOINTMENTS });
  getMyHistory.mockResolvedValue({
    data: [{ appointmentId: 3, diagnosis: "Viral fever", followUpDate: null, symptoms: "Fever", notes: "Rest", medicines: [{ medicineName: "Paracetamol", dosage: "500 mg" }] }],
  });
  getMyBills.mockResolvedValue({ data: [] });
});
afterEach(() => jest.resetAllMocks());

test("upcoming visits first, soonest first, each with Reschedule and Cancel", async () => {
  renderPage();
  await screen.findAllByRole("article");
  expect(screen.getByRole("button", { name: "Upcoming (2)" })).toHaveAttribute("aria-pressed", "true");
  expect(cards()).toHaveLength(2);
  expect(cards()[0]).toHaveTextContent("for Ravi Verma");
  expect(cards()[0]).toHaveTextContent("Knee pain");
  expect(within(cards()[1]).getByRole("button", { name: "Reschedule" })).toBeInTheDocument();
  expect(within(cards()[1]).getByRole("button", { name: "Cancel" })).toBeInTheDocument();
});

test("past visits, newest first: missed, cancelled by the doctor, completed with the doctor's notes", async () => {
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Past (3)" }));
  const [cancelled, missed, completed] = cards();
  expect(cancelled).toHaveTextContent("Cancelled by doctor");
  expect(missed).toHaveTextContent("Missed");
  expect(completed).toHaveTextContent("Viral fever");
  expect(within(missed).queryByRole("button", { name: /Prescription/ })).not.toBeInTheDocument();
  fireEvent.click(within(completed).getByRole("button", { name: /The doctor's notes \(1 medicines\)/ }));
  expect(completed).toHaveTextContent("Paracetamol — 500 mg");
});

test("cancelling asks first; Keep it does nothing, confirming cancels and moves the visit to the past", async () => {
  appointmentsApi.cancelAppointment.mockResolvedValue({ data: "Appointment cancelled successfully" });
  renderPage();
  await screen.findAllByRole("article");
  fireEvent.click(within(cards()[0]).getByRole("button", { name: "Cancel" }));
  const dialog = screen.getByRole("dialog", { name: "Cancel this appointment?" });
  expect(dialog).toHaveTextContent("with Dr. Anita Rao");
  fireEvent.click(within(dialog).getByRole("button", { name: "Keep it" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(appointmentsApi.cancelAppointment).not.toHaveBeenCalled();

  fireEvent.click(within(cards()[0]).getByRole("button", { name: "Cancel" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Cancel appointment" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Appointment cancelled successfully");
  expect(appointmentsApi.cancelAppointment).toHaveBeenCalledWith(2);
  expect(screen.getByRole("button", { name: "Upcoming (1)" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Past (4)" })).toBeInTheDocument();
});

test("Reschedule opens the booking page with the visit", async () => {
  renderPage();
  await screen.findAllByRole("article");
  fireEvent.click(within(cards()[0]).getByRole("button", { name: "Reschedule" }));
  expect(screen.getByTestId("where")).toHaveTextContent('"rescheduleAppointment":{"appointmentID":2');
});

test("Book again keeps the doctor", async () => {
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Past (3)" }));
  fireEvent.click(within(cards()[2]).getByRole("button", { name: "Book again" }));
  expect(screen.getByTestId("where")).toHaveTextContent('"doctorName":"Anita Rao","doctorId":3');
});

test("a prescription that can't be downloaded says so", async () => {
  getPrescriptionPdf.mockRejectedValue({ response: { data: { message: "Prescription not found" } } });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Past (3)" }));
  fireEvent.click(within(cards()[2]).getByRole("button", { name: "Prescription" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Prescription not found");
});

test("when the list can't be loaded it says so and can try again; nothing upcoming offers to book", async () => {
  appointmentsApi.getMyAppointments.mockRejectedValueOnce(new Error("offline"));
  appointmentsApi.getMyAppointments.mockResolvedValueOnce({ data: [visit(3, day(-10), "COMPLETED")] });
  renderPage();
  expect(await screen.findByText("We couldn't load your appointments")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("No upcoming appointments")).toBeInTheDocument();
  expect(screen.getAllByRole("link", { name: "Book an appointment" }).every((l) => l.getAttribute("href") === "/appointments")).toBe(true);
});

test("a visit later today is still upcoming", async () => {
  appointmentsApi.getMyAppointments.mockResolvedValue({ data: [visit(8, day(0), "SCHEDULED", { startTime: "23:00:00" })] });
  renderPage();
  expect(await screen.findByRole("button", { name: "Upcoming (1)" })).toBeInTheDocument();
  expect(within(cards()[0]).getByRole("button", { name: "Reschedule" })).toBeInTheDocument();
});

// ---- Bills (docs/PATIENT_BILLS_PLAN.md)

const bill = (id, appointmentId, status, extra = {}) => ({
  id, billNumber: `B-2026-0000${id}`, appointmentId, patientName: "Asha Verma", doctorName: "Anita Rao", visitDate: day(-10), status,
  items: [{ description: "Consultation — Dr. Anita Rao", amount: 600 }], subtotal: 600, discount: 0, total: 600,
  paidAt: null, paymentMethod: null, paymentReference: null, refundedAt: null, refundMethod: null, refundReference: null, ...extra,
});

test("a visit's bill shows its total and status; Bill opens it with the lines, the payment and the receipt", async () => {
  URL.createObjectURL = jest.fn(() => "blob:receipt");
  URL.revokeObjectURL = jest.fn();
  const click = jest.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  getMyBills.mockResolvedValue({
    data: [
      bill(21, 3, "PAID", {
        items: [{ description: "Consultation — Dr. Anita Rao", amount: 600 }, { description: "Dressing", amount: 150 }],
        subtotal: 750, discount: 50, total: 700, paidAt: `${day(-10)}T10:05:31`, paymentMethod: "UPI", paymentReference: "UPI-1234",
      }),
    ],
  });
  getMyReceiptPdf.mockResolvedValue({ data: "%PDF-" });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Past (3)" }));
  const [cancelled, missed, completed] = cards();
  expect(completed).toHaveTextContent("Bill₹700.00Paid");
  expect(within(missed).queryByRole("button", { name: /Bill/ })).not.toBeInTheDocument();
  expect(within(cancelled).queryByRole("button", { name: /Bill/ })).not.toBeInTheDocument();

  fireEvent.click(within(completed).getByRole("button", { name: "Bill B-2026-000021" }));
  const dialog = screen.getByRole("dialog", { name: /Bill B-2026-000021/ });
  expect(dialog).toHaveTextContent("For Asha Verma · Dr. Anita Rao");
  const items = within(dialog).getAllByRole("listitem");
  expect(items.map((i) => i.textContent)).toEqual(["Consultation — Dr. Anita Rao₹600.00", "Dressing₹150.00"]);
  expect(dialog).toHaveTextContent("Subtotal₹750.00Discount− ₹50.00Total₹700.00");
  expect(dialog).toHaveTextContent(/Paid ₹700\.00 by UPI \(ref\. UPI-1234\) on .+, 10:05\./);
  expect(dialog).not.toHaveTextContent("front desk");

  fireEvent.click(within(dialog).getByRole("button", { name: "Download receipt" }));
  await waitFor(() => expect(click).toHaveBeenCalled());
  expect(getMyReceiptPdf).toHaveBeenCalledWith(21);
  expect(click.mock.instances[0].download).toBe("receipt-B-2026-000021.pdf");
  fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  click.mockRestore();
});

test("today's checked-in visit: an unpaid bill is paid at the desk and has no receipt yet; a cancelled paid visit is refunded", async () => {
  appointmentsApi.getMyAppointments.mockResolvedValue({
    data: [visit(8, day(0), "CONFIRMED", { startTime: "23:00:00" }), visit(5, day(-1), "CANCELLED_BY_DOCTOR")],
  });
  getMyBills.mockResolvedValue({
    data: [bill(30, 8, "UNPAID", { visitDate: day(0) }), bill(29, 5, "REFUND_DUE", { visitDate: day(-1), paidAt: `${day(-1)}T09:00:00`, paymentMethod: "CASH" })],
  });
  renderPage();
  await screen.findAllByRole("article");
  expect(cards()[0]).toHaveTextContent("Bill₹600.00Unpaid");
  fireEvent.click(within(cards()[0]).getByRole("button", { name: "Bill B-2026-000030" }));
  let dialog = screen.getByRole("dialog", { name: /Bill B-2026-000030/ });
  expect(dialog).toHaveTextContent("Please pay ₹600.00 at the front desk.");
  expect(dialog).not.toHaveTextContent("Subtotal");
  expect(within(dialog).queryByRole("button", { name: "Download receipt" })).not.toBeInTheDocument();
  fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));

  fireEvent.click(screen.getByRole("button", { name: "Past (1)" }));
  fireEvent.click(within(cards()[0]).getByRole("button", { name: "Bill B-2026-000029" }));
  dialog = screen.getByRole("dialog", { name: /Bill B-2026-000029/ });
  expect(dialog).toHaveTextContent("Paid ₹600.00 by Cash");
  expect(dialog).toHaveTextContent("The visit was cancelled: the front desk will refund ₹600.00.");
  expect(within(dialog).getByRole("button", { name: "Download receipt" })).toBeInTheDocument();
});

test("the appointments show even when the bills can't be loaded", async () => {
  getMyBills.mockRejectedValue(new Error("offline"));
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Past (3)" }));
  expect(cards()[2]).toHaveTextContent("Viral fever");
  expect(screen.queryByRole("button", { name: /Bill/ })).not.toBeInTheDocument();
});

test("a receipt that can't be downloaded says so", async () => {
  getMyBills.mockResolvedValue({ data: [bill(21, 3, "PAID", { paidAt: `${day(-10)}T10:05:31`, paymentMethod: "CARD" })] });
  getMyReceiptPdf.mockRejectedValue({ response: { data: { message: "This bill is unpaid: there is no receipt" } } });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Past (3)" }));
  fireEvent.click(screen.getByRole("button", { name: "Bill B-2026-000021" }));
  fireEvent.click(screen.getByRole("button", { name: "Download receipt" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("This bill is unpaid: there is no receipt");
});
