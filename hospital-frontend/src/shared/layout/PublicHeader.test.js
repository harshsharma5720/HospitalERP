import { render, screen, fireEvent, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import PublicHeader from "./PublicHeader";
import useAuthStore from "../../features/auth/useAuthStore";


// Unsigned test token; the header only reads the payload
const tokenFor = (sub, role) => `x.${btoa(JSON.stringify({ sub, role, userId: 1 }))}.y`;

const renderAs = (role, path = "/somewhere") => {
  const token = role ? tokenFor("asha", role) : null;
  if (token) localStorage.setItem("jwtToken", token);
  useAuthStore.setState({ token, role, userId: role ? 1 : null });
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="*" element={<PublicHeader />} />
      </Routes>
      <Routes>
        <Route path="/" element={<p>home page</p>} />
        <Route path="*" element={null} />
      </Routes>
    </MemoryRouter>
  );
};

const main = () => screen.getByRole("navigation", { name: "Main" });

afterEach(() => {
  localStorage.clear();
  useAuthStore.setState({ token: null, role: null, userId: null });
});

test("everyone sees the hospital, the site's pages and the phone number; visitors get Log in and Register", () => {
  renderAs(null);
  expect(screen.getByRole("link", { name: /Shreya Hospital/ })).toHaveAttribute("href", "/");
  for (const [name, href] of [["Home", "/"], ["Treatments", "/treatments"], ["Doctors", "/doctors"], ["Contact Us", "/contact"], ["About Us", "/about"]]) {
    expect(within(main()).getByRole("link", { name })).toHaveAttribute("href", href);
  }
  expect(screen.getByRole("link", { name: "+91 92895 20303" })).toHaveAttribute("href", "tel:+919289520303");
  expect(screen.getByRole("link", { name: "Log in" })).toHaveAttribute("href", "/login");
  expect(screen.getByRole("link", { name: "Register" })).toHaveAttribute("href", "/register");
  expect(screen.queryByRole("link", { name: "My account" })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: /Staff portal/ })).not.toBeInTheDocument();
});

test("the page you're on is marked", () => {
  renderAs(null, "/doctors");
  expect(within(main()).getByRole("link", { name: "Doctors" })).toHaveAttribute("aria-current", "page");
  expect(within(main()).getByRole("link", { name: "Home" })).not.toHaveAttribute("aria-current");
});

test.each([
  ["ROLE_DOCTOR", "/doctor/dashboard"],
  ["ROLE_RECEPTIONIST", "/receptionist-appointments"],
  ["ROLE_ADMIN", "/admin/dashboard"],
])("%s gets a link back to the staff portal (%s)", (role, home) => {
  renderAs(role);
  expect(screen.getByRole("link", { name: "Staff portal" })).toHaveAttribute("href", home);
  expect(screen.queryByRole("link", { name: "My appointments" })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "My account" })).not.toBeInTheDocument(); // their profile is in the portal
});

test("a patient gets their appointments and their account, and no staff portal", () => {
  renderAs("ROLE_PATIENT");
  expect(screen.getByRole("link", { name: "My appointments" })).toHaveAttribute("href", "/appointment-details");
  expect(screen.getByRole("link", { name: "My account" })).toHaveAttribute("href", "/edit-profile");
  expect(screen.queryByRole("link", { name: "Staff portal" })).not.toBeInTheDocument();
});

test("Log out ends the session and goes to the home page", () => {
  renderAs("ROLE_PATIENT");
  fireEvent.click(screen.getByRole("button", { name: "Log out asha" }));
  expect(screen.getByText("home page")).toBeInTheDocument();
  expect(localStorage.getItem("jwtToken")).toBeNull();
  expect(screen.getByRole("link", { name: "Log in" })).toBeInTheDocument();
});

test("on a phone the links fold into a menu that closes on the next page", () => {
  renderAs(null);
  expect(screen.queryByRole("navigation", { name: "Menu" })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Open menu" }));
  const menu = screen.getByRole("navigation", { name: "Menu" });
  expect(screen.getByRole("button", { name: "Close menu" })).toHaveAttribute("aria-expanded", "true");
  fireEvent.click(within(menu).getByRole("link", { name: "Treatments" }));
  expect(screen.queryByRole("navigation", { name: "Menu" })).not.toBeInTheDocument();
});
