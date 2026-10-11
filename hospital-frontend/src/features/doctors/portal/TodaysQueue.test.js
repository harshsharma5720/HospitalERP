import { act, render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import TodaysQueue from "./TodaysQueue";
import { callNextPatient, callPatient, getMyQueue, patientNotHere } from "../../appointments/api";

jest.mock("../../appointments/api");
// The real form loads the consultation and the patient's history; here it only reports back
jest.mock("../../clinical/ConsultationModal", () => ({ appointment, onClose, onSaved }) => (
  <div role="dialog" aria-label={`Consultation with ${appointment.patientName}`}>
    <button type="button" onClick={() => onSaved(appointment.appointmentID)}>
      Save consultation
    </button>
    <button type="button" onClick={onClose}>
      Close
    </button>
  </div>
));

const TODAY = "2026-10-11";
const visit = (appointmentID, patientName, startTime, extra = {}) => ({
  appointmentID, patientName, date: TODAY, status: "SCHEDULED", age: 34, gender: "FEMALE",
  startTime: `${startTime}:00`, endTime: `${startTime.slice(0, 3)}30:00`, message: null, ...extra,
});
const ASHA = visit(1, "Asha Rao", "09:00", { tokenNumber: 1, checkedInAt: `${TODAY}T08:50:00`, calledAt: `${TODAY}T09:01:00` });
const MEERA = visit(2, "Meera Nair", "10:00", { tokenNumber: 4, checkedInAt: `${TODAY}T09:20:00`, message: "Fever" });
const KIRAN = visit(3, "Kiran Rao", "11:00", { tokenNumber: 7, checkedInAt: `${TODAY}T09:05:00`, gender: "MALE" });
const ROHAN = visit(4, "Rohan Das", "12:00");
const ARJUN = visit(5, "Arjun Das", "08:00", { status: "COMPLETED" });

const queueOf = ({ withDoctor = [ASHA], waiting = [MEERA, KIRAN], notArrived = [ROHAN], done = [ARJUN] } = {}) => ({
  data: { date: TODAY, withDoctor, waiting, notArrived, done },
});

const section = (name) => screen.getByRole("region", { name });

beforeEach(() => getMyQueue.mockResolvedValue(queueOf()));
afterEach(() => jest.resetAllMocks());

test("who is with the doctor, who is waiting by time with their tokens, who hasn't come and who is done", async () => {
  render(<TodaysQueue />);

  expect(await screen.findByText("Sun 11 Oct 2026")).toBeInTheDocument();
  const withYou = section("With you");
  expect(within(withYou).getByText("Asha Rao")).toBeInTheDocument();
  expect(within(withYou).getByLabelText("Token 1")).toBeInTheDocument();
  expect(within(withYou).getByText("34 y · F · 09:00–09:30")).toBeInTheDocument();

  const waiting = section("Waiting (2)");
  const rows = within(waiting).getAllByRole("listitem");
  expect(rows[0]).toHaveTextContent("Meera Nair");
  expect(within(rows[0]).getByLabelText("Token 4")).toBeInTheDocument();
  expect(rows[0]).toHaveTextContent("Fever");
  expect(rows[1]).toHaveTextContent("Kiran Rao");
  expect(within(section("Not arrived yet (1)")).getByText("Rohan Das")).toBeInTheDocument();
  expect(within(section("Done (1)")).getByText("Arjun Das")).toBeInTheDocument();
});

test("Call next calls the first waiting patient in and shows the queue again", async () => {
  callNextPatient.mockResolvedValue({ data: { ...MEERA, calledAt: `${TODAY}T10:02:00` } });
  render(<TodaysQueue />);
  await screen.findByText("Meera Nair");
  getMyQueue.mockResolvedValue(queueOf({ withDoctor: [{ ...MEERA, calledAt: `${TODAY}T10:02:00` }], waiting: [KIRAN] }));

  fireEvent.click(screen.getByRole("button", { name: "Call next" }));

  await waitFor(() => expect(callNextPatient).toHaveBeenCalled());
  expect(await screen.findByRole("status")).toHaveTextContent("Token 4 · Meera Nair called in.");
  await waitFor(() => expect(within(section("With you")).getByText("Meera Nair")).toBeInTheDocument());
  expect(screen.getByRole("region", { name: "Waiting (1)" })).toBeInTheDocument();
});

test("Call a particular patient, and Not here sends one back to waiting", async () => {
  callPatient.mockResolvedValue({ data: { ...KIRAN, calledAt: `${TODAY}T10:03:00` } });
  patientNotHere.mockResolvedValue({ data: { ...ASHA, calledAt: null } });
  render(<TodaysQueue />);
  await screen.findByText("Kiran Rao");

  fireEvent.click(screen.getByRole("button", { name: "Call Kiran Rao" }));
  await waitFor(() => expect(callPatient).toHaveBeenCalledWith(3));
  expect(await screen.findByRole("status")).toHaveTextContent("Token 7 · Kiran Rao called in.");

  fireEvent.click(screen.getByRole("button", { name: "Asha Rao is not here" }));
  await waitFor(() => expect(patientNotHere).toHaveBeenCalledWith(1));
  expect(await screen.findByRole("status")).toHaveTextContent("Asha Rao is waiting again (token 1).");
  expect(getMyQueue.mock.calls.length).toBeGreaterThanOrEqual(3); // shown again after each change
});

test("the consultation form completes the visit; the queue is shown again", async () => {
  render(<TodaysQueue />);
  fireEvent.click(await screen.findByRole("button", { name: "Start consultation" }));

  const form = screen.getByRole("dialog", { name: "Consultation with Asha Rao" });
  getMyQueue.mockResolvedValue(queueOf({ withDoctor: [], done: [ARJUN, { ...ASHA, status: "COMPLETED" }] }));
  fireEvent.click(within(form).getByRole("button", { name: "Save consultation" }));

  expect(await screen.findByRole("status")).toHaveTextContent("Consultation saved; the visit is done.");
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(await screen.findByRole("region", { name: "Done (2)" })).toBeInTheDocument();
});

test("nobody waiting: Call next is off", async () => {
  getMyQueue.mockResolvedValue(queueOf({ withDoctor: [], waiting: [] }));
  render(<TodaysQueue />);

  expect(await screen.findByText("Nobody is waiting.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Call next" })).toBeDisabled();
});

test("refreshes itself every 10 seconds, so patients checked in at the front desk appear", async () => {
  jest.useFakeTimers();
  try {
    getMyQueue.mockResolvedValue(queueOf({ waiting: [] }));
    render(<TodaysQueue />);
    expect(await screen.findByText("Nobody has checked in.")).toBeInTheDocument();
    getMyQueue.mockResolvedValue(queueOf({ waiting: [MEERA] }));

    act(() => {
      jest.advanceTimersByTime(10000);
    });

    expect(await screen.findByText("Meera Nair")).toBeInTheDocument();
    expect(getMyQueue).toHaveBeenCalledTimes(2);
  } finally {
    jest.useRealTimers();
  }
});

test("a refused call shows the server's message", async () => {
  callNextPatient.mockRejectedValue({ response: { status: 409, data: { message: "Nobody is waiting" } } });
  render(<TodaysQueue />);

  fireEvent.click(await screen.findByRole("button", { name: "Call next" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("Nobody is waiting");
});
