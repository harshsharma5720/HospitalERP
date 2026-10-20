import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import FrontDeskPatient from "./FrontDeskPatient";
import * as patientsApi from "./api";
import { getFrontDeskAppointmentPage } from "../appointments/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { ToastProvider } from "../../shared/ui";

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
    <ToastProvider>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/patients/:patientId" element={<FrontDeskPatient />} />
          <Route path="/admin/patients/:patientId" element={<FrontDeskPatient />} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

const KABIR = { id: 31, name: "Kabir Das", relationship: "SON", gender: "MALE", dob: "2016-02-01", abhaNumber: "91000000000012" };
const MIRA = { id: 32, name: "Mira Das", relationship: "DAUGHTER", gender: "FEMALE", dob: null, abhaNumber: null };

beforeEach(() => {
  patientsApi.getFrontDeskPatient.mockResolvedValue({ data: ARJUN });
  patientsApi.getPatientRelatives.mockResolvedValue({ data: [KABIR, MIRA] });
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

// ---- ABHA numbers (docs/ABHA_NUMBER_PLAN.md)

const abhaOf = (region) => within(region).getByText("ABHA").nextSibling;

test("the patient's ABHA number: added, checked, refused when on another record, then removed", async () => {
  renderAt("/patients/7");
  const details = await screen.findByRole("region", { name: "Details" });
  expect(abhaOf(details)).toHaveTextContent("—");

  fireEvent.click(within(details).getByRole("button", { name: "Add Arjun Das's ABHA number" }));
  const input = within(details).getByLabelText("Arjun Das's ABHA number");
  expect(input).toHaveValue("");
  expect(input).toHaveAccessibleDescription("14 digits. Empty removes it.");
  fireEvent.change(input, { target: { value: "12-3456" } });
  fireEvent.click(within(details).getByRole("button", { name: "Save" }));
  expect(await within(details).findByRole("alert")).toHaveTextContent("An ABHA number has 14 digits");
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(patientsApi.setPatientAbha).not.toHaveBeenCalled();

  patientsApi.setPatientAbha.mockRejectedValueOnce({
    response: { status: 409, data: { message: "This ABHA number is already on another patient's record" } },
  });
  fireEvent.change(input, { target: { value: " 12 3456 7890 1234 " } });
  fireEvent.click(within(details).getByRole("button", { name: "Save" }));
  expect(await within(details).findByRole("alert")).toHaveTextContent("already on another patient's record");
  expect(patientsApi.setPatientAbha).toHaveBeenLastCalledWith("7", "12 3456 7890 1234");

  patientsApi.setPatientAbha.mockResolvedValueOnce({ data: { ...ARJUN, abhaNumber: "12345678901235" } });
  fireEvent.change(input, { target: { value: "12345678901235" } });
  fireEvent.click(within(details).getByRole("button", { name: "Save" }));
  expect(await screen.findByRole("status")).toHaveTextContent("ABHA number saved.");
  expect(abhaOf(details)).toHaveTextContent("12-3456-7890-1235");

  fireEvent.click(within(details).getByRole("button", { name: "Change Arjun Das's ABHA number" }));
  expect(within(details).getByLabelText("Arjun Das's ABHA number")).toHaveValue("12-3456-7890-1235");
  patientsApi.setPatientAbha.mockResolvedValueOnce({ data: { ...ARJUN, abhaNumber: null } });
  fireEvent.change(within(details).getByLabelText("Arjun Das's ABHA number"), { target: { value: "" } });
  fireEvent.click(within(details).getByRole("button", { name: "Save" }));
  expect(await screen.findByText("ABHA number removed.")).toBeInTheDocument();
  expect(patientsApi.setPatientAbha).toHaveBeenLastCalledWith("7", "");
  expect(abhaOf(details)).toHaveTextContent("—");
});

test("the family members, each with their own ABHA number, changed in place", async () => {
  renderAt("/patients/7");
  const family = await screen.findByRole("region", { name: "Family members" });
  expect(patientsApi.getPatientRelatives).toHaveBeenCalledWith("7");
  const kabir = (await within(family).findByText("Kabir Das")).closest("li");
  expect(kabir).toHaveTextContent(`Son · ${calculateAgeFromDOB("2016-02-01")} years · Male`);
  expect(kabir).toHaveTextContent("91-0000-0000-0012");
  const mira = within(family).getByText("Mira Das").closest("li");
  expect(mira).toHaveTextContent("Daughter · Female");

  // Cancel leaves it as it was
  fireEvent.click(within(mira).getByRole("button", { name: "Add Mira Das's ABHA number" }));
  fireEvent.click(within(mira).getByRole("button", { name: "Cancel" }));
  expect(within(mira).queryByLabelText("Mira Das's ABHA number")).not.toBeInTheDocument();

  patientsApi.setRelativeAbha.mockResolvedValueOnce({ data: { ...MIRA, abhaNumber: "91000000000013" } });
  fireEvent.click(within(mira).getByRole("button", { name: "Add Mira Das's ABHA number" }));
  fireEvent.change(within(mira).getByLabelText("Mira Das's ABHA number"), { target: { value: "91-0000-0000-0013" } });
  fireEvent.click(within(mira).getByRole("button", { name: "Save" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Mira Das's ABHA number saved.");
  expect(patientsApi.setRelativeAbha).toHaveBeenCalledWith("7", 32, "91-0000-0000-0013");
  expect(mira).toHaveTextContent("91-0000-0000-0013");
  expect(kabir).toHaveTextContent("91-0000-0000-0012");
});

test("no family members, or a list that couldn't be loaded, says so", async () => {
  patientsApi.getPatientRelatives.mockResolvedValueOnce({ data: [] });
  const { unmount } = renderAt("/patients/7");
  expect(await screen.findByText("No family members.")).toBeInTheDocument();
  unmount();

  patientsApi.getPatientRelatives.mockRejectedValueOnce({ response: { status: 500 } });
  renderAt("/patients/7");
  expect(await screen.findByText("The family members couldn't be loaded.")).toBeInTheDocument();
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

test("opened from the appointment list, back goes there", async () => {
  render(
    <MemoryRouter
      initialEntries={[{ pathname: "/patients/7", state: { from: "/receptionist-appointments?from=&to=", fromLabel: "Appointments" } }]}
    >
      <Routes>
        <Route path="/patients/:patientId" element={<FrontDeskPatient />} />
      </Routes>
    </MemoryRouter>
  );

  expect(await screen.findByRole("link", { name: "← Appointments" })).toHaveAttribute(
    "href",
    "/receptionist-appointments?from=&to="
  );
});

test("an unknown patient: the server's message", async () => {
  patientsApi.getFrontDeskPatient.mockRejectedValue({
    response: { status: 404, data: { message: "Patient not found with id : '9'" } },
  });
  renderAt("/patients/9");

  expect(await screen.findByRole("alert")).toHaveTextContent("Patient not found");
  expect(screen.getByRole("link", { name: "← Patients" })).toHaveAttribute("href", "/patients");
});
