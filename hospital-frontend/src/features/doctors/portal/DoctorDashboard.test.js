import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import DoctorDashboard from "./DoctorDashboard";
import useAuthStore from "../../auth/useAuthStore";
import { getDoctor } from "../api";
import * as appointmentsApi from "../../appointments/api";

jest.mock("../api");
jest.mock("../../appointments/api");
// The analytics are tested in DoctorAnalytics.test.js
jest.mock("../analytics/DoctorAnalytics", () => ({ __esModule: true, default: (props) => <div data-testid="analytics" data-doctor={props.doctorId ?? "me"} /> }));

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_DOCTOR", userId: 5 });
  getDoctor.mockResolvedValue({ data: { name: "Anita Rao", specialist: "CARDIOLOGY" } });
});
afterEach(() => jest.resetAllMocks());

const renderDashboard = () =>
  render(
    <MemoryRouter>
      <DoctorDashboard />
    </MemoryRouter>
  );

test("greets the doctor, links to the day's work and shows their own analytics", async () => {
  renderDashboard();
  expect(await screen.findByRole("heading", { name: "Hello, Dr. Anita Rao" })).toBeInTheDocument();
  expect(getDoctor).toHaveBeenCalledWith(5);
  expect(screen.getByText("Cardiology")).toBeInTheDocument();
  const links = screen.getByRole("navigation", { name: "Quick links" });
  expect(links).toHaveTextContent("Ask for days off");
  expect(screen.getAllByRole("link", { name: /Today's queue/ })[0]).toHaveAttribute("href", "/doctor/queue");
  expect(screen.getByTestId("analytics")).toHaveAttribute("data-doctor", "me");
  // The figures come from the analytics now: the full appointment lists aren't downloaded
  expect(appointmentsApi.getDoctorAppointments).not.toHaveBeenCalled();
  expect(appointmentsApi.getDoctorCompletedAppointments).not.toHaveBeenCalled();
});

test("a doctor whose profile can't be loaded is still greeted", async () => {
  getDoctor.mockRejectedValue(new Error("offline"));
  renderDashboard();
  expect(await screen.findByRole("heading", { name: "Hello" })).toBeInTheDocument();
  expect(screen.getByTestId("analytics")).toBeInTheDocument();
});
