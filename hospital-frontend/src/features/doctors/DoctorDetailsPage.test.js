import { render, screen, fireEvent, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import DoctorDetailsPage from "./DoctorDetailsPage";
import { getAllDoctors, getPublicDoctor } from "./api";
import useAuthStore from "../auth/useAuthStore";

jest.mock("./api");

const RAO = { id: 1, name: "Anita Rao", specialist: "CARDIOLOGY", email: "rao@example.com", phoneNumber: "+91 98765 00011", active: true };

const renderProfile = (id = 1) =>
  render(
    <MemoryRouter initialEntries={[`/doctor-profile/${id}`]}>
      <Routes>
        <Route path="/doctor-profile/:doctorId" element={<DoctorDetailsPage />} />
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => {
  useAuthStore.setState({ token: null, role: null, userId: null });
  getPublicDoctor.mockResolvedValue({ data: RAO });
  getAllDoctors.mockResolvedValue({
    data: [RAO, { id: 3, name: "Meera Nair", specialist: "CARDIOLOGY", active: true }, { id: 2, name: "Sunil Iyer", specialist: "ORTHOPEDICS", active: true }],
  });
});
afterEach(() => jest.resetAllMocks());

test("shows what the directory knows - and nothing made up", async () => {
  renderProfile();
  expect(await screen.findByRole("heading", { level: 1, name: "Dr. Anita Rao" })).toBeInTheDocument();
  expect(getPublicDoctor).toHaveBeenCalledWith("1");
  expect(screen.getByRole("link", { name: "rao@example.com" })).toHaveAttribute("href", "mailto:rao@example.com");
  expect(screen.getByRole("link", { name: "+91 98765 00011" })).toHaveAttribute("href", "tel:+919876500011");
  expect(screen.getByRole("region", { name: "Cardiology at Shreya Hospital" })).toHaveTextContent("High blood pressure");
  expect(screen.queryByText(/★|Chat with Doctor|years of experience|English, Hindi/)).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "← All doctors" })).toHaveAttribute("href", "/doctors?speciality=CARDIOLOGY");
});

test("other doctors in the same speciality are suggested", async () => {
  renderProfile();
  const others = await screen.findByRole("region", { name: "Other doctors in Cardiology" });
  expect(within(others).getByText("Dr. Meera Nair")).toBeInTheDocument();
  expect(within(others).queryByText("Dr. Anita Rao")).not.toBeInTheDocument();
  expect(within(others).queryByText("Dr. Sunil Iyer")).not.toBeInTheDocument();
});

test("a visitor who books is asked to log in", async () => {
  renderProfile();
  fireEvent.click(await screen.findByRole("button", { name: "Book an appointment" }));
  expect(screen.getByRole("dialog", { name: "Welcome to Shreya Hospital" })).toBeInTheDocument();
});

test("a doctor who left can't be booked", async () => {
  getPublicDoctor.mockResolvedValue({ data: { ...RAO, active: false } });
  renderProfile();
  expect(await screen.findByText("Dr. Anita Rao no longer takes appointments here.")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Book an appointment" })).not.toBeInTheDocument();
});

test("a profile that can't be loaded can be tried again", async () => {
  getPublicDoctor.mockRejectedValueOnce(new Error("offline"));
  renderProfile();
  expect(await screen.findByText("We couldn't load this doctor")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByRole("heading", { level: 1, name: "Dr. Anita Rao" })).toBeInTheDocument();
});
