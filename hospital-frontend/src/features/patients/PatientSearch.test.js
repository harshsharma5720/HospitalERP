import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import PatientSearch from "./PatientSearch";
import * as patientsApi from "./api";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";

jest.mock("./api");

const ASHA = {
  patientId: 41, patientName: "Asha Rao", gender: "FEMALE", dob: "1990-01-15", contactNo: "+919000011111",
  email: "asha@example.com", hasLogin: true,
};
const KIRAN = {
  patientId: 42, patientName: "Kiran Rao", gender: "MALE", dob: null, contactNo: "9000011111", email: null,
  hasLogin: false,
};

const pageOf = (entries, { page = 0, totalPages = 1, totalEntries = entries.length } = {}) => ({
  data: { entries, page, size: 20, totalEntries, totalPages },
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
        {["/patients", "/admin/patients"].map((path) => (
          <Route key={path} path={path} element={<><PatientSearch /><Address /></>} />
        ))}
      </Routes>
    </MemoryRouter>
  );

const search = (text) => {
  fireEvent.change(screen.getByRole("searchbox"), { target: { value: text } });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
};

beforeEach(() => patientsApi.searchPatients.mockResolvedValue(pageOf([ASHA, KIRAN])));
afterEach(() => jest.resetAllMocks());

test("lists everyone at first, with what the front desk needs to tell them apart", async () => {
  renderAt("/patients");

  const asha = (await screen.findByRole("link", { name: "Asha Rao" })).closest("tr");
  expect(patientsApi.searchPatients).toHaveBeenCalledWith("", 0, 20);
  for (const text of ["#41", "Female", String(calculateAgeFromDOB("1990-01-15")), "+919000011111", "asha@example.com", "App account"]) {
    expect(within(asha).getByText(text)).toBeInTheDocument();
  }
  expect(screen.getByRole("link", { name: "Asha Rao" })).toHaveAttribute("href", "/patients/41");
  expect(screen.getByRole("link", { name: "Kiran Rao" }).closest("tr")).toHaveTextContent("Front-desk record");
  expect(screen.getByText("Page 1 of 1 · 2 patients")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Show everyone" })).not.toBeInTheDocument();
});

test("a search is kept in the address and paged", async () => {
  patientsApi.searchPatients.mockResolvedValue(pageOf([ASHA], { totalPages: 3, totalEntries: 41 }));
  renderAt("/patients");
  await screen.findByRole("link", { name: "Asha Rao" });

  search("  rao ");
  await waitFor(() => expect(patientsApi.searchPatients).toHaveBeenLastCalledWith("rao", 0, 20));
  expect(screen.getByTestId("address")).toHaveTextContent("/patients?q=rao");

  await waitFor(() => expect(screen.getByRole("button", { name: "Next" })).toBeEnabled());
  fireEvent.click(screen.getByRole("button", { name: "Next" }));
  await waitFor(() => expect(patientsApi.searchPatients).toHaveBeenLastCalledWith("rao", 1, 20));
  expect(screen.getByTestId("address")).toHaveTextContent("/patients?q=rao&page=1");
  expect(await screen.findByText("Page 2 of 3 · 41 patients")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Show everyone" }));
  await waitFor(() => expect(patientsApi.searchPatients).toHaveBeenLastCalledWith("", 0, 20));
  expect(screen.getByTestId("address")).toHaveTextContent(/^\/patients$/);
});

test("coming back with a search in the address shows it again; in the admin portal links stay there", async () => {
  renderAt("/admin/patients?q=asha&page=2");

  expect(await screen.findByRole("link", { name: "Asha Rao" })).toHaveAttribute("href", "/admin/patients/41");
  expect(patientsApi.searchPatients).toHaveBeenCalledWith("asha", 2, 20);
  expect(screen.getByRole("searchbox")).toHaveValue("asha");
});

test("no match, and the server's message for a search it can't do", async () => {
  patientsApi.searchPatients.mockResolvedValueOnce(pageOf([]));
  renderAt("/patients?q=zzz");
  expect(await screen.findByText('No patient found for "zzz".')).toBeInTheDocument();

  patientsApi.searchPatients.mockRejectedValueOnce({
    response: { status: 400, data: { message: "Please enter at least 10 digits of the phone number" } },
  });
  search("(900) 001");
  expect(await screen.findByRole("alert")).toHaveTextContent("Please enter at least 10 digits of the phone number");
});
