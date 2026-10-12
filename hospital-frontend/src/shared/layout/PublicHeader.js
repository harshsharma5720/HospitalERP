import React, { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { HeartPulse, LayoutDashboard, LogOut, Menu, Phone, UserCircle, X } from "lucide-react";
import useAuthStore, { homePathForRole } from "../../features/auth/useAuthStore";
import { getUsernameFromToken } from "../utils/jwtUtils";
import { HOSPITAL, phoneHref } from "../hospitalInfo";
import ProfilePage from "../ProfilePage";
import { Button, ThemeButton } from "../ui";

// The header of the public site and the patient pages (docs/UI_REFRESH_PLAN.md, U.6): the hospital, the site's
// pages, the phone number, the light / dark switch and the visitor's way in - Log in / Register, or for a logged-in
// user their profile, their appointments (patients) or the staff portal (staff), and Log out. On a phone the
// links fold into a menu.
const LINKS = [
  { to: "/", label: "Home", end: true },
  { to: "/treatments", label: "Treatments" },
  { to: "/doctors", label: "Doctors" },
  { to: "/contact", label: "Contact Us" },
  { to: "/about", label: "About Us" },
];
const STAFF = ["ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_RECEPTIONIST"];

const linkClass = ({ isActive }) =>
  `rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
    isActive ? "text-primary-ink" : "text-ink-muted hover:text-ink hover:bg-surface-2"
  }`;

export default function PublicHeader() {
  const role = useAuthStore((s) => s.role);
  const token = useAuthStore((s) => s.token);
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [showProfile, setShowProfile] = useState(false);

  useEffect(() => setMenuOpen(false), [pathname]);

  const logout = () => {
    useAuthStore.getState().logout(); // clears token + role everywhere
    navigate("/");
  };

  const signedIn = Boolean(role);
  const accountLinks = signedIn ? (
    <>
      {STAFF.includes(role) && (
        <Button as={Link} to={homePathForRole(role)} variant="secondary" size="sm">
          <LayoutDashboard size={16} aria-hidden="true" />
          Staff portal
        </Button>
      )}
      {role === "ROLE_PATIENT" && (
        <Button as={Link} to="/appointment-details" variant="secondary" size="sm">
          My appointments
        </Button>
      )}
      <Button variant="ghost" size="sm" onClick={() => setShowProfile(true)}>
        <UserCircle size={16} aria-hidden="true" />
        Profile
      </Button>
      <Button variant="ghost" size="sm" onClick={logout} aria-label={`Log out ${getUsernameFromToken(token) ?? ""}`.trim()}>
        <LogOut size={16} aria-hidden="true" />
        Log out
      </Button>
    </>
  ) : (
    <>
      <Button as={Link} to="/login" variant="ghost" size="sm">
        Log in
      </Button>
      <Button as={Link} to="/register" size="sm">
        Register
      </Button>
    </>
  );

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-surface/85 backdrop-blur supports-[backdrop-filter]:bg-surface/75">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex shrink-0 items-center gap-2.5 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-white">
            <HeartPulse size={20} aria-hidden="true" />
          </span>
          <span className="leading-tight">
            <span className="block font-semibold text-ink">{HOSPITAL.name}</span>
            <span className="hidden text-xs text-ink-muted sm:block">{HOSPITAL.area}</span>
          </span>
        </Link>

        <nav aria-label="Main" className="ml-4 hidden items-center gap-1 lg:flex">
          {LINKS.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className={linkClass}>
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-1 sm:gap-2">
          <a
            href={phoneHref}
            className="hidden items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-ink hover:bg-surface-2 xl:flex"
          >
            <Phone size={16} aria-hidden="true" className="text-primary-ink" />
            {HOSPITAL.phone}
          </a>
          <ThemeButton />
          <div className="hidden items-center gap-2 md:flex">{accountLinks}</div>
          <button
            type="button"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-expanded={menuOpen}
            aria-label={menuOpen ? "Close menu" : "Open menu"}
            className="rounded-lg p-2 text-ink-muted hover:bg-surface-2 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 lg:hidden"
          >
            {menuOpen ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
          </button>
        </div>
      </div>

      {menuOpen && (
        <div className="border-t border-line bg-surface px-4 pb-4 pt-2 shadow-card lg:hidden">
          <nav aria-label="Menu" className="flex flex-col">
            {LINKS.map((link) => (
              <NavLink key={link.to} to={link.to} end={link.end} className={linkClass}>
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="mt-3 flex flex-wrap gap-2 border-t border-line pt-3 md:hidden">{accountLinks}</div>
        </div>
      )}

      {showProfile && <ProfilePage onClose={() => setShowProfile(false)} />}
    </header>
  );
}
