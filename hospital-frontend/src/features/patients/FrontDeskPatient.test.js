import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import FrontDeskPatient from "./FrontDeskPatient";
import * as patientsApi from "./api";
import { getFrontDeskAppointmentPage } from "../appointments/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";

jest.mock("./api");
jest.mock("../appointments/api");

const TODAY = toLocalISODate();
const TOMORROW = toLocalISODate(addDays(new Date(), 1));
const LAST_WEEK = toLocalISODate(addDays(new Date(), -7));

const ARJUN = {
  patientId: 7, patientName: "Arjun Das", gender: "MALE", dob: "1990-01-15", contactNo: "+919000011121",
  email: "arjun@example.com", patientAddress: "12 MG Road", patientAadharNo: 123456789012, hasLogin: true,
};

const visit = (appointmentID, date, status, extra = {}) => ({
  appointmentID, date, status, startTime: "10:00:00", endTime: "10:30:00", doctorName: "Rao", patientName: "Arjun Das",
  ...extra,
});
const pageOf = (entries, { totalPages = 1, totalEntries = entries.length } = {}) => ({
  data: { entries, page: 0, size: 10, totalEntries, totalPages },
});

const renderAt = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/patients/:patientId" element={<FrontDeskPatient />} />
        <Route path="/admin/patients/:patientId" element={<FrontDeskPatient />} />
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => {
  patientsApi.getFrontDeskPatient.mockResolvedValue({ data: ARJUN });
  getFrontDeskAppointmentPage.mockImplementation((params) =>
    Promise.resolve(
      params.status === "upcoming"
        ? pageOf([visit(1, TOMORROW, "SCHEDULED")])
        : pageOf(
            [
              visit(1, TOMORROW, "SCHEDULED"),
              visit(2, LAST_WEEK, "COMPLETED"),
              visit(3, LAST_WEEK, "SCHEDULED", { patientName: "Mira Das", startTime: null, endTime: null }),
            ],
            { totalPages: 2, totalEntries: 12 }
          )
    )
  );
});
afterEach(() => jest.resetAllMocks());

test("details, upcoming appointments and every appointment newest first", async () => {
  renderAt("/patients/7");

  expect(await screen.findByRole("heading", { name: "Arjun Das" })).toBeInTheDocument();
  expect(patientsApi.getFrontDeskPatient).toHaveBeenCalledWith("7");
  expect(screen.getByText("#7 · App account")).toBeInTheDocument();
  const details = screen.getByRole("region", { name: "Details" });
  for (const text of [
    `${calculateAgeFromDOB("1990-01-15")} years (born Mon 15 Jan 1990)`, "+919000011121", "arjun@example.com",
    "12 MG Road", "XXXX XXXX 9012",
  ]) {
    expect(within(details).getByText(text)).toBeInTheDocument();
  }
  expect(screen.queryByText("123456789012")).not.toBeInTheDocument(); // Aadhaar: only the last 4 digits

  expect(getFrontDeskAppointmentPage).toHaveBeenCalledWith({ patientId: "7", status: "upcoming", from: TODAY, size: 20 });
  const upcoming = screen.getByRole("region", { name: "Upcoming appointments" });
  expect(await within(upcoming).findByText("Tomorrow 10:00–10:30")).toBeInTheDocument();
  expect(within(upcoming).getByText("Scheduled")).toBeInTheDocument();

  expect(getFrontDeskAppointmentPage).toHaveBeenCalledWith({ patientId: "7", order: "desc", page: 0, size: 10 });
  const history = screen.getByRole("region", { name: "All appointments, newest first" });
  const forMira = (await within(history).findByText("Mira Das")).closest("tr");
  expect(within(forMira).getByText("Missed")).toBeInTheDocument(); // last week, never completed
  expect(within(forMira).getByText("—")).toBeInTheDocument(); // no slot any more
  expect(within(history).getByText("Completed")).toBeInTheDocument();
  expect(within(history).getByText("Page 1 of 2 · 12 appointments")).toBeInTheDocument();

  fireEvent.click(within(history).getByRole("button", { name: "Older" }));
  await waitFor(() =>
    expect(getFrontDeskAppointmentPage).toHaveBeenLastCalledWith({ patientId: "7", order: "desc", page: 1, size: 10 })
  );

  expect(screen.getByRole("link", { name: "Book an appointment" })).toHaveAttribute("href", "/walk-in?patientId=7");
  expect(screen.getByRole("link", { name: "← Patients" })).toHaveAttribute("href", "/patients");
});

test("in the admin portal the links stay there; back goes to the list it came from", async () => {
  render(
    <MemoryRouter initialEntries={[{ pathname: "/admin/patients/7", state: { from: "/admin/patients?q=das&page=1" } }]}>
      <Routes>
        <Route path="/admin/patients/:patientId" element={<FrontDeskPatient />} />
      </Routes>
    </MemoryRouter>
  );

  expect(await screen.findByRole("link", { name: "Book an appointment" })).toHaveAttribute(
    "href",
    "/admin/walk-in?patientId=7"
  );
  expect(screen.getByRole("link", { name: "← Patients" })).toHaveAttribute("href", "/admin/patients?q=das&page=1");
});

test("an unknown patient: the server's message", async () => {
  patientsApi.getFrontDeskPatient.mockRejectedValue({
    response: { status: 404, data: { message: "Patient not found with id : '9'" } },
  });
  renderAt("/patients/9");

  expect(await screen.findByRole("alert")).toHaveTextContent("Patient not found");
  expect(screen.getByRole("link", { name: "← Patients" })).toHaveAttribute("href", "/patients");
});
