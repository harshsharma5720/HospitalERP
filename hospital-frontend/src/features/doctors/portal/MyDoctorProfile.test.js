import { render, screen, fireEvent } from "@testing-library/react";
import MyDoctorProfile from "./MyDoctorProfile";
import * as doctorsApi from "../api";
import { ToastProvider } from "../../../shared/ui";

jest.mock("../api");

const token = `x.${btoa(JSON.stringify({ sub: "rao", role: "ROLE_DOCTOR", userId: 5 }))}.y`;
const DOCTOR = { id: 9, name: "Anita Rao", userName: "rao", email: "rao@example.com", phoneNumber: "9876543210", specialist: "CARDIOLOGY" };

// jsdom's Blob has no text(); FileReader reads it
const readBlob = (blob) =>
  new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.readAsText(blob);
  });

const renderProfile = () =>
  render(
    <ToastProvider>
      <MyDoctorProfile />
    </ToastProvider>
  );

beforeEach(() => {
  localStorage.setItem("jwtToken", token);
  doctorsApi.getDoctor.mockResolvedValue({ data: DOCTOR });
});
afterEach(() => {
  localStorage.clear();
  jest.resetAllMocks();
});

test("shows the profile; editing and saving sends it and says so", async () => {
  doctorsApi.updateDoctor.mockResolvedValue({ data: {} });
  renderProfile();
  expect(await screen.findByRole("heading", { name: "Dr. Anita Rao" })).toBeInTheDocument();
  expect(doctorsApi.getDoctor).toHaveBeenCalledWith(5);
  expect(screen.getAllByText("Cardiology")).not.toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Edit profile" }));
  fireEvent.change(screen.getByLabelText("Phone Number"), { target: { value: "9000011111" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

  expect(await screen.findByRole("status")).toHaveTextContent("Profile updated.");
  const [userId, form] = doctorsApi.updateDoctor.mock.calls[0];
  expect(userId).toBe(5);
  const sent = JSON.parse(await readBlob(form.get("doctorDTO")));
  expect(sent).toMatchObject({ id: 9, phoneNumber: "9000011111", name: "Anita Rao" });
  expect(screen.queryByLabelText("Phone Number")).not.toBeInTheDocument(); // back to viewing
  expect(screen.getByText("9000011111")).toBeInTheDocument();
});

test("Cancel drops the changes", async () => {
  renderProfile();
  fireEvent.click(await screen.findByRole("button", { name: "Edit profile" }));
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "new@example.com" } });
  fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
  expect(screen.getByText("rao@example.com")).toBeInTheDocument();
  expect(doctorsApi.updateDoctor).not.toHaveBeenCalled();
});

test("a failed save says why and stays in editing", async () => {
  doctorsApi.updateDoctor.mockRejectedValue({ response: { data: { message: "Email already in use" } } });
  renderProfile();
  fireEvent.click(await screen.findByRole("button", { name: "Edit profile" }));
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Email already in use");
  expect(screen.getByLabelText("Email")).toBeInTheDocument();
});
