import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ExternalLink, HeartPulse, LogOut, Menu, X } from "lucide-react";
import useAuthStore from "../../features/auth/useAuthStore";
import { hasProfile, getOwnProfile } from "../profileApi";
import { getUsernameFromToken } from "../utils/jwtUtils";
import { API_BASE_URL } from "../../app/config";
import { Button, ThemeButton } from "../ui";
import { LogOutEverywhereButton } from "../../features/auth/LogOutEverywhere";
import { STAFF_MENUS, isActive } from "./staffMenus";

// The staff portals' frame (docs/UI_REFRESH_PLAN.md): the logged-in role's menu on the left (a drawer on a phone)
// and a top bar with the user, the light / dark switch and Log out. The page comes as children, or from the
// matching child route (<Outlet />).
export default function StaffLayout({ children }) {
  const role = useAuthStore((s) => s.role);
  const token = useAuthStore((s) => s.token);
  const userId = useAuthStore((s) => s.userId);
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const closeDrawer = useCallback(() => setDrawerOpen(false), []);

  const menu = STAFF_MENUS[role] ?? { area: "", sections: [] };
  const user = useStaffUser(role, userId, token);

  const logout = () => {
    useAuthStore.getState().logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[80] focus:rounded-lg focus:bg-surface focus:px-4 focus:py-2 focus:shadow-overlay"
      >
        Skip to content
      </a>

      <aside className="hidden border-r border-line bg-surface lg:fixed lg:inset-y-0 lg:left-0 lg:z-30 lg:flex lg:w-64 lg:flex-col">
        <Sidebar menu={menu} pathname={pathname} />
      </aside>

      {drawerOpen && <Drawer menu={menu} pathname={pathname} onClose={closeDrawer} />}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-line bg-surface/90 px-4 backdrop-blur sm:px-6 lg:px-8">
          <button
            type="button"
            onClick={() => setDrawerOpen(true)}
            aria-label="Open menu"
            aria-expanded={drawerOpen}
            className={`${ICON_BUTTON} lg:hidden`}
          >
            <Menu size={20} aria-hidden="true" />
          </button>
          <div className="lg:hidden">
            <Brand />
          </div>

          <div className="ml-auto flex items-center gap-1 sm:gap-2">
            <ThemeButton />
            <UserChip user={user} area={menu.area} profilePath={menu.profilePath} />
            {/* The others have it on their profile page */}
            {role === "ROLE_ADMIN" && <LogOutEverywhereButton />}
            <Button variant="ghost" size="sm" onClick={logout} aria-label="Log out">
              <LogOut size={16} aria-hidden="true" />
              <span className="hidden sm:inline">Log out</span>
            </Button>
          </div>
        </header>

        <main id="main-content" className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  );
}

const ICON_BUTTON =
  "rounded-lg p-2 text-ink-muted hover:bg-surface-2 hover:text-ink " +
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40";

const LINK = "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40";

function Brand({ area }) {
  return (
    <div className="flex items-center gap-2.5">
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-white">
        <HeartPulse size={18} aria-hidden="true" />
      </span>
      <span className="leading-tight">
        <span className="block text-sm font-semibold text-ink">HospitalERP</span>
        {area && <span className="block text-xs text-ink-muted">{area}</span>}
      </span>
    </div>
  );
}

function Sidebar({ menu, pathname, onNavigate }) {
  return (
    <>
      <div className="flex h-16 shrink-0 items-center border-b border-line px-5">
        <Brand area={menu.area} />
      </div>
      <nav aria-label="Main" className="flex-1 overflow-y-auto px-3 py-4">
        {menu.sections.map((section, i) => (
          <div key={section.title ?? i} className={i > 0 ? "mt-6" : ""}>
            {section.title && (
              <h2 className="px-3 pb-2 text-xs font-semibold uppercase tracking-wide text-ink-muted">{section.title}</h2>
            )}
            <ul className="space-y-1">
              {section.items.map((item) => (
                <li key={item.label}>
                  <MenuLink item={item} active={isActive(item, pathname)} onNavigate={onNavigate} />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>
    </>
  );
}

function MenuLink({ item, active, onNavigate }) {
  const Icon = item.icon;
  if (item.href) {
    return (
      <a href={item.href} target="_blank" rel="noreferrer" className={`${LINK} text-ink-muted hover:bg-surface-2 hover:text-ink`}>
        <Icon size={18} aria-hidden="true" className="shrink-0" />
        <span className="flex-1">{item.label}</span>
        <ExternalLink size={14} aria-hidden="true" />
        <span className="sr-only">(opens in a new tab)</span>
      </a>
    );
  }
  return (
    <Link
      to={item.to}
      onClick={onNavigate}
      aria-current={active ? "page" : undefined}
      className={`${LINK} ${active ? "bg-primary-ink/10 text-primary-ink" : "text-ink-muted hover:bg-surface-2 hover:text-ink"}`}
    >
      <Icon size={18} aria-hidden="true" className="shrink-0" />
      <span className="flex-1">{item.label}</span>
    </Link>
  );
}

// The menu on a phone: slides over the page; closes with ×, Escape, a tap beside it or on a page
function Drawer({ menu, pathname, onClose }) {
  const closeButton = useRef(null);

  useEffect(() => {
    closeButton.current?.focus();
    const onKey = (e) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-40 lg:hidden">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} aria-hidden="true" />
      <div role="dialog" aria-modal="true" aria-label="Menu" className="absolute inset-y-0 left-0 flex w-72 max-w-[85vw] flex-col bg-surface shadow-overlay">
        <button ref={closeButton} type="button" onClick={onClose} aria-label="Close menu" className={`${ICON_BUTTON} absolute right-3 top-3`}>
          <X size={20} aria-hidden="true" />
        </button>
        <Sidebar menu={menu} pathname={pathname} onNavigate={onClose} />
      </div>
    </div>
  );
}

function UserChip({ user, area, profilePath }) {
  const body = (
    <>
      <Avatar name={user.name} photo={user.photo} />
      <span className="hidden text-left leading-tight sm:block">
        <span className="block text-sm font-medium text-ink">{user.name}</span>
        <span className="block text-xs text-ink-muted">{area}</span>
      </span>
    </>
  );
  const chip = "flex items-center gap-2.5 rounded-lg px-2 py-1.5";
  if (!profilePath) return <div className={chip}>{body}</div>;
  return (
    <Link
      to={profilePath}
      aria-label={`My profile (${user.name})`}
      className={`${chip} hover:bg-surface-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40`}
    >
      {body}
    </Link>
  );
}

function Avatar({ name, photo }) {
  if (photo) return <img src={photo} alt="" className="h-8 w-8 rounded-full object-cover" />;
  const initials = (name || "?")
    .replace(/^Dr\.?\s+/i, "")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join("");
  return (
    <span aria-hidden="true" className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-ink/10 text-xs font-semibold text-primary-ink">
      {initials}
    </span>
  );
}

// The name and photo in the top bar: from the user's profile (doctors and receptionists have one), else the login name
function useStaffUser(role, userId, token) {
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    setProfile(null);
    if (!userId || !hasProfile(role)) return undefined;
    let current = true;
    getOwnProfile(role, userId)
      .then((res) => current && setProfile(res.data))
      .catch(() => {}); // the login name stays
    return () => {
      current = false;
    };
  }, [role, userId]);

  const name = profile?.name ? (role === "ROLE_DOCTOR" ? `Dr. ${profile.name}` : profile.name) : getUsernameFromToken(token) ?? "";
  return { name, photo: profile?.profileImage ? `${API_BASE_URL}${profile.profileImage}` : null };
}
