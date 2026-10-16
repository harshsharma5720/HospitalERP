import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarDays, CalendarOff, ChevronRight, Clock, ListOrdered } from "lucide-react";
import useAuthStore from "../../auth/useAuthStore";
import { specialityLabel } from "../../appointments/appointmentFormat";
import { getDoctor } from "../api";
import DoctorAnalytics from "../analytics/DoctorAnalytics";
import { Button, PageHeader } from "../../../shared/ui";

// The doctor's home: the way to the day's work, and their visits and earnings in numbers
// (docs/DOCTOR_ANALYTICS_PLAN.md)

const QUICK_LINKS = [
  { to: "/doctor/queue", icon: ListOrdered, label: "Today's queue", hint: "Call patients in as they arrive" },
  { to: "/doctor/doctor-appointments", icon: CalendarDays, label: "Appointments", hint: "Pending and completed visits" },
  { to: "/doctor/schedule", icon: Clock, label: "Schedule", hint: "Your weekly working hours" },
  { to: "/doctor/leave-management", icon: CalendarOff, label: "Leave", hint: "Ask for days off" },
];

export default function DoctorDashboard() {
  const userId = useAuthStore((s) => s.userId);
  const [doctor, setDoctor] = useState(null);

  useEffect(() => {
    let current = true;
    getDoctor(userId)
      .then((res) => current && setDoctor(res.data))
      .catch(() => current && setDoctor({}));
    return () => {
      current = false;
    };
  }, [userId]);

  return (
    <>
      <PageHeader
        title={doctor?.name ? `Hello, Dr. ${doctor.name}` : "Hello"}
        description={specialityLabel(doctor?.specialist) || null}
        actions={
          <Button as={Link} to="/doctor/queue">
            <ListOrdered size={16} aria-hidden="true" />
            Today's queue
          </Button>
        }
      />

      <nav aria-label="Quick links" className="mb-8">
        <ul className="grid grid-cols-2 gap-3 xl:grid-cols-4">
          {QUICK_LINKS.map(({ to, icon: Icon, label, hint }) => (
            <li key={to}>
              <Link
                to={to}
                className="group flex h-full items-center gap-3 rounded-xl border border-line bg-surface p-4 shadow-card hover:bg-surface-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
              >
                <span className="rounded-lg bg-primary-ink/10 p-2 text-primary-ink">
                  <Icon size={18} aria-hidden="true" />
                </span>
                <span className="flex-1">
                  <span className="block text-sm font-medium">{label}</span>
                  <span className="hidden text-xs text-ink-muted sm:block">{hint}</span>
                </span>
                <ChevronRight size={16} aria-hidden="true" className="text-ink-muted group-hover:text-ink" />
              </Link>
            </li>
          ))}
        </ul>
      </nav>

      <DoctorAnalytics />
    </>
  );
}
