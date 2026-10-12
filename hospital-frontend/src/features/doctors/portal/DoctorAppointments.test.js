import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import DoctorAppointments from "./DoctorAppointments";
import useAuthStore from "../../auth/useAuthStore";
import * as appointmentsApi from "../../appointments/api";
import { getPrescriptionPdf } from "../../clinical/api";
import { ConfirmProvider, ToastProvider } from "../../../shared/ui";

jest.mock("../../appointments/api");
jest.mock("../../clinical/api");
// The real form loads the consultation and the patient's history; here it only reports back
jest.mock("../../clinical/ConsultationModal", () => ({ appointment, onSaved }) => (
  <div role="dialog" aria-label={`Consultation with ${appointment.patientName}`}>
    <button type="button" onClick={() => onSaved(appointment.appointmentID)}>
      Save consultation
    </button>
  </div>
));

const visit = (appointmentID, patientName, extra = {}) => ({
  appointmentID, patientName, date: "2026-10-12", startTime: "10:00:00", endTime: "10:30:00", age: 34, message: "Fever", ...extra,
});

const renderPage = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <DoctorAppointments />
      </ConfirmProvider>
    </ToastProvider>
  );

const rowOf = (name) => screen.getByText(name).closest("tr");

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_DOCTOR", userId: 5 });
  appointmentsApi.getDoctorPendingAppointments.mockResolvedValue({ data: [visit(1, "Asha Rao"), visit(2, "Kiran Das", { message: null })] });
  appointmentsApi.getDoctorCompletedAppointments.mockResolvedValue({ data: [visit(3, "Meera Nair", { date: "2026-10-01" })] });
});
afterEach(() => jest.resetAllMocks());

test("shows the doctor's pending visits, and the completed ones on the other tab", async () => {
  renderPage();
  expect(await screen.findByText("Asha Rao")).toBeInTheDocument();
  expect(appointmentsApi.getDoctorPendingAppointments).toHaveBeenCalledWith(5);
  for (const text of ["Mon 12 Oct 2026", "10:00–10:30", "Fever"]) {
    expect(within(rowOf("Asha Rao")).getByText(text)).toBeInTheDocument();
  }
  expect(screen.getByRole("button", { name: "Pending" })).toHaveAttribute("aria-pressed", "true");

  fireEvent.click(screen.getByRole("button", { name: "Completed" }));
  expect(await screen.findByText("Meera Nair")).toBeInTheDocument();
  expect(appointmentsApi.getDoctorCompletedAppointments).toHaveBeenCalledWith(5);
  expect(screen.queryByText("Asha Rao")).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "View / edit notes of Meera Nair" })).toBeInTheDocument();
});

test("completing without notes asks first; Cancel leaves the visit alone", async () => {
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Complete without notes: Asha Rao" }));
  const dialog = screen.getByRole("dialog", { name: "Complete the visit of Asha Rao?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(appointmentsApi.completeAppointment).not.toHaveBeenCalled();
  expect(screen.getByText("Asha Rao")).toBeInTheDocument();
});

test("a confirmed completion takes the visit off the list and says so", async () => {
  appointmentsApi.completeAppointment.mockResolvedValue({ data: "Appointment marked as completed" });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Complete without notes: Asha Rao" }));
  fireEvent.click(screen.getByRole("button", { name: "Complete visit" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Appointment marked as completed");
  expect(appointmentsApi.completeAppointment).toHaveBeenCalledWith(1);
  expect(screen.queryByText("Asha Rao")).not.toBeInTheDocument();
  expect(screen.getByText("Kiran Das")).toBeInTheDocument();
});

test("a failed completion keeps the visit and shows why", async () => {
  appointmentsApi.completeAppointment.mockRejectedValue({ response: { data: { message: "Already completed" } } });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Complete without notes: Asha Rao" }));
  fireEvent.click(screen.getByRole("button", { name: "Complete visit" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Already completed");
  expect(screen.getByText("Asha Rao")).toBeInTheDocument();
});

test("a saved consultation completes the visit", async () => {
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Start consultation with Kiran Das" }));
  fireEvent.click(within(screen.getByRole("dialog", { name: "Consultation with Kiran Das" })).getByText("Save consultation"));
  expect(await screen.findByRole("status")).toHaveTextContent("Consultation saved and appointment completed.");
  expect(screen.queryByText("Kiran Das")).not.toBeInTheDocument();
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

test("a prescription that can't be downloaded says so", async () => {
  getPrescriptionPdf.mockRejectedValue({ response: { data: { message: "No consultation for this visit" } } });
  renderPage();
  fireEvent.click(screen.getByRole("button", { name: "Completed" }));
  fireEvent.click(await screen.findByRole("button", { name: "Prescription PDF of Meera Nair" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("No consultation for this visit");
});

test("nothing pending: says so", async () => {
  appointmentsApi.getDoctorPendingAppointments.mockResolvedValue({ data: [] });
  renderPage();
  expect(await screen.findByText("No pending appointments")).toBeInTheDocument();
});
