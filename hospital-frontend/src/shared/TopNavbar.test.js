import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import TopNavbar from "./TopNavbar";

// Unsigned test token; the navbar only reads the payload
const tokenFor = (username) => `x.${btoa(JSON.stringify({ sub: username }))}.y`;

const renderNavbar = () =>
  render(
    <MemoryRouter initialEntries={["/somewhere"]}>
      <Routes>
        <Route path="/somewhere" element={<TopNavbar />} />
        <Route path="/" element={<p>home page</p>} />
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

test("logged in: shows the user and logs out to the home page", () => {
  localStorage.setItem("jwtToken", tokenFor("asha"));
  renderNavbar();
  expect(screen.getByText("asha")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: /logout/i }));

  expect(screen.getByText("home page")).toBeInTheDocument();
  expect(localStorage.getItem("jwtToken")).toBeNull();
});
