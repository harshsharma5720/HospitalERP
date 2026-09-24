import { render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import useAuthStore from "../Store/useAuthStore";

const renderAt = (role) => {
  useAuthStore.setState({ token: role ? "t" : null, role, userId: role ? 1 : null });
  render(
    <MemoryRouter initialEntries={["/appointments"]}>
      <Routes>
        <Route path="/" element={<p>home page</p>} />
        <Route path="/login" element={<p>login page</p>} />
        <Route
          path="/appointments"
          element={<ProtectedRoute roles={["ROLE_PATIENT"]}><p>booking page</p></ProtectedRoute>}
        />
      </Routes>
    </MemoryRouter>
  );
};

test("logged-out users are sent to login", () => {
  renderAt(null);
  expect(screen.getByText("login page")).toBeInTheDocument();
});

test("users with another role are sent home", () => {
  renderAt("ROLE_DOCTOR");
  expect(screen.getByText("home page")).toBeInTheDocument();
});

test("allowed role sees the page", () => {
  renderAt("ROLE_PATIENT");
  expect(screen.getByText("booking page")).toBeInTheDocument();
});
