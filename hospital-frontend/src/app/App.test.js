import { render, screen, waitFor } from "@testing-library/react";
import App from "./App";
import { getMyAppointments, getQueueScreen } from "../features/appointments/api";
import { getPatientAccount, getRelatives } from "../features/patients/api";
import { getDoctor } from "../features/doctors/api";
import { getReceptionist } from "../shared/profileApi";
import useAuthStore from "../features/auth/useAuthStore";

jest.mock("../features/appointments/api");
jest.mock("../features/patients/api");
jest.mock("../features/doctors/api");
jest.mock("../shared/profileApi", () => ({ ...jest.requireActual("../shared/profileApi"), getReceptionist: jest.fn() }));

afterEach(() => {
  window.history.pushState({}, "", "/");
  useAuthStore.setState({ token: null, role: null, userId: null });
  jest.resetAllMocks();
});

const signIn = (role) => useAuthStore.setState({ token: `x.${btoa(JSON.stringify({ sub: "u", role }))}.y`, role, userId: 7 });

test("the waiting-room screen opens without a login, without the site's footer", async () => {
  localStorage.clear();
  getQueueScreen.mockResolvedValue({ data: { date: "2026-10-11", doctors: [] } });
  window.history.pushState({}, "", "/queue-screen");

  render(<App />);

  expect(await screen.findByText("No patients in the queue right now.")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Now serving" })).toBeInTheDocument();
  expect(screen.queryByRole("contentinfo")).not.toBeInTheDocument(); // no site footer on the TV
});

test("a patient's /relatives and /edit-profile lead to their account", async () => {
  signIn("ROLE_PATIENT");
  getPatientAccount.mockResolvedValue({ data: { patientId: 41, patientName: "Asha Verma", userName: "asha" } });
  getRelatives.mockResolvedValue({ data: [] });
  getMyAppointments.mockResolvedValue({ data: [] });
  window.history.pushState({}, "", "/relatives");

  render(<App />);

  expect(await screen.findByRole("heading", { level: 1, name: "Hello, Asha" })).toBeInTheDocument();
  expect(window.location.pathname).toBe("/edit-profile");
});

test("staff opening /edit-profile go to their profile in their portal", async () => {
  signIn("ROLE_DOCTOR");
  getDoctor.mockResolvedValue({ data: { id: 9, name: "Anita Rao", specialist: "CARDIOLOGY" } });
  window.history.pushState({}, "", "/edit-profile");
  const { unmount } = render(<App />);
  await waitFor(() => expect(window.location.pathname).toBe("/doctor/profile"));
  unmount();

  signIn("ROLE_RECEPTIONIST");
  getReceptionist.mockResolvedValue({ data: { name: "Ravi Desk", userName: "desk1" } });
  window.history.pushState({}, "", "/edit-profile");
  render(<App />);
  await waitFor(() => expect(window.location.pathname).toBe("/my-profile"));
  expect(await screen.findByRole("heading", { level: 1, name: "My profile" })).toBeInTheDocument();
});
