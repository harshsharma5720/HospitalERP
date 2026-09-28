import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import ForgotPasswordPage from "./ForgotPasswordPage";

jest.mock("axios");

const renderPage = () =>
  render(
    <MemoryRouter>
      <ForgotPasswordPage />
    </MemoryRouter>
  );

afterEach(() => jest.resetAllMocks());

test("requests a code, then resets the password with it", async () => {
  axios.post.mockResolvedValueOnce({ data: { message: "If an account exists, a reset code has been sent." } });
  axios.post.mockResolvedValueOnce({ data: { success: true } });
  window.alert = jest.fn();
  renderPage();

  fireEvent.change(screen.getByLabelText("Username or email"), { target: { value: "asha" } });
  fireEvent.click(screen.getByText("Send Code"));

  expect(await screen.findByText(/reset code has been sent/)).toBeInTheDocument();
  expect(axios.post).toHaveBeenCalledWith(expect.stringContaining("/api/auth/forgot-password"), { identifier: "asha" });

  fireEvent.change(screen.getByLabelText("Reset code"), { target: { value: "123456" } });
  fireEvent.change(screen.getByLabelText("New password"), { target: { value: "new-secret" } });
  fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "new-secret" } });
  fireEvent.click(screen.getByText("Reset Password"));

  await waitFor(() =>
    expect(axios.post).toHaveBeenLastCalledWith(expect.stringContaining("/api/auth/reset-password"), {
      identifier: "asha",
      code: "123456",
      newPassword: "new-secret",
    })
  );
});

test("shows an error when the passwords differ", async () => {
  axios.post.mockResolvedValueOnce({ data: { message: "sent" } });
  renderPage();
  fireEvent.change(screen.getByLabelText("Username or email"), { target: { value: "asha" } });
  fireEvent.click(screen.getByText("Send Code"));
  await screen.findByLabelText("Reset code");

  fireEvent.change(screen.getByLabelText("Reset code"), { target: { value: "123456" } });
  fireEvent.change(screen.getByLabelText("New password"), { target: { value: "one-secret" } });
  fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "two-secret" } });
  fireEvent.click(screen.getByText("Reset Password"));

  expect(await screen.findByText("Passwords do not match")).toBeInTheDocument();
  expect(axios.post).toHaveBeenCalledTimes(1);
});
