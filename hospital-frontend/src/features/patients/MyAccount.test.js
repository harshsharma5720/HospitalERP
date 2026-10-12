import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import MyAccount from "./MyAccount";
import useAuthStore from "../auth/useAuthStore";
import * as patientsApi from "./api";
import { getMyAppointments } from "../appointments/api";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api");
jest.mock("../appointments/api");

const ACCOUNT = {
  patientId: 41, patientName: "Asha Verma", userName: "asha", email: "asha@example.com", contactNo: "9876543210",
  gender: "FEMALE", dob: "1990-04-12", patientAadharNo: "123456789012", patientAddress: "12 MG Road", profileImage: null,
};
const day = (n) => toLocalISODate(addDays(new Date(), n));

// jsdom's Blob has no text(); FileReader reads it
const readBlob = (blob) =>
  new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.readAsText(blob);
  });

const renderPage = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter>
          <MyAccount />
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_PATIENT", userId: 7 });
  patientsApi.getPatientAccount.mockResolvedValue({ data: ACCOUNT });
  patientsApi.getRelatives.mockResolvedValue({
    data: [
      { id: 5, name: "Ravi Verma", relationship: "FATHER", gender: "MALE", dob: "1958-01-01" },
      { id: 6, name: "Mira Verma", relationship: "DAUGHTER", gender: "FEMALE", dob: "2018-06-01" },
    ],
  });
  getMyAppointments.mockResolvedValue({
    data: [
      { appointmentID: 1, date: day(6), status: "SCHEDULED", startTime: "09:00:00", endTime: "09:30:00", doctorName: "Sunil Iyer", patientName: "Asha Verma" },
      { appointmentID: 2, date: day(2), status: "CONFIRMED", startTime: "10:00:00", endTime: "10:30:00", doctorName: "Anita Rao", patientName: "Mira Verma" },
      { appointmentID: 3, date: day(-5), status: "COMPLETED", startTime: "10:00:00", endTime: "10:30:00", doctorName: "Anita Rao", patientName: "Asha Verma" },
    ],
  });
});
afterEach(() => jest.resetAllMocks());

test("shows the patient's details (the Aadhaar number masked), family members and the next visit", async () => {
  renderPage();
  expect(await screen.findByRole("heading", { level: 1, name: "Hello, Asha" })).toBeInTheDocument();
  expect(patientsApi.getPatientAccount).toHaveBeenCalledWith(7);
  const details = screen.getByRole("region", { name: "Your details" });
  expect(details).toHaveTextContent("XXXX XXXX 9012");
  expect(details).not.toHaveTextContent("123456789012");
  expect(details).toHaveTextContent("#41");
  const family = await screen.findByRole("region", { name: "Family members" });
  expect(within(family).getByText("Ravi Verma")).toBeInTheDocument();
  expect(within(family).getByText(/Father · \d+ y · Male/)).toBeInTheDocument();
  const appointments = screen.getByRole("region", { name: "Your appointments" });
  expect(await within(appointments).findByText(/Dr\. Anita Rao · for Mira Verma/)).toBeInTheDocument();
  expect(appointments).toHaveTextContent("and 1 more upcoming");
});

test("editing the details sends them as the profile endpoint expects; an empty Aadhaar number is sent as none", async () => {
  patientsApi.updatePatientAccount.mockImplementation(async (userId, form) => ({ data: { ...ACCOUNT, ...JSON.parse(await readBlob(form.get("ptInfoDTO"))) } }));
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  fireEvent.change(screen.getByLabelText("Phone number"), { target: { value: "9000011111" } });
  fireEvent.change(screen.getByLabelText("Aadhaar number"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

  expect(await screen.findByRole("status")).toHaveTextContent("Your details are saved.");
  const [userId, form] = patientsApi.updatePatientAccount.mock.calls[0];
  expect(userId).toBe(7);
  const sent = JSON.parse(await readBlob(form.get("ptInfoDTO")));
  expect(sent).toMatchObject({ contactNo: "9000011111", patientAadharNo: null, patientName: "Asha Verma" });
  expect(sent).not.toHaveProperty("profileImage");
  expect(form.get("profileImage")).toBeNull();
  expect(screen.queryByLabelText("Phone number")).not.toBeInTheDocument();
  expect(screen.getByRole("region", { name: "Your details" })).toHaveTextContent("9000011111");
});

test("a refused change keeps the form open; Cancel drops the changes", async () => {
  patientsApi.updatePatientAccount.mockRejectedValue({ response: { data: { message: "Email already in use" } } });
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "taken@example.com" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Email already in use");
  expect(screen.getByLabelText("Email")).toHaveValue("taken@example.com");
  fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
  expect(screen.getByRole("region", { name: "Your details" })).toHaveTextContent("asha@example.com");
});

test("removing a family member asks first", async () => {
  patientsApi.deleteRelative.mockResolvedValue({ data: {} });
  renderPage();
  const family = await screen.findByRole("region", { name: "Family members" });
  fireEvent.click(await within(family).findByRole("button", { name: "Remove Mira Verma" }));
  const dialog = screen.getByRole("dialog", { name: "Remove Mira Verma?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(patientsApi.deleteRelative).not.toHaveBeenCalled();

  fireEvent.click(within(family).getByRole("button", { name: "Remove Mira Verma" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Remove" }));
  await waitFor(() => expect(within(family).queryByText("Mira Verma")).not.toBeInTheDocument());
  expect(patientsApi.deleteRelative).toHaveBeenCalledWith(6);
  expect(within(family).getByRole("link", { name: "Edit Ravi Verma" })).toHaveAttribute("href", "/edit-relative");
});
