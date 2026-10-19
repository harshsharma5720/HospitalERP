import { cleanup, render, screen, fireEvent, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import StaffLayout from "./StaffLayout";
import axios from "axios";
import useAuthStore from "../../features/auth/useAuthStore";
import useAppStore from "../useAppStore";
import { getOwnProfile } from "../profileApi";

jest.mock("../profileApi", () => ({
  hasProfile: (role) => role === "ROLE_DOCTOR" || role === "ROLE_RECEPTIONIST",
  getOwnProfile: jest.fn(),
}));

// Unsigned test token; the layout only reads the payload
const tokenFor = (sub, role, userId) => `x.${btoa(JSON.stringify({ sub, role, userId }))}.y`;

function Address() {
  return <div data-testid="address">{useLocation().pathname}</div>;
}

const renderLayout = (role, path, { username = "asha", userId = 7 } = {}) => {
  const token = tokenFor(username, role, userId);
  useAuthStore.setState({ token, role, userId });
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<p>login page</p>} />
        <Route
          path="*"
          element={
            <>
              <StaffLayout>
                <p>page content</p>
              </StaffLayout>
              <Address />
            </>
          }
        />
      </Routes>
    </MemoryRouter>
  );
};

const menu = () => screen.getAllByRole("navigation", { name: "Main" })[0];
const menuLink = (name) => within(menu()).getByRole("link", { name });

beforeEach(() => getOwnProfile.mockReset().mockReturnValue(new Promise(() => {})));
afterEach(() => {
  localStorage.clear();
  useAppStore.setState({ darkMode: false });
  document.documentElement.classList.remove("dark");
});

describe("the menu of each role", () => {
  test("a receptionist gets the front-desk pages and the queue screen in a new tab", () => {
    renderLayout("ROLE_RECEPTIONIST", "/walk-in");
    expect(screen.getByText("page content")).toBeInTheDocument();
    expect(screen.getAllByText("Front desk").length).toBeGreaterThan(0);
    expect(menuLink("Appointments")).toHaveAttribute("href", "/receptionist-appointments");
    expect(menuLink("Walk-in")).toHaveAttribute("href", "/walk-in");
    expect(menuLink("Patients")).toHaveAttribute("href", "/patients");
    expect(menuLink("Bills")).toHaveAttribute("href", "/bills");
    expect(menuLink("Day summary")).toHaveAttribute("href", "/cash-summary");
    const queueScreen = menuLink(/Queue screen/);
    expect(queueScreen).toHaveAttribute("href", "/queue-screen");
    expect(queueScreen).toHaveAttribute("target", "_blank");
    expect(queueScreen).toHaveAccessibleName("Queue screen (opens in a new tab)");
    expect(within(menu()).queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(within(menu()).queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  test("a doctor gets the doctor's pages", () => {
    renderLayout("ROLE_DOCTOR", "/doctor/dashboard");
    const expected = {
      Dashboard: "/doctor/dashboard",
      "Today's queue": "/doctor/queue",
      Appointments: "/doctor/doctor-appointments",
      Schedule: "/doctor/schedule",
      Leave: "/doctor/leave-management",
      Profile: "/doctor/profile",
    };
    for (const [name, href] of Object.entries(expected)) {
      expect(menuLink(name)).toHaveAttribute("href", href);
    }
    expect(within(menu()).queryByRole("link", { name: "Walk-in" })).not.toBeInTheDocument();
  });

  test("an admin gets the dashboard, the front desk under /admin and the administration pages - no dead links", () => {
    renderLayout("ROLE_ADMIN", "/admin/dashboard", { username: "flowadmin" });
    expect(within(menu()).getByRole("heading", { name: "Front desk" })).toBeInTheDocument();
    expect(within(menu()).getByRole("heading", { name: "Administration" })).toBeInTheDocument();
    const expected = {
      Dashboard: "/admin/dashboard",
      Appointments: "/admin/appointments",
      "Walk-in": "/admin/walk-in",
      Patients: "/admin/patients",
      Bills: "/admin/bills",
      "Day summary": "/admin/cash-summary",
      Users: "/admin/manage-users",
      Doctors: "/admin/manage-doctor",
      "Leave approvals": "/admin/leave-approval",
      Fees: "/admin/fees",
      "Audit log": "/admin/audit-log",
      Notifications: "/admin/notifications",
    };
    for (const [name, href] of Object.entries(expected)) {
      expect(menuLink(name)).toHaveAttribute("href", href);
    }
    expect(menuLink(/Queue screen/)).toHaveAttribute("href", "/queue-screen");
    expect(screen.queryByText(/Departments|Receptionists/)).not.toBeInTheDocument();
  });
});

describe("the page you're on", () => {
  test.each([
    ["/admin/dashboard", "Dashboard"],
    ["/admin/patients/42", "Patients"],
    ["/admin/bills/visit/9", "Bills"],
    ["/admin/register-user", "Users"],
    ["/admin/doctor/7/appointments", "Doctors"],
  ])("%s lights %s, and only that", (path, name) => {
    renderLayout("ROLE_ADMIN", path);
    expect(menuLink(name)).toHaveAttribute("aria-current", "page");
    const current = within(menu())
      .getAllByRole("link")
      .filter((link) => link.getAttribute("aria-current") === "page");
    expect(current).toHaveLength(1);
  });

  test("a receptionist's patient page lights Patients", () => {
    renderLayout("ROLE_RECEPTIONIST", "/patients/5");
    expect(menuLink("Patients")).toHaveAttribute("aria-current", "page");
    expect(menuLink("Appointments")).not.toHaveAttribute("aria-current");
  });
});

describe("the user in the top bar", () => {
  test("a doctor is shown by name and photo from their profile, linking to it", async () => {
    getOwnProfile.mockResolvedValue({ data: { name: "Anita Rao", profileImage: "/uploads/anita.jpg" } });
    renderLayout("ROLE_DOCTOR", "/doctor/queue", { userId: 12 });
    const profile = await screen.findByRole("link", { name: "My profile (Dr. Anita Rao)" });
    expect(getOwnProfile).toHaveBeenCalledWith("ROLE_DOCTOR", 12);
    expect(profile).toHaveAttribute("href", "/doctor/profile");
    expect(profile.querySelector("img")).toHaveAttribute("src", expect.stringMatching(/\/uploads\/anita\.jpg$/));
  });

  test("a receptionist without a photo gets their initials", async () => {
    getOwnProfile.mockResolvedValue({ data: { name: "Asha Verma" } });
    renderLayout("ROLE_RECEPTIONIST", "/bills");
    const profile = await screen.findByRole("link", { name: "My profile (Asha Verma)" });
    expect(profile).toHaveAttribute("href", "/my-profile");
    expect(profile).toHaveTextContent("AV");
  });

  test("while the profile loads, or if it can't, the login name is shown", async () => {
    getOwnProfile.mockRejectedValue(new Error("offline"));
    renderLayout("ROLE_RECEPTIONIST", "/bills", { username: "desk1" });
    expect(await screen.findByRole("link", { name: "My profile (desk1)" })).toBeInTheDocument();
  });

  test("an admin has no profile: the login name, no link and no request", () => {
    renderLayout("ROLE_ADMIN", "/admin/fees", { username: "flowadmin" });
    expect(screen.getByText("flowadmin")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /My profile/ })).not.toBeInTheDocument();
    expect(getOwnProfile).not.toHaveBeenCalled();
  });

  test("an admin, who has no profile page, gets Log out of all devices in the top bar; the others on their profile", () => {
    renderLayout("ROLE_DOCTOR", "/doctor/dashboard");
    expect(screen.queryByRole("button", { name: "Log out of all devices" })).not.toBeInTheDocument();
    cleanup();
    renderLayout("ROLE_ADMIN", "/admin/fees", { username: "flowadmin" });
    expect(screen.getByRole("button", { name: "Log out of all devices" })).toBeInTheDocument();
  });

  test("every role has the notifications bell", () => {
    for (const role of ["ROLE_DOCTOR", "ROLE_RECEPTIONIST", "ROLE_ADMIN"]) {
      renderLayout(role, "/somewhere");
      expect(screen.getByRole("button", { name: "Notifications" })).toBeInTheDocument();
      cleanup();
    }
  });

  test("Log out ends the session and goes to the login page", () => {
    const post = jest.spyOn(axios, "post").mockResolvedValue({ data: {} });
    renderLayout("ROLE_DOCTOR", "/doctor/dashboard");
    fireEvent.click(screen.getByRole("button", { name: "Log out" }));
    expect(screen.getByText("login page")).toBeInTheDocument();
    expect(useAuthStore.getState().token).toBeNull();
    expect(useAuthStore.getState().role).toBeNull();
    expect(post).toHaveBeenCalledWith(expect.stringMatching(/\/api\/auth\/logout$/), null, expect.objectContaining({ withCredentials: true }));
    post.mockRestore();
  });

  test("the light / dark switch changes the page and remembers it", () => {
    renderLayout("ROLE_ADMIN", "/admin/dashboard");
    fireEvent.click(screen.getByRole("button", { name: "Switch to dark mode" }));
    expect(document.documentElement).toHaveClass("dark");
    expect(localStorage.getItem("theme")).toBe("dark");
    fireEvent.click(screen.getByRole("button", { name: "Switch to light mode" }));
    expect(document.documentElement).not.toHaveClass("dark");
    expect(localStorage.getItem("theme")).toBe("light");
  });
});

describe("the menu on a phone", () => {
  const openDrawer = () => {
    fireEvent.click(screen.getByRole("button", { name: "Open menu" }));
    return screen.getByRole("dialog", { name: "Menu" });
  };

  test("opens as a drawer, focused on its close button, and a page in it closes it", () => {
    renderLayout("ROLE_RECEPTIONIST", "/receptionist-appointments");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    const drawer = openDrawer();
    expect(screen.getByRole("button", { name: "Open menu" })).toHaveAttribute("aria-expanded", "true");
    expect(within(drawer).getByRole("button", { name: "Close menu" })).toHaveFocus();
    fireEvent.click(within(drawer).getByRole("link", { name: "Bills" }));
    expect(screen.getByTestId("address")).toHaveTextContent("/bills");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  test("closes with ×, with Escape and with a tap beside it", () => {
    renderLayout("ROLE_DOCTOR", "/doctor/dashboard");
    fireEvent.click(within(openDrawer()).getByRole("button", { name: "Close menu" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    openDrawer();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    const drawer = openDrawer();
    fireEvent.click(drawer.previousSibling);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
