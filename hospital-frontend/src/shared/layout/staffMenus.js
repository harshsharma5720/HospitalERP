import {
  LayoutDashboard, CalendarDays, UserPlus, Users, Receipt, MonitorPlay, ListOrdered, Clock, CalendarOff, UserRound,
  UserCog, Stethoscope, CalendarCheck, IndianRupee, ScrollText, Bell, Banknote,
} from "lucide-react";

// The staff portals' menus (docs/UI_REFRESH_PLAN.md), per role. An item is a page in the app (to; also lit on the
// pages under it, and on the paths in "also") or a page opened in a new tab (href).

// The front-desk pages: a receptionist's at the top level, an admin's under /admin
const frontDesk = (base) => [
  { label: "Appointments", to: base ? `${base}/appointments` : "/receptionist-appointments", icon: CalendarDays },
  { label: "Walk-in", to: `${base}/walk-in`, icon: UserPlus },
  { label: "Patients", to: `${base}/patients`, icon: Users },
  { label: "Bills", to: `${base}/bills`, icon: Receipt },
  { label: "Day summary", to: `${base}/cash-summary`, icon: Banknote },
  { label: "Queue screen", href: "/queue-screen", icon: MonitorPlay },
];

export const STAFF_MENUS = {
  ROLE_RECEPTIONIST: {
    area: "Front desk",
    profilePath: "/my-profile",
    sections: [{ items: frontDesk("") }],
  },
  ROLE_DOCTOR: {
    area: "Doctor",
    profilePath: "/doctor/profile",
    sections: [
      {
        items: [
          { label: "Dashboard", to: "/doctor/dashboard", icon: LayoutDashboard },
          { label: "Today's queue", to: "/doctor/queue", icon: ListOrdered },
          { label: "Appointments", to: "/doctor/doctor-appointments", icon: CalendarDays },
          { label: "Schedule", to: "/doctor/schedule", icon: Clock },
          { label: "Leave", to: "/doctor/leave-management", icon: CalendarOff },
          { label: "Profile", to: "/doctor/profile", icon: UserRound },
        ],
      },
    ],
  },
  ROLE_ADMIN: {
    area: "Admin",
    sections: [
      { items: [{ label: "Dashboard", to: "/admin/dashboard", icon: LayoutDashboard }] },
      { title: "Front desk", items: frontDesk("/admin") },
      {
        title: "Administration",
        items: [
          { label: "Users", to: "/admin/manage-users", also: ["/admin/register-user"], icon: UserCog },
          { label: "Doctors", to: "/admin/manage-doctor", also: ["/admin/doctor/"], icon: Stethoscope },
          { label: "Leave approvals", to: "/admin/leave-approval", icon: CalendarCheck },
          { label: "Fees", to: "/admin/fees", icon: IndianRupee },
          { label: "Audit log", to: "/admin/audit-log", icon: ScrollText },
          { label: "Notifications", to: "/admin/notifications", icon: Bell },
        ],
      },
    ],
  },
};

// True when the item is the page at this path, or a page under it
export const isActive = (item, pathname) =>
  Boolean(item.to) &&
  (pathname === item.to ||
    pathname.startsWith(`${item.to}/`) ||
    (item.also ?? []).some((path) => pathname.startsWith(path)));
