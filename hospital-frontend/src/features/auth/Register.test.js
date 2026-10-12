import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import RegisterPage from "./Register";
import useAuthStore from "./useAuthStore";
import * as authApi from "./api";
import { ToastProvider } from "../../shared/ui";

jest.mock("./api");

const TOKEN = `x.${btoa(JSON.stringify({ sub: "asha", role: "ROLE_PATIENT", userId: 7, exp: 4102444800 }))}.y`;

const renderPage = () =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={["/register"]}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<p>home page</p>} />
          <Route path="/login" element={<p>login page</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

const fill = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

const fillAccount = () => {
  fill("Email", "asha@example.com");
  fill("Username", " asha ");
  fill("Phone number", "9876543210");
  fill("Password", "secret1");
  fill("Confirm password", "secret1");
};

afterEach(() => {
  jest.resetAllMocks();
  localStorage.clear();
  useAuthStore.setState({ token: null, role: null, userId: null });
});

test("with phone verification: send the code, verify it, then register and log in", async () => {
  authApi.isOtpRequired.mockResolvedValue({ data: { otpRequired: true } });
  authApi.sendOtp.mockResolvedValue({ data: { message: "Code sent to your phone." } });
  authApi.verifyOtp.mockResolvedValue({ data: { success: true } });
  authApi.register.mockResolvedValue({ data: { token: TOKEN } });
  renderPage();
  fillAccount();
  expect(screen.getByRole("button", { name: "Create account" })).toBeDisabled();

  fireEvent.click(screen.getByRole("button", { name: "Send code" }));
  expect(await screen.findByText("Code sent to your phone.")).toBeInTheDocument();
  expect(authApi.sendOtp).toHaveBeenCalledWith("9876543210");
  fill("Code", "123456");
  fireEvent.click(screen.getByRole("button", { name: "Verify" }));
  expect(await screen.findByText("Phone number verified")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "Create account" }));
  expect(await screen.findByText("home page")).toBeInTheDocument();
  expect(authApi.register).toHaveBeenCalledWith({
    email: "asha@example.com", username: "asha", phoneNumber: "9876543210", password: "secret1",
  });
  expect(useAuthStore.getState().role).toBe("ROLE_PATIENT");
});

test("without phone verification (OTP_REQUIRED=false) registering works straight away", async () => {
  authApi.isOtpRequired.mockResolvedValue({ data: { otpRequired: false } });
  authApi.register.mockResolvedValue({ data: { token: TOKEN } });
  renderPage();
  await waitFor(() => expect(screen.queryByRole("button", { name: "Send code" })).not.toBeInTheDocument());
  fillAccount();
  const create = screen.getByRole("button", { name: "Create account" });
  expect(create).toBeEnabled();
  fireEvent.click(create);
  expect(await screen.findByText("home page")).toBeInTheDocument();
});

test("a wrong code keeps the account from being created", async () => {
  authApi.isOtpRequired.mockResolvedValue({ data: { otpRequired: true } });
  authApi.sendOtp.mockResolvedValue({ data: { message: "Code sent." } });
  authApi.verifyOtp.mockResolvedValue({ data: { success: false } });
  renderPage();
  fillAccount();
  fireEvent.click(screen.getByRole("button", { name: "Send code" }));
  await screen.findByLabelText("Code");
  fill("Code", "000000");
  fireEvent.click(screen.getByRole("button", { name: "Verify" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("That code isn't right");
  expect(screen.getByRole("button", { name: "Create account" })).toBeDisabled();
});

test("changing the phone number after verifying needs a new code", async () => {
  authApi.isOtpRequired.mockResolvedValue({ data: { otpRequired: true } });
  authApi.sendOtp.mockResolvedValue({ data: { message: "Code sent." } });
  authApi.verifyOtp.mockResolvedValue({ data: { success: true } });
  renderPage();
  fillAccount();
  fireEvent.click(screen.getByRole("button", { name: "Send code" }));
  await screen.findByLabelText("Code");
  fill("Code", "123456");
  fireEvent.click(screen.getByRole("button", { name: "Verify" }));
  await screen.findByText("Phone number verified");

  fill("Phone number", "9000000000");
  expect(screen.queryByText("Phone number verified")).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Send code" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Create account" })).toBeDisabled();
});

test("different passwords are caught before anything is sent", async () => {
  authApi.isOtpRequired.mockResolvedValue({ data: { otpRequired: false } });
  renderPage();
  await waitFor(() => expect(screen.queryByRole("button", { name: "Send code" })).not.toBeInTheDocument());
  fillAccount();
  fill("Confirm password", "other");
  fireEvent.click(screen.getByRole("button", { name: "Create account" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Passwords do not match");
  expect(authApi.register).not.toHaveBeenCalled();
});
