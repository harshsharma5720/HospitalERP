import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import axios from "axios";
import ForgotPasswordPage from "./ForgotPasswordPage";
import { ToastProvider } from "../../shared/ui";

jest.mock("axios");

const renderPage = () =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={["/forgot-password"]}>
        <Routes>
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/login" element={<p>login page</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

afterEach(() => jest.resetAllMocks());

test("requests a code, then resets the password with it and goes to the login", async () => {
  axios.post.mockResolvedValueOnce({ data: { message: "If an account exists, a reset code has been sent." } });
  axios.post.mockResolvedValueOnce({ data: { success: true } });
  renderPage();

  fireEvent.change(screen.getByLabelText("Username or email"), { target: { value: "asha" } });
  fireEvent.click(screen.getByRole("button", { name: "Send code" }));

  expect(await screen.findByText(/reset code has been sent/)).toBeInTheDocument();
  expect(axios.post).toHaveBeenCalledWith(expect.stringContaining("/api/auth/forgot-password"), { identifier: "asha" });

  fireEvent.change(screen.getByLabelText("Reset code"), { target: { value: "123456" } });
  fireEvent.change(screen.getByLabelText("New password"), { target: { value: "new-secret" } });
  fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "new-secret" } });
  fireEvent.click(screen.getByRole("button", { name: "Reset password" }));

  expect(await screen.findByText("login page")).toBeInTheDocument();
  expect(axios.post).toHaveBeenLastCalledWith(expect.stringContaining("/api/auth/reset-password"), {
    identifier: "asha",
    code: "123456",
    newPassword: "new-secret",
  });
  expect(screen.getByRole("status")).toHaveTextContent("Password changed.");
});

test("shows an error when the passwords differ", async () => {
  axios.post.mockResolvedValueOnce({ data: { message: "sent" } });
  renderPage();
  fireEvent.change(screen.getByLabelText("Username or email"), { target: { value: "asha" } });
  fireEvent.click(screen.getByRole("button", { name: "Send code" }));
  await screen.findByLabelText("Reset code");

  fireEvent.change(screen.getByLabelText("Reset code"), { target: { value: "123456" } });
  fireEvent.change(screen.getByLabelText("New password"), { target: { value: "one-secret" } });
  fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "two-secret" } });
  fireEvent.click(screen.getByRole("button", { name: "Reset password" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("Passwords do not match");
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
});
