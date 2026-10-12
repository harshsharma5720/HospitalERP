import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import AppointmentDetails from "./AppointmentDetails";
import * as appointmentsApi from "./api";
import { getMyHistory, getPrescriptionPdf } from "../clinical/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api");
jest.mock("../clinical/api");
jest.mock("../../shared/ProfilePage", () => () => null);

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
