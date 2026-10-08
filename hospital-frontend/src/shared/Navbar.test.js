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
  expect(screen.queryByText("My Appointments")).not.toBeInTheDocument();
});

test("doctors get their appointments link", () => {
  renderAs("ROLE_DOCTOR");
  expect(screen.getByText("My Appointments").closest("a")).toHaveAttribute("href", "/doctor-appointments");
  expect(screen.queryByText("Walk-in")).not.toBeInTheDocument();
  expect(screen.getByText("Profile")).toBeInTheDocument();
});

test("receptionists get the front-desk appointments and walk-in links", () => {
  renderAs("ROLE_RECEPTIONIST");
  expect(screen.getByText("My Appointments").closest("a")).toHaveAttribute("href", "/receptionist-appointments");
  expect(screen.getByText("Walk-in").closest("a")).toHaveAttribute("href", "/walk-in");
});

test("admins get no appointments link", () => {
  renderAs("ROLE_ADMIN");
  expect(screen.queryByText("My Appointments")).not.toBeInTheDocument();
  expect(screen.queryByText("Walk-in")).not.toBeInTheDocument(); // admins: in the admin portal's sidebar
  expect(screen.getByText("Profile")).toBeInTheDocument();
});
