import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import LoginPage from "./Login";
import useAuthStore from "./useAuthStore";
import * as authApi from "./api";

jest.mock("./api");

// Unsigned test token; the app only reads the payload
const tokenFor = (role) => `x.${btoa(JSON.stringify({ sub: "asha", role, userId: 7, exp: 4102444800 }))}.y`;

const renderAt = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<p>home page</p>} />
        <Route path="/doctor/dashboard" element={<p>doctor dashboard</p>} />
        <Route path="/appointments" element={<p>booking page</p>} />
      </Routes>
    </MemoryRouter>
  );

const logIn = (username = "asha", password = "secret") => {
  fireEvent.change(screen.getByLabelText("Username or email"), { target: { value: username } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
};

afterEach(() => {
  jest.resetAllMocks();
  localStorage.clear();
  useAuthStore.setState({ token: null, role: null, userId: null });
});

test("logs in and goes to the role's home", async () => {
  authApi.login.mockResolvedValue({ data: { token: tokenFor("ROLE_DOCTOR") } });
  renderAt("/login");
  logIn("drrao", "pw");
  expect(await screen.findByText("doctor dashboard")).toBeInTheDocument();
  expect(authApi.login).toHaveBeenCalledWith("drrao", "pw");
  expect(useAuthStore.getState().role).toBe("ROLE_DOCTOR");
});

test("goes back to the page that asked for a login", async () => {
  authApi.login.mockResolvedValue({ data: { token: tokenFor("ROLE_PATIENT") } });
  renderAt({ pathname: "/login", state: { from: "/appointments" } });
  logIn();
  expect(await screen.findByText("booking page")).toBeInTheDocument();
});

test("wrong credentials say so and stay on the page", async () => {
  authApi.login.mockRejectedValue({ response: { data: { message: "Invalid username or password" } } });
  renderAt("/login");
  logIn();
  expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password");
  expect(screen.getByRole("button", { name: "Log in" })).toBeEnabled();
});

test("an expired session is explained; the other ways are links, and no button that does nothing", () => {
  renderAt("/login?expired=1");
  expect(screen.getByRole("status")).toHaveTextContent("Your session has expired");
  expect(screen.getByRole("link", { name: "Forgot password?" })).toHaveAttribute("href", "/forgot-password");
  expect(screen.getByRole("link", { name: "Create an account" })).toHaveAttribute("href", "/register");
  expect(screen.queryByRole("button", { name: /Sign in with other/ })).not.toBeInTheDocument();
});
