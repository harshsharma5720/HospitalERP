import { render, screen, fireEvent, within, act } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import DoctorPage from "./DoctorPage";
import { getAllDoctors } from "./api";
import useAuthStore from "../auth/useAuthStore";

jest.mock("./api");

const DOCTORS = [
  { id: 1, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
  { id: 2, name: "Sunil Iyer", specialist: "ORTHOPEDICS", active: true },
  { id: 3, name: "Meera Nair", specialist: "CARDIOLOGY", active: true },
  { id: 4, name: "Vikram Shah", specialist: "NEUROLOGY", active: false },
];

function Where() {
  const location = useLocation();
  return (
    <div data-testid="where">
      {location.pathname + location.search}|{location.state?.doctorName ?? ""}
    </div>
  );
}

const renderAt = (path = "/doctors") =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path="/doctors"
          element={
            <>
              <DoctorPage />
              <Where />
            </>
          }
        />
        <Route path="/appointments" element={<Where />} />
      </Routes>
    </MemoryRouter>
  );

const names = () => screen.getAllByRole("heading", { level: 3 }).map((h) => h.textContent);

beforeEach(() => {
  useAuthStore.setState({ token: null, role: null, userId: null });
  getAllDoctors.mockResolvedValue({ data: DOCTORS });
});
afterEach(() => jest.resetAllMocks());

test("lists the active doctors with their profile links", async () => {
  renderAt();
  await screen.findByText("Dr. Anita Rao");
  expect(names()).toEqual(["Dr. Anita Rao", "Dr. Sunil Iyer", "Dr. Meera Nair"]);
  expect(screen.getByRole("link", { name: "View profile of Dr. Sunil Iyer" })).toHaveAttribute("href", "/doctor-profile/2");
});

test("the speciality filter narrows the list and lives in the address", async () => {
  renderAt();
  await screen.findByText("Dr. Anita Rao");
  const specialities = screen.getByRole("group", { name: "Speciality" });
  fireEvent.click(within(specialities).getByRole("button", { name: "Cardiology · 2" }));
  expect(names()).toEqual(["Dr. Anita Rao", "Dr. Meera Nair"]);
  expect(screen.getByTestId("where")).toHaveTextContent("/doctors?speciality=CARDIOLOGY");
  expect(within(specialities).getByRole("button", { name: "Cardiology · 2" })).toHaveAttribute("aria-pressed", "true");
});

test("opened from a department, the list is already filtered; name search narrows it further", async () => {
  renderAt("/doctors?speciality=CARDIOLOGY");
  await screen.findByText("Dr. Meera Nair");
  expect(names()).toEqual(["Dr. Anita Rao", "Dr. Meera Nair"]);
  fireEvent.change(screen.getByLabelText("Search doctors by name"), { target: { value: "MEERA" } });
  expect(names()).toEqual(["Dr. Meera Nair"]);
  fireEvent.change(screen.getByLabelText("Search doctors by name"), { target: { value: "zzz" } });
  expect(screen.getByText("No doctors match")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Show all doctors" }));
  expect(names()).toHaveLength(3);
  expect(screen.getByTestId("where")).toHaveTextContent(/^\/doctors\|/);
});

test("a visitor who books is asked to log in; a logged-in patient goes to the booking page", async () => {
  renderAt();
  fireEvent.click(await screen.findByRole("button", { name: "Book with Dr. Anita Rao" }));
  expect(screen.getByRole("dialog", { name: "Welcome to Shreya Hospital" })).toHaveTextContent("Log in to book this appointment.");
  fireEvent.click(screen.getByRole("button", { name: "Close" }));

  act(() => useAuthStore.setState({ token: "t", role: "ROLE_PATIENT", userId: 9 }));
  fireEvent.click(screen.getByRole("button", { name: "Book with Dr. Sunil Iyer" }));
  expect(screen.getByTestId("where")).toHaveTextContent("/appointments|Sunil Iyer");
});

test("when the directory can't be reached it says so and can try again", async () => {
  getAllDoctors.mockRejectedValueOnce(new Error("offline"));
  renderAt();
  expect(await screen.findByText("We couldn't load the doctors")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("Dr. Anita Rao")).toBeInTheDocument();
  expect(getAllDoctors).toHaveBeenCalledTimes(2);
});
