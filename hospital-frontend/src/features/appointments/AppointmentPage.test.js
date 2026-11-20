import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import AppointmentPage from "./AppointmentPage";
import useAuthStore from "../auth/useAuthStore";
import * as appointmentsApi from "./api";
import { getAllDoctors } from "../doctors/api";
import { getPatientAccount, getRelatives } from "../patients/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { ToastProvider } from "../../shared/ui";
import * as waitlistApi from "../waitlist/api";
import { dayLabel } from "./appointmentFormat";

jest.mock("./api");
jest.mock("../doctors/api");
jest.mock("../patients/api");

const TODAY = toLocalISODate();
const TOMORROW = toLocalISODate(addDays(new Date(), 1));
const SLOTS = [
  { id: 11, startTime: "10:00:00", endTime: "10:30:00" },
  { id: 12, startTime: "10:30:00", endTime: "11:00:00" },
];

const renderAt = (state) =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={[{ pathname: "/appointments", state }]}>
        <Routes>
          <Route path="/appointments" element={<AppointmentPage />} />
          <Route path="/appointment-details" element={<p>my appointments</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_PATIENT", userId: 7 });
  getAllDoctors.mockResolvedValue({
    data: [
      { id: 3, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
      { id: 4, name: "Sunil Iyer", specialist: "ORTHOPEDICS", active: true },
    ],
  });
  getPatientAccount.mockResolvedValue({ data: { patientId: 41, patientName: "Asha Verma", gender: "FEMALE", dob: "1990-04-12" } });
  getRelatives.mockResolvedValue({ data: [{ id: 5, name: "Ravi Verma", relationship: "FATHER", gender: "MALE", dob: "1958-01-01" }] });
  appointmentsApi.getAvailableSlots.mockResolvedValue({ data: SLOTS });
  appointmentsApi.bookAppointment.mockResolvedValue({ data: {} });
});
afterEach(() => jest.resetAllMocks());

const chooseTime = async (label = "10:30") => fireEvent.click(await screen.findByRole("radio", { name: label }));

test("books for the patient: the doctor from the doctor's page, today's morning, a free time", async () => {
  renderAt({ doctorName: "Anita Rao" });
  expect(await screen.findByRole("radio", { name: /Asha Verma/ })).toBeChecked();
  await waitFor(() => expect(screen.getByLabelText("Doctor")).toHaveValue("3"));
  await waitFor(() => expect(appointmentsApi.getAvailableSlots).toHaveBeenCalledWith("3", TODAY, "MORNING"));
  const confirm = screen.getByRole("button", { name: "Confirm booking" });
  expect(confirm).toBeDisabled();

  await chooseTime();
  fireEvent.change(screen.getByLabelText(/What would you like to see the doctor about/), { target: { value: " Fever " } });
  expect(within(screen.getByRole("region", { name: "Summary" })).getByText(/10:30–11:00/)).toBeInTheDocument();
  fireEvent.click(confirm);

  expect(await screen.findByRole("heading", { name: "Appointment booked" })).toBeInTheDocument();
  expect(appointmentsApi.bookAppointment).toHaveBeenCalledWith({
    patientName: "Asha Verma", gender: "FEMALE", age: expect.any(Number), message: "Fever", relativeId: null, slotId: 12,
  });
  expect(screen.getByRole("link", { name: "View my appointments" })).toHaveAttribute("href", "/appointment-details");
});

test("books for a family member", async () => {
  renderAt();
  fireEvent.click(await screen.findByRole("radio", { name: /Ravi Verma/ }));
  expect(screen.getByRole("radio", { name: /Ravi Verma/ })).toHaveAccessibleName(/Father/);
  fireEvent.change(screen.getByLabelText("Doctor"), { target: { value: "4" } });
  await chooseTime("10:00");
  fireEvent.click(screen.getByRole("button", { name: "Confirm booking" }));
  await screen.findByRole("heading", { name: "Appointment booked" });
  expect(appointmentsApi.bookAppointment).toHaveBeenCalledWith(expect.objectContaining({ patientName: "Ravi Verma", relativeId: 5, slotId: 11 }));
});

test("a profile without date of birth or gender asks for them", async () => {
  getPatientAccount.mockResolvedValue({ data: { patientId: 41, patientName: "Asha Verma", gender: null, dob: null } });
  getRelatives.mockResolvedValue({ data: [] });
  renderAt({ doctorId: 3 });
  await chooseTime();
  const confirm = screen.getByRole("button", { name: "Confirm booking" });
  expect(confirm).toBeDisabled();
  fireEvent.change(screen.getByLabelText("Gender"), { target: { value: "FEMALE" } });
  fireEvent.change(screen.getByLabelText("Age"), { target: { value: "35" } });
  fireEvent.click(confirm);
  await screen.findByRole("heading", { name: "Appointment booked" });
  expect(appointmentsApi.bookAppointment).toHaveBeenCalledWith(expect.objectContaining({ gender: "FEMALE", age: 35 }));
});

test("another day or the evening loads its free times; none says so", async () => {
  renderAt({ doctorId: 3 });
  await screen.findByRole("radio", { name: "10:00" });
  appointmentsApi.getAvailableSlots.mockResolvedValue({ data: [] });
  fireEvent.click(within(screen.getByRole("group", { name: "Day" })).getByRole("button", { name: "Tomorrow" }));
  fireEvent.click(within(screen.getByRole("group", { name: "Part of the day" })).getByRole("button", { name: "Evening" }));
  await waitFor(() => expect(appointmentsApi.getAvailableSlots).toHaveBeenLastCalledWith("3", TOMORROW, "EVENING"));
  expect(await screen.findByText(/No free times then/)).toBeInTheDocument();
  expect(within(screen.getByRole("group", { name: "Day" })).getAllByRole("button")).toHaveLength(7);
});

test("a time taken meanwhile: the error is shown and the free times are loaded again", async () => {
  appointmentsApi.bookAppointment.mockRejectedValue({ response: { data: { message: "This slot is already booked" } } });
  renderAt({ doctorId: 3 });
  await chooseTime();
  const calls = appointmentsApi.getAvailableSlots.mock.calls.length;
  fireEvent.click(screen.getByRole("button", { name: "Confirm booking" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("This slot is already booked");
  await waitFor(() => expect(appointmentsApi.getAvailableSlots.mock.calls.length).toBe(calls + 1));
  expect(screen.queryByRole("heading", { name: "Appointment booked" })).not.toBeInTheDocument();
});

test("rescheduling keeps the person, moves the visit and goes back to my appointments", async () => {
  appointmentsApi.rescheduleAppointment.mockResolvedValue({ data: {} });
  renderAt({
    rescheduleAppointment: {
      appointmentID: 90, patientName: "Ravi Verma", doctorId: 3, doctorName: "Anita Rao", shift: "EVENING", date: TOMORROW,
      startTime: "17:00:00", message: "Follow-up",
    },
  });
  expect(screen.getByRole("heading", { level: 1, name: "Choose a new time" })).toBeInTheDocument();
  expect(screen.queryByRole("radio", { name: /Asha Verma/ })).not.toBeInTheDocument();
  await waitFor(() => expect(appointmentsApi.getAvailableSlots).toHaveBeenCalledWith("3", TODAY, "EVENING"));
  await chooseTime("10:00");
  fireEvent.click(screen.getByRole("button", { name: "Move appointment" }));
  expect(await screen.findByText("my appointments")).toBeInTheDocument();
  expect(appointmentsApi.rescheduleAppointment).toHaveBeenCalledWith(90, { slotId: 11, message: "Follow-up" });
  expect(getPatientAccount).not.toHaveBeenCalled();
});

// ---- The waitlist (docs/WAITLIST_PLAN.md); its api is mocked for every test (setupTests.js)

const renderUrl = (url, state) =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={[{ pathname: "/appointments", search: url, state }]}>
        <Routes>
          <Route path="/appointments" element={<AppointmentPage />} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

test("a link chooses the doctor, a later day and the evening; a full time offers the waitlist", async () => {
  const later = toLocalISODate(addDays(new Date(), 12));
  appointmentsApi.getAvailableSlots.mockResolvedValue({ data: [] });
  waitlistApi.joinWaitlist.mockResolvedValue({ data: {} });
  renderUrl(`?doctorId=3&date=${later}&shift=EVENING`);

  await waitFor(() => expect(appointmentsApi.getAvailableSlots).toHaveBeenCalledWith("3", later, "EVENING"));
  expect(screen.getByRole("button", { name: dayLabel(later) })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: /Evening/ })).toHaveAttribute("aria-pressed", "true");
  expect(await screen.findByText("Full? Join the waitlist")).toBeInTheDocument();
  expect(screen.getByLabelText("When")).toHaveValue("EVENING");
  fireEvent.change(screen.getByLabelText("When"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Join the waitlist" }));

  await waitFor(() =>
    expect(waitlistApi.joinWaitlist).toHaveBeenCalledWith({ doctorId: 3, date: later, shift: null, relativeId: null })
  );
  expect(await screen.findByText("Asha Verma is on the waitlist. We'll tell you when a time frees up.")).toBeInTheDocument();
  expect(waitlistApi.getMyWaits).toHaveBeenCalledTimes(2); // loaded again
  expect(appointmentsApi.bookAppointment).not.toHaveBeenCalled();
});

test("for a family member; a refused join says why", async () => {
  appointmentsApi.getAvailableSlots.mockResolvedValue({ data: [] });
  waitlistApi.joinWaitlist.mockRejectedValue({ response: { status: 409, data: { message: "There are free times then. Please book one." } } });
  renderUrl(`?doctorId=4&date=${TOMORROW}`);
  fireEvent.click(await screen.findByRole("radio", { name: /Ravi Verma/ }));
  expect(await screen.findByLabelText("When")).toHaveValue("MORNING");
  fireEvent.click(screen.getByRole("button", { name: "Join the waitlist" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("There are free times then. Please book one.");
  expect(waitlistApi.joinWaitlist).toHaveBeenCalledWith({ doctorId: 4, date: TOMORROW, shift: "MORNING", relativeId: 5 });
  expect(waitlistApi.getMyWaits).toHaveBeenCalledTimes(1);
  // Another person: the reason was about Ravi, it goes; their "When" starts afresh
  fireEvent.change(screen.getByLabelText("When"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("radio", { name: /Asha Verma/ }));
  await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
  expect(screen.getByLabelText("When")).toHaveValue("MORNING");
});

test("already on the waitlist for that person, doctor and day: it shows, and they can leave", async () => {
  appointmentsApi.getAvailableSlots.mockResolvedValue({ data: [] });
  waitlistApi.getMyWaits.mockResolvedValue({
    data: [
      { id: 9, doctorId: 3, date: TODAY, relativeId: null, shift: null, name: "Asha Verma" },
      { id: 10, doctorId: 3, date: TODAY, relativeId: 5, shift: "MORNING", name: "Ravi Verma" },
    ],
  });
  waitlistApi.leaveWaitlist.mockResolvedValue({});
  renderUrl("?doctorId=3");
  expect(await screen.findByText("Asha Verma is on the waitlist")).toBeInTheDocument();
  expect(screen.getByText(/any time that day\. We'll tell you/)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Join the waitlist" })).not.toBeInTheDocument();

  // Her father's own wait: the morning
  fireEvent.click(screen.getByRole("radio", { name: /Ravi Verma/ }));
  expect(await screen.findByText("Ravi Verma is on the waitlist")).toBeInTheDocument();
  expect(screen.getByText(/morning\. We'll tell you/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Leave the waitlist" }));
  await waitFor(() => expect(waitlistApi.leaveWaitlist).toHaveBeenCalledWith(10));
  expect(await screen.findByText("You've left the waitlist.")).toBeInTheDocument();
  expect(waitlistApi.getMyWaits).toHaveBeenCalledTimes(2);

  // Another day, then another doctor: not on those waitlists
  fireEvent.click(screen.getByRole("button", { name: "Tomorrow" }));
  expect(await screen.findByText("Full? Join the waitlist")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Today" }));
  expect(await screen.findByText("Ravi Verma is on the waitlist")).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Doctor"), { target: { value: "4" } });
  expect(await screen.findByText("Full? Join the waitlist")).toBeInTheDocument();

  // "When" follows the part of the day shown
  expect(screen.getByLabelText("When")).toHaveValue("MORNING");
  fireEvent.click(screen.getByRole("button", { name: /Evening/ }));
  await waitFor(() => expect(screen.getByLabelText("When")).toHaveValue("EVENING"));
});

test("a link to a day that can't be booked opens today; no waitlist with free times or when moving a visit", async () => {
  const { unmount } = renderUrl(`?doctorId=3&date=${toLocalISODate(addDays(new Date(), 30))}&shift=NIGHT`); // the window: 30 days
  await waitFor(() => expect(appointmentsApi.getAvailableSlots).toHaveBeenCalledWith("3", TODAY, "MORNING"));
  await screen.findByRole("radio", { name: "10:00" });
  expect(screen.queryByText("Full? Join the waitlist")).not.toBeInTheDocument();
  unmount();

  renderUrl(`?doctorId=3&date=${toLocalISODate(addDays(new Date(), -1))}&shift=EVENING`);
  await waitFor(() => expect(appointmentsApi.getAvailableSlots).toHaveBeenLastCalledWith("3", TODAY, "EVENING"));
});

test("moving a visit to a full day offers no waitlist", async () => {
  appointmentsApi.getAvailableSlots.mockResolvedValue({ data: [] });
  renderUrl("", {
    rescheduleAppointment: { appointmentID: 90, doctorId: 3, doctorName: "Anita Rao", patientName: "Asha Verma", date: TOMORROW, shift: "MORNING" },
  });
  expect(await screen.findByText(/No free times then/)).toBeInTheDocument();
  expect(screen.queryByText("Full? Join the waitlist")).not.toBeInTheDocument();
  expect(waitlistApi.getMyWaits).not.toHaveBeenCalled();
});
