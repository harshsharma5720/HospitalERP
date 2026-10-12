import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import DoctorDashboard from "./DoctorDashboard";
import useAuthStore from "../../auth/useAuthStore";
import { getDoctor } from "../api";
import * as appointmentsApi from "../../appointments/api";

jest.mock("../api");
jest.mock("../../appointments/api");

const list = (n) => ({ data: Array.from({ length: n }, (_, i) => ({ appointmentID: i })) });

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_DOCTOR", userId: 5 });
  getDoctor.mockResolvedValue({ data: { name: "Anita Rao", specialist: "CARDIOLOGY" } });
  appointmentsApi.getDoctorAppointments.mockResolvedValue(list(12));
  appointmentsApi.getDoctorCompletedAppointments.mockResolvedValue(list(6));
  appointmentsApi.getDoctorPendingAppointments.mockResolvedValue(list(2));
});
afterEach(() => jest.resetAllMocks());

const renderDashboard = () =>
  render(
    <MemoryRouter>
      <DoctorDashboard />
    </MemoryRouter>
  );

test("greets the doctor and shows their appointments in numbers", async () => {
  renderDashboard();
  expect(await screen.findByRole("heading", { name: "Hello, Dr. Anita Rao" })).toBeInTheDocument();
  expect(screen.getByText("Cardiology")).toBeInTheDocument();
  expect(appointmentsApi.getDoctorPendingAppointments).toHaveBeenCalledWith(5);
  expect(screen.getByText("Total appointments").nextSibling).toHaveTextContent("12");
  expect(screen.getByText("6 of 8 visits assigned to you")).toBeInTheDocument();
  expect(screen.getByRole("img", { name: "75% of your visits are completed" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Today's queue" })).toHaveAttribute("href", "/doctor/queue");
  expect(screen.getByRole("navigation", { name: "Quick links" })).toHaveTextContent("Ask for days off");
});

test("a list that can't be loaded counts as empty; nothing assigned is 0%", async () => {
  appointmentsApi.getDoctorAppointments.mockRejectedValue(new Error("offline"));
  appointmentsApi.getDoctorCompletedAppointments.mockResolvedValue(list(0));
  appointmentsApi.getDoctorPendingAppointments.mockResolvedValue(list(0));
  renderDashboard();
  expect(await screen.findByRole("img", { name: "0% of your visits are completed" })).toBeInTheDocument();
  expect(screen.getByText("Total appointments").nextSibling).toHaveTextContent("0");
});
