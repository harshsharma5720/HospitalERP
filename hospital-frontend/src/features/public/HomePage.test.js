import { render, screen, within, act } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import HomePage from "./HomePage";
import { getAllDoctors } from "../doctors/api";
import useAuthStore from "../auth/useAuthStore";

jest.mock("../doctors/api");

const DOCTORS = [
  { id: 1, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
  { id: 2, name: "Sunil Iyer", specialist: "ORTHOPEDICS", active: true },
  { id: 3, name: "Meera Nair", specialist: "CARDIOLOGY", active: true, profileImage: "/uploads/meera.jpg" },
  { id: 4, name: "Vikram Shah", specialist: "NEUROLOGY", active: false },
];

const renderHome = () =>
  render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>
  );

beforeEach(() => {
  useAuthStore.setState({ token: null, role: null, userId: null });
  getAllDoctors.mockResolvedValue({ data: DOCTORS });
});
afterEach(() => {
  jest.resetAllMocks();
  jest.useRealTimers();
  sessionStorage.clear();
  localStorage.clear();
});

test("the hero says what the hospital offers and leads to booking and the doctors", async () => {
  renderHome();
  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Specialist care, booked in a minute.");
  const book = screen.getAllByRole("link", { name: /Book an appointment/ });
  expect(book.every((link) => link.getAttribute("href") === "/appointments")).toBe(true);
  expect(screen.getByRole("link", { name: "Find a doctor" })).toHaveAttribute("href", "/doctors");
  expect(screen.getAllByRole("link", { name: /\+91 92895 20303/ })[0]).toHaveAttribute("href", "tel:+919289520303");
  await screen.findByText("Dr. Anita Rao");
});

test("the numbers come from the doctors' directory, deactivated doctors left out", async () => {
  renderHome();
  const facts = await screen.findByRole("region", { name: "At a glance" });
  expect(await within(facts).findByText("3")).toBeInTheDocument();
  expect(within(facts).getByText("doctors")).toBeInTheDocument();
  expect(within(facts).getByText("2")).toBeInTheDocument();
  expect(within(facts).getByText("specialities")).toBeInTheDocument();
  // Specialities with their doctors
  expect(screen.getByRole("link", { name: /Cardiology.*2 doctors/ })).toHaveAttribute("href", "/doctors?speciality=CARDIOLOGY");
  expect(screen.getByRole("link", { name: /Orthopedics.*1 doctor$/ })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Neurology.*See doctors/ })).toBeInTheDocument();
});

test("some doctors are shown with a link to their profile", async () => {
  renderHome();
  const doctors = await screen.findByRole("region", { name: "Meet some of our doctors" });
  expect(within(doctors).getByRole("link", { name: "View profile of Dr. Anita Rao" })).toHaveAttribute("href", "/doctor-profile/1");
  expect(within(doctors).queryByText("Dr. Vikram Shah")).not.toBeInTheDocument();
  expect(doctors.querySelector("img")).toHaveAttribute("src", expect.stringMatching(/\/uploads\/meera\.jpg$/));
});

test("without the directory the page still works: no made-up numbers, no doctors section", async () => {
  getAllDoctors.mockRejectedValue(new Error("offline"));
  renderHome();
  const facts = screen.getByRole("region", { name: "At a glance" });
  await act(async () => {});
  expect(within(facts).queryByText("doctors")).not.toBeInTheDocument();
  expect(within(facts).getByText("24×7")).toBeInTheDocument();
  expect(screen.queryByRole("region", { name: "Meet some of our doctors" })).not.toBeInTheDocument();
  expect(screen.getByRole("region", { name: /Shreya Hospital, Sahibabad/ })).toBeInTheDocument();
});

test("visitors get the welcome after a moment, once per visit", async () => {
  jest.useFakeTimers();
  const { unmount } = renderHome();
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  await act(async () => jest.advanceTimersByTime(2000));
  const welcome = screen.getByRole("dialog", { name: "Welcome to Shreya Hospital" });
  expect(within(welcome).getByRole("link", { name: "Log in" })).toHaveAttribute("href", "/login");
  expect(within(welcome).getByRole("link", { name: "Create an account" })).toHaveAttribute("href", "/register");
  await act(async () => within(welcome).getByRole("button", { name: "Close" }).click());
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  unmount();

  renderHome(); // back on the home page in the same visit
  await act(async () => jest.advanceTimersByTime(5000));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

test("logged-in users get no welcome", async () => {
  jest.useFakeTimers();
  useAuthStore.setState({ token: "x.e30.y", role: "ROLE_PATIENT", userId: 1 });
  renderHome();
  await act(async () => jest.advanceTimersByTime(5000));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});
