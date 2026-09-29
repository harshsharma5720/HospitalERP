import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import TopNavbar from "./TopNavbar";

// Unsigned test token; the navbar only reads the payload
const tokenFor = (username) => `x.${btoa(JSON.stringify({ sub: username }))}.y`;

const renderNavbar = (variant) =>
  render(
    <MemoryRouter initialEntries={["/somewhere"]}>
      <Routes>
        <Route path="/somewhere" element={<TopNavbar variant={variant} />} />
        <Route path="/" element={<p>home page</p>} />
        <Route path="/login" element={<p>login page</p>} />
      </Routes>
    </MemoryRouter>
  );

afterEach(() => localStorage.clear());

test("logged out: login and register buttons, site logo with the name", () => {
  renderNavbar();
  expect(screen.getByRole("button", { name: /login/i })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /register/i })).toBeInTheDocument();
  expect(screen.getByText("HospitalERP")).toBeInTheDocument();
});

test("site variant: shows the user and logs out to the home page", () => {
  localStorage.setItem("jwtToken", tokenFor("asha"));
  renderNavbar();
  expect(screen.getByText("asha")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: /logout/i }));

  expect(screen.getByText("home page")).toBeInTheDocument();
  expect(localStorage.getItem("jwtToken")).toBeNull();
});

test("portal variant: compact logo and logs out to the login page", () => {
  localStorage.setItem("jwtToken", tokenFor("admin"));
  renderNavbar("portal");
  expect(screen.queryByText("HospitalERP")).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: /logout/i }));

  expect(screen.getByText("login page")).toBeInTheDocument();
  expect(localStorage.getItem("jwtToken")).toBeNull();
});
