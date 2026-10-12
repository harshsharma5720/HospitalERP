import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Navbar from "./Navbar";

// Unsigned test token; the navbar only reads the payload
const tokenFor = (role) => `x.${btoa(JSON.stringify({ sub: "user", role, userId: 1 }))}.y`;

const renderAs = (role) => {
  if (role) localStorage.setItem("jwtToken", tokenFor(role));
  render(
    <MemoryRouter>
      <Navbar />
    </MemoryRouter>
  );
};

afterEach(() => localStorage.clear());

test("everyone sees the site links; logged-out users get no profile button", () => {
  renderAs(null);
  for (const name of ["Home", "Treatments", "Doctors", "Contact Us", "About Us"]) {
    expect(screen.getByText(name)).toBeInTheDocument();
  }
  expect(screen.queryByText("Profile")).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Staff portal" })).not.toBeInTheDocument();
});

test.each([
  ["ROLE_DOCTOR", "/doctor/dashboard"],
  ["ROLE_RECEPTIONIST", "/receptionist-appointments"],
  ["ROLE_ADMIN", "/admin/dashboard"],
])("%s gets a link back to the staff portal (%s) and the profile button", (role, home) => {
  renderAs(role);
  expect(screen.getByRole("link", { name: "Staff portal" })).toHaveAttribute("href", home);
  expect(screen.getByText("Profile")).toBeInTheDocument();
});

test("patients get no staff portal link", () => {
  renderAs("ROLE_PATIENT");
  expect(screen.queryByRole("link", { name: "Staff portal" })).not.toBeInTheDocument();
  expect(screen.getByText("Profile")).toBeInTheDocument();
});
