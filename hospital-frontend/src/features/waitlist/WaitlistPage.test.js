import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import WaitlistPage from "./WaitlistPage";
import * as waitlistApi from "./api";
import { getAllDoctors } from "../doctors/api";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

// The waitlist api is mocked for every test (setupTests.js)
jest.mock("../doctors/api");

const TODAY = toLocalISODate();

const wait = (id, name, extra = {}) => ({
  id, patientId: 40 + id, relativeId: null, name, patientName: name, phone: "+919800000001", hasLogin: true, doctorId: 3,
  doctorName: "Anita Rao", date: "2026-11-20", shift: "MORNING", status: "WAITING", joinedAt: "2026-11-18T09:15:00",
  joinedBy: "deskmeena", toldAt: null, ...extra,
});

const renderAt = (entry) =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter initialEntries={[entry]}>
          <Routes>
            <Route path="/waitlist" element={<WaitlistPage />} />
            <Route path="/admin/waitlist" element={<WaitlistPage />} />
          </Routes>
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );

beforeEach(() => {
  getAllDoctors.mockResolvedValue({
    data: [
      { id: 3, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
      { id: 4, name: "Gone", specialist: "NEUROLOGY", active: false },
    ],
  });
});
afterEach(() => jest.resetAllMocks());

test("today's waits of every doctor, each with who, phone, shift, when joined and told, and the status", async () => {
  waitlistApi.getDeskWaitlist.mockResolvedValue({
    data: [
      wait(1, "Asha Verma", { joinedAt: `${TODAY}T09:15:00`, toldAt: `${TODAY}T10:05:00` }),
      wait(2, "Mira Verma", { relativeId: 5, patientId: 41, patientName: "Asha Verma", shift: null, joinedAt: "2026-11-17T09:15:00" }),
      wait(3, "Walk In", { hasLogin: false, phone: "9000081001", joinedBy: "deskravi" }),
      wait(4, "Ravi Kumar", { status: "BOOKED" }),
      wait(5, "Ended One", { status: "ENDED" }),
    ],
  });
  renderAt("/waitlist");

  const asha = (await screen.findByRole("link", { name: "Asha Verma" })).closest("tr");
  expect(waitlistApi.getDeskWaitlist).toHaveBeenCalledWith({ date: TODAY, doctorId: undefined });
  expect(asha).toHaveTextContent("Dr. Anita Rao");
  expect(asha).toHaveTextContent("+919800000001");
  expect(asha).toHaveTextContent("Morning");
  expect(asha).toHaveTextContent("09:15deskmeena");
  expect(asha).toHaveTextContent("10:05");
  expect(within(asha).getByText("Waiting")).toBeInTheDocument();
  expect(within(asha).getByRole("link", { name: "Asha Verma" })).toHaveAttribute("href", "/patients/41");
  expect(within(asha).getByRole("link", { name: "Book Asha Verma" })).toHaveAttribute("href", "/walk-in?patientId=41&doctorId=3");

  const mira = screen.getByRole("link", { name: "Mira Verma" }).closest("tr");
  expect(mira).toHaveTextContent("family of Asha Verma");
  expect(mira).toHaveTextContent("Any time that day");
  expect(mira).toHaveTextContent("Tue 17 Nov, 09:15"); // joined another day
  expect(within(mira).queryByRole("link", { name: /^Book/ })).not.toBeInTheDocument(); // books in the app
  expect(screen.getByRole("link", { name: "Walk In" }).closest("tr")).toHaveTextContent("front-desk record: SMS only");

  const booked = screen.getByRole("link", { name: "Ravi Kumar" }).closest("tr");
  expect(within(booked).getByText("Booked")).toBeInTheDocument();
  expect(within(booked).queryByRole("button")).not.toBeInTheDocument();
  expect(within(screen.getByRole("link", { name: "Ended One" }).closest("tr")).getByText("Ended")).toBeInTheDocument();
  expect(screen.queryByRole("option", { name: /Gone/ })).not.toBeInTheDocument();
});

test("another day and one doctor, kept in the address; nobody waiting says so", async () => {
  renderAt("/admin/waitlist?date=2026-11-20");
  expect(await screen.findByText("Nobody is waiting for Fri 20 Nov 2026.")).toBeInTheDocument();
  expect(waitlistApi.getDeskWaitlist).toHaveBeenCalledWith({ date: "2026-11-20", doctorId: undefined });
  expect(screen.getByLabelText("Day")).toHaveValue("2026-11-20");

  await screen.findByRole("option", { name: "Dr. Anita Rao — Cardiology" });
  fireEvent.change(screen.getByLabelText("Doctor"), { target: { value: "3" } });
  await waitFor(() => expect(waitlistApi.getDeskWaitlist).toHaveBeenLastCalledWith({ date: "2026-11-20", doctorId: "3" }));
  fireEvent.change(screen.getByLabelText("Day"), { target: { value: "2026-11-21" } });
  await waitFor(() => expect(waitlistApi.getDeskWaitlist).toHaveBeenLastCalledWith({ date: "2026-11-21", doctorId: "3" }));
  fireEvent.change(screen.getByLabelText("Doctor"), { target: { value: "" } });
  await waitFor(() => expect(waitlistApi.getDeskWaitlist).toHaveBeenLastCalledWith({ date: "2026-11-21", doctorId: undefined }));
});

test("removing someone asks first and loads the list again; a refused one says why", async () => {
  waitlistApi.getDeskWaitlist.mockResolvedValue({ data: [wait(1, "Asha Verma")] });
  waitlistApi.removeFromWaitlist.mockResolvedValueOnce({}).mockRejectedValueOnce({
    response: { status: 409, data: { message: "This wait has already ended" } },
  });
  renderAt("/admin/waitlist");
  fireEvent.click(await screen.findByRole("button", { name: "Remove Asha Verma" }));
  const dialog = screen.getByRole("dialog", { name: "Remove Asha Verma from the waitlist?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Remove" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Asha Verma is off the waitlist.");
  expect(waitlistApi.removeFromWaitlist).toHaveBeenCalledWith(1);
  await waitFor(() => expect(waitlistApi.getDeskWaitlist).toHaveBeenCalledTimes(2));
  expect(screen.getByRole("link", { name: "Book Asha Verma" })).toHaveAttribute("href", "/admin/walk-in?patientId=41&doctorId=3");

  fireEvent.click(await screen.findByRole("button", { name: "Remove Asha Verma" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Remove" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("This wait has already ended");
});

test("a list that can't be loaded says why", async () => {
  waitlistApi.getDeskWaitlist.mockRejectedValue({ response: { status: 500, data: { message: "Server down" } } });
  renderAt("/waitlist");
  expect(await screen.findByRole("alert")).toHaveTextContent("Server down");
  expect(screen.queryByText(/Nobody is waiting/)).not.toBeInTheDocument();
});
