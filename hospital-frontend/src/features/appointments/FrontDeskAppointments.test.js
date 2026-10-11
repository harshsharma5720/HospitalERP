import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import FrontDeskAppointments from "./FrontDeskAppointments";
import { getFrontDeskAppointmentPage } from "./api";
import { getAllDoctors } from "../doctors/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";

jest.mock("./api");
jest.mock("../doctors/api");

const TODAY = toLocalISODate();
const YESTERDAY = toLocalISODate(addDays(new Date(), -1));

const DOCTORS = [
  { id: 3, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
  { id: 4, name: "Vikram Shah", specialist: "NEUROLOGY", active: false },
];

const visit = (appointmentID, status, extra = {}) => ({
  appointmentID, status, date: TODAY, startTime: "10:00:00", endTime: "10:30:00", doctorName: "Anita Rao",
  patientName: "Asha Rao", ptInfoId: 41, message: "Fever", ...extra,
});
const pageOf = (entries, { totalPages = 1, totalEntries = entries.length } = {}) => ({
  data: { entries, page: 0, size: 20, totalEntries, totalPages },
});

// Shows the address, so the tests can check what the page keeps in it
function Address() {
  const location = useLocation();
  return <output data-testid="address">{location.pathname + location.search}</output>;
}

const renderAt = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        {["/receptionist-appointments", "/admin/appointments"].map((path) => (
          <Route key={path} path={path} element={<><FrontDeskAppointments /><Address /></>} />
        ))}
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => {
  getAllDoctors.mockResolvedValue({ data: DOCTORS });
  getFrontDeskAppointmentPage.mockResolvedValue(
    pageOf([
      visit(1, "SCHEDULED"),
      visit(2, "COMPLETED", { patientName: "Kiran Rao", ptInfoId: 42, message: null, startTime: "11:00:00", endTime: "11:30:00" }),
    ])
  );
});
afterEach(() => jest.resetAllMocks());

test("opens on today's appointments, each linked to its patient", async () => {
  renderAt("/receptionist-appointments");

  const asha = (await screen.findByRole("link", { name: "Asha Rao" })).closest("tr");
  expect(getFrontDeskAppointmentPage).toHaveBeenCalledWith({ from: TODAY, to: TODAY, page: 0, size: 20 });
  for (const text of ["Today", "10:00–10:30", "Dr. Anita Rao", "Scheduled", "Fever"]) {
    expect(within(asha).getByText(text)).toBeInTheDocument();
  }
  expect(screen.getByRole("link", { name: "Asha Rao" })).toHaveAttribute("href", "/patients/41");
  expect(screen.getByRole("link", { name: "Kiran Rao" }).closest("tr")).toHaveTextContent("Completed");
  expect(screen.getByLabelText("From")).toHaveValue(TODAY);
  expect(screen.getByLabelText("To")).toHaveValue(TODAY);
  expect(screen.getByText("Page 1 of 1 · 2 appointments")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Walk-in booking" })).toHaveAttribute("href", "/walk-in");
  // A deactivated doctor stays in the filter, for their past appointments
  expect(await screen.findByRole("option", { name: "Dr. Vikram Shah — Neurology (inactive)" })).toBeInTheDocument();
});

test("the filters go into the address and the request, and the list pages", async () => {
  getFrontDeskAppointmentPage.mockResolvedValue(pageOf([visit(1, "SCHEDULED")], { totalPages: 2, totalEntries: 21 }));
  renderAt("/receptionist-appointments");
  await screen.findByRole("option", { name: "Dr. Anita Rao — Cardiology" });

  fireEvent.change(screen.getByLabelText("From"), { target: { value: YESTERDAY } });
  fireEvent.change(screen.getByLabelText("Doctor"), { target: { value: "3" } });
  fireEvent.change(screen.getByLabelText("Status"), { target: { value: "upcoming" } });
  fireEvent.change(screen.getByLabelText("Patient"), { target: { value: " asha " } });
  fireEvent.click(screen.getByRole("button", { name: "Show" }));

  await waitFor(() =>
    expect(getFrontDeskAppointmentPage).toHaveBeenLastCalledWith({
      from: YESTERDAY, to: TODAY, doctorId: "3", status: "upcoming", patient: "asha", page: 0, size: 20,
    })
  );
  expect(screen.getByTestId("address")).toHaveTextContent(
    `/receptionist-appointments?from=${YESTERDAY}&to=${TODAY}&doctorId=3&status=upcoming&patient=asha`
  );

  await waitFor(() => expect(screen.getByRole("button", { name: "Next" })).toBeEnabled());
  fireEvent.click(screen.getByRole("button", { name: "Next" }));
  await waitFor(() =>
    expect(getFrontDeskAppointmentPage).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, doctorId: "3" }))
  );
  expect(await screen.findByText("Page 2 of 2 · 21 appointments")).toBeInTheDocument();
});

test("any date, back to today, and inside the admin portal", async () => {
  renderAt("/admin/appointments?from=&to=&status=cancelled");

  expect(await screen.findByRole("link", { name: "Asha Rao" })).toHaveAttribute("href", "/admin/patients/41");
  expect(getFrontDeskAppointmentPage).toHaveBeenCalledWith({ status: "cancelled", page: 0, size: 20 }); // no dates
  expect(screen.getByLabelText("From")).toHaveValue("");
  expect(screen.getByLabelText("Status")).toHaveValue("cancelled");
  expect(screen.getByRole("link", { name: "Walk-in booking" })).toHaveAttribute("href", "/admin/walk-in");

  fireEvent.click(screen.getByRole("button", { name: "Today" }));
  await waitFor(() =>
    expect(getFrontDeskAppointmentPage).toHaveBeenLastCalledWith({
      from: TODAY, to: TODAY, status: "cancelled", page: 0, size: 20,
    })
  );
  fireEvent.click(screen.getByRole("button", { name: "Any date" }));
  await waitFor(() =>
    expect(getFrontDeskAppointmentPage).toHaveBeenLastCalledWith({ status: "cancelled", page: 0, size: 20 })
  );
  expect(screen.getByTestId("address")).toHaveTextContent("/admin/appointments?from=&to=&status=cancelled");
});

test("the server's message for wrong filters, and when nothing matches", async () => {
  getFrontDeskAppointmentPage.mockRejectedValueOnce({
    response: { status: 400, data: { message: "The start date must not be after the end date" } },
  });
  renderAt(`/receptionist-appointments?from=${TODAY}&to=${YESTERDAY}`);
  expect(await screen.findByRole("alert")).toHaveTextContent("The start date must not be after the end date");

  getFrontDeskAppointmentPage.mockResolvedValueOnce(pageOf([]));
  fireEvent.click(screen.getByRole("button", { name: "Today" }));
  expect(await screen.findByText("No appointments for these filters.")).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
