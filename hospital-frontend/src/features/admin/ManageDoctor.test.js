import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ManageDoctor from "./ManageDoctor";
import * as adminApi from "./api";
import { getAllDoctors } from "../doctors/api";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api");
jest.mock("../doctors/api");

const TODAY = toLocalISODate();
const DOCTORS = [
  { id: 1, userId: 11, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
  { id: 2, userId: 12, name: "Sunil Iyer", specialist: "ORTHOPEDICS", active: true },
  { id: 3, userId: 13, name: "Vikram Shah", specialist: "NEUROLOGY", active: false },
];
const COUNTS = { 1: { completed: 4, pending: 2 }, 2: { completed: 9, pending: 1 }, 3: { completed: 1, pending: 0 } };

const rowOf = (name) => screen.getByText(`Dr. ${name}`).closest("tr");

beforeEach(() => {
  getAllDoctors.mockResolvedValue({ data: DOCTORS });
  adminApi.getDoctorAppointmentCount.mockImplementation((id) => Promise.resolve({ data: COUNTS[id] }));
  adminApi.getApprovedLeaves.mockResolvedValue({ data: [{ userId: 11, startDate: TODAY, endDate: TODAY }] });
  adminApi.deactivateUser.mockResolvedValue({ data: {} });
});
afterEach(() => jest.resetAllMocks());

const renderPage = async () => {
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter>
          <ManageDoctor />
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );
  await screen.findByText("Dr. Anita Rao");
};

test("ranks the doctors by completed appointments and marks who is on leave today", async () => {
  await renderPage();
  const names = screen.getAllByRole("row").slice(1).map((row) => within(row).getByText(/^Dr\./).textContent);
  expect(names).toEqual(["Dr. Sunil Iyer", "Dr. Anita Rao", "Dr. Vikram Shah"]);
  expect(rowOf("Anita Rao")).toHaveTextContent("On leave today");
  expect(rowOf("Vikram Shah")).toHaveTextContent("Deactivated");
  expect(within(rowOf("Sunil Iyer")).getByRole("link", { name: "Appointments of Dr. Sunil Iyer" })).toHaveAttribute(
    "href",
    "/admin/doctor/12/appointments"
  );
  // Analytics by the doctor's id (docs/DOCTOR_ANALYTICS_PLAN.md), also for a deactivated doctor's past figures
  expect(within(rowOf("Sunil Iyer")).getByRole("link", { name: "Analytics of Dr. Sunil Iyer" })).toHaveAttribute(
    "href",
    "/admin/doctor/2/analytics"
  );
  expect(within(rowOf("Vikram Shah")).getByRole("link", { name: "Analytics of Dr. Vikram Shah" })).toBeInTheDocument();
});

test("today's filter shows the doctors working or on leave; search narrows the list", async () => {
  await renderPage();
  fireEvent.click(screen.getByRole("button", { name: "On leave today (1)" }));
  expect(screen.getByText("Dr. Anita Rao")).toBeInTheDocument();
  expect(screen.queryByText("Dr. Sunil Iyer")).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Working today (1)" }));
  expect(screen.getByText("Dr. Sunil Iyer")).toBeInTheDocument();
  expect(screen.queryByText("Dr. Vikram Shah")).not.toBeInTheDocument(); // deactivated: neither

  fireEvent.click(screen.getByRole("button", { name: "All (3)" }));
  fireEvent.change(screen.getByLabelText("Search doctors"), { target: { value: "neuro" } });
  expect(screen.getByText("Dr. Vikram Shah")).toBeInTheDocument();
  expect(screen.queryByText("Dr. Anita Rao")).not.toBeInTheDocument();
});

test("deactivating asks first, then deactivates the doctor's account and says so", async () => {
  await renderPage();
  fireEvent.click(screen.getByRole("button", { name: "Deactivate Dr. Sunil Iyer" }));
  const dialog = screen.getByRole("dialog", { name: "Deactivate Dr. Sunil Iyer?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Deactivate" }));
  await waitFor(() => expect(adminApi.deactivateUser).toHaveBeenCalledWith(12));
  expect(await screen.findByText("Dr. Sunil Iyer deactivated.")).toBeInTheDocument();
});
