import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import WalkInBooking, { dayLabel, formatDate } from "./WalkInBooking";
import * as appointmentsApi from "./api";
import { getAllDoctors } from "../doctors/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";

jest.mock("./api");
jest.mock("../doctors/api");

const TOMORROW = toLocalISODate(addDays(new Date(), 1));
const LATER = toLocalISODate(addDays(new Date(), 3));

const slot = (slotId, date, startTime, endTime, shift = "MORNING") => ({ slotId, date, shift, startTime, endTime });
const SLOTS = [
  slot(101, TOMORROW, "10:00:00", "10:30:00"),
  slot(102, TOMORROW, "16:00:00", "16:30:00", "EVENING"),
  slot(103, LATER, "09:00:00", "09:30:00"),
];

const DOCTORS = [
  { id: 3, userName: "drrao", name: "Rao", specialist: "CARDIOLOGY", active: true },
  { id: 4, userName: "drgone", name: "Gone", specialist: "NEUROLOGY", active: false },
];

const ARJUN = {
  patientId: 7, patientName: "Arjun Das", gender: "MALE", dob: "1990-01-15", email: "arjun@example.com",
  contactNo: "+919000011121", hasLogin: true,
};
const MIRA = {
  patientId: 8, patientName: "Mira Das", gender: "FEMALE", dob: null, email: null, contactNo: "9000011121",
  hasLogin: false,
};

const APPOINTMENT = {
  appointmentID: 90, patientName: "Lakshmi Iyer", doctorName: "Rao", date: TOMORROW,
  startTime: "10:00:00", endTime: "10:30:00", status: "SCHEDULED",
};
const booked = (overrides = {}) => ({ data: { patientId: 41, newPatient: true, appointment: APPOINTMENT, ...overrides } });

beforeEach(() => {
  getAllDoctors.mockResolvedValue({ data: DOCTORS });
  appointmentsApi.findPatientsByPhone.mockResolvedValue({ data: [] });
  appointmentsApi.getNextFreeSlots.mockResolvedValue({ data: SLOTS });
  appointmentsApi.bookWalkIn.mockResolvedValue(booked());
});
afterEach(() => jest.resetAllMocks());

const findPhone = async (phone) => {
  fireEvent.change(screen.getByLabelText("Phone number"), { target: { value: phone } });
  fireEvent.click(screen.getByRole("button", { name: "Find" }));
  await waitFor(() => expect(appointmentsApi.findPatientsByPhone).toHaveBeenCalledWith(phone));
};

const chooseDoctor = async (firstTime = "Tomorrow 10:00") => {
  await screen.findByRole("option", { name: "Dr. Rao — Cardiology" });
  fireEvent.change(screen.getByLabelText("Doctor"), { target: { value: "3" } });
  if (firstTime) await screen.findByRole("radio", { name: firstTime });
};

test("a new number: registers the patient and books the earliest free time", async () => {
  render(<WalkInBooking />);
  await findPhone("+91 90000 11120");

  expect(await screen.findByText(/No patient with this number yet/)).toBeInTheDocument();
  expect(screen.getByText("+91 90000 11120")).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: " Lakshmi Iyer " } });
  fireEvent.change(screen.getByLabelText("Gender"), { target: { value: "FEMALE" } });
  await chooseDoctor();
  expect(screen.queryByRole("option", { name: /Gone/ })).not.toBeInTheDocument(); // deactivated
  expect(appointmentsApi.getNextFreeSlots).toHaveBeenCalledWith("3", 6);
  expect(screen.getByRole("radio", { name: "Tomorrow 10:00" })).toBeChecked();
  expect(screen.getByRole("radio", { name: `${formatDate(LATER)} 09:00` })).not.toBeChecked();
  fireEvent.change(screen.getByLabelText("Age"), { target: { value: "52" } });
  fireEvent.change(screen.getByLabelText(/Reason for the visit/), { target: { value: "Fever " } });
  fireEvent.click(screen.getByRole("button", { name: "Register and book" }));

  await waitFor(() =>
    expect(appointmentsApi.bookWalkIn).toHaveBeenCalledWith({
      newPatient: { name: "Lakshmi Iyer", phone: "+91 90000 11120", gender: "FEMALE", dob: null, email: null },
      slotId: 101,
      age: 52,
      message: "Fever",
    })
  );
  const confirmation = await screen.findByRole("status");
  expect(confirmation).toHaveTextContent("Lakshmi Iyer with Dr. Rao — Cardiology");
  expect(confirmation).toHaveTextContent(`${formatDate(TOMORROW, true)}, 10:00–10:30`);
  expect(confirmation).toHaveTextContent("New patient record created. An SMS confirmation is sent to +91 90000 11120.");

  fireEvent.click(screen.getByRole("button", { name: "Next patient" }));
  expect(screen.getByLabelText("Phone number")).toHaveValue("");
  expect(screen.queryByText("Visit")).not.toBeInTheDocument();
});

test("a shared number lists the family; booking for the one chosen needs an age when no birth date is known", async () => {
  appointmentsApi.findPatientsByPhone.mockResolvedValue({ data: [ARJUN, MIRA] });
  appointmentsApi.bookWalkIn.mockResolvedValue(
    booked({ patientId: 8, newPatient: false, appointment: { ...APPOINTMENT, patientName: "Mira Das" } })
  );
  render(<WalkInBooking />);
  await findPhone("9000011121");

  const arjunAge = calculateAgeFromDOB("1990-01-15");
  expect((await screen.findByText("Arjun Das")).closest("li")).toHaveTextContent(
    `Male · ${arjunAge} years · arjun@example.com · App account`
  );
  expect(screen.getByText("Mira Das").closest("li")).toHaveTextContent("Female · Front-desk record");
  expect(screen.queryByLabelText("Name")).not.toBeInTheDocument(); // no new-patient form unless asked for

  // The age comes from the date of birth when there is one
  fireEvent.click(screen.getByRole("button", { name: "Book for Arjun Das" }));
  expect(screen.getByLabelText("Age")).toHaveValue(arjunAge);

  fireEvent.click(screen.getByRole("button", { name: "Book for Mira Das" }));
  expect(screen.getByRole("button", { name: "Book for Mira Das" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByLabelText("Age")).toHaveValue(null);
  await chooseDoctor();
  fireEvent.click(screen.getByRole("radio", { name: "Tomorrow 16:00" }));
  fireEvent.click(screen.getByRole("button", { name: "Book" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Please enter the patient's age.");
  expect(appointmentsApi.bookWalkIn).not.toHaveBeenCalled();

  fireEvent.change(screen.getByLabelText("Age"), { target: { value: "8" } });
  fireEvent.click(screen.getByRole("button", { name: "Book" }));

  await waitFor(() =>
    expect(appointmentsApi.bookWalkIn).toHaveBeenCalledWith({ patientId: 8, slotId: 102, age: 8, message: null })
  );
  const confirmation = await screen.findByRole("status");
  expect(confirmation).toHaveTextContent("Mira Das with Dr. Rao");
  expect(confirmation).not.toHaveTextContent("New patient record");
});

test("a time taken meanwhile: says so and offers the times still free", async () => {
  appointmentsApi.findPatientsByPhone.mockResolvedValue({ data: [MIRA] });
  appointmentsApi.bookWalkIn.mockRejectedValue({
    response: { status: 409, data: { message: "This slot is no longer available. Please choose another one." } },
  });
  render(<WalkInBooking />);
  await findPhone("9000011121");
  fireEvent.click(await screen.findByRole("button", { name: "Book for Mira Das" }));
  await chooseDoctor();
  fireEvent.change(screen.getByLabelText("Age"), { target: { value: "8" } });

  appointmentsApi.getNextFreeSlots.mockResolvedValue({ data: SLOTS.slice(1) });
  fireEvent.click(screen.getByRole("button", { name: "Book" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("This slot is no longer available");
  expect(await screen.findByRole("radio", { name: "Tomorrow 16:00" })).toBeChecked();
  expect(screen.queryByRole("radio", { name: "Tomorrow 10:00" })).not.toBeInTheDocument();
  expect(appointmentsApi.getNextFreeSlots).toHaveBeenCalledTimes(2);
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

test("the server's message for a wrong number; a new patient needs a name and gender", async () => {
  appointmentsApi.findPatientsByPhone.mockRejectedValueOnce({
    response: { status: 400, data: { message: "Please enter at least 10 digits of the phone number" } },
  });
  render(<WalkInBooking />);
  await findPhone("12345");
  expect(await screen.findByRole("alert")).toHaveTextContent("Please enter at least 10 digits of the phone number");
  expect(screen.queryByText("Patient")).not.toBeInTheDocument();

  await findPhone("9000011122");
  await chooseDoctor();
  fireEvent.click(screen.getByRole("button", { name: "Register and book" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("Please enter the patient's name and gender.");
  expect(appointmentsApi.bookWalkIn).not.toHaveBeenCalled();
});

test("More times asks for up to 20; a doctor without free times says so", async () => {
  const six = [0, 1, 2, 3, 4, 5].map((i) => slot(200 + i, LATER, `1${i}:00:00`, `1${i}:30:00`));
  appointmentsApi.getNextFreeSlots.mockResolvedValueOnce({ data: six }).mockResolvedValueOnce({ data: [] });
  render(<WalkInBooking />);
  await findPhone("9000011122");
  await chooseDoctor(`${formatDate(LATER)} 10:00`);

  fireEvent.click(screen.getByRole("button", { name: "More times" }));

  expect(await screen.findByText(/No free times in the next 30 days/)).toBeInTheDocument();
  expect(appointmentsApi.getNextFreeSlots).toHaveBeenLastCalledWith("3", 20);
});

test("dates read as today, tomorrow, or weekday day month", () => {
  const monday = new Date(2026, 9, 5); // Mon 5 Oct 2026
  expect(dayLabel("2026-10-05", monday)).toBe("Today");
  expect(dayLabel("2026-10-06", monday)).toBe("Tomorrow");
  expect(dayLabel("2026-10-09", monday)).toBe("Fri 9 Oct");
  expect(formatDate("2026-10-09", true)).toBe("Fri 9 Oct 2026");
});
