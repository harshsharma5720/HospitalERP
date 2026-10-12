import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarCheck, CalendarDays, CalendarOff, ChevronRight, Clock, ListOrdered, Users } from "lucide-react";
import useAuthStore from "../../auth/useAuthStore";
import { specialityLabel } from "../../appointments/appointmentFormat";
import { getDoctor } from "../api";
import * as appointmentsApi from "../../appointments/api";
import { Button, Card, CardHeader, PageHeader } from "../../../shared/ui";

// The doctor's home: their appointments in numbers, how many are done, and the way to the day's work
const QUICK_LINKS = [
  { to: "/doctor/queue", icon: ListOrdered, label: "Today's queue", hint: "Call patients in as they arrive" },
  { to: "/doctor/doctor-appointments", icon: CalendarDays, label: "Appointments", hint: "Pending and completed visits" },
  { to: "/doctor/schedule", icon: Clock, label: "Schedule", hint: "Your weekly working hours" },
  { to: "/doctor/leave-management", icon: CalendarOff, label: "Leave", hint: "Ask for days off" },
];

// A list from the API, or an empty one when it fails (the dashboard still shows what it has)
const listOf = (request) => request.then((res) => res.data || []).catch(() => []);

export default function DoctorDashboard() {
  const userId = useAuthStore((s) => s.userId);
  const [doctor, setDoctor] = useState(null);
  const [counts, setCounts] = useState(null); // { total, completed, pending }

  useEffect(() => {
    let current = true;
    getDoctor(userId)
      .then((res) => current && setDoctor(res.data))
      .catch(() => current && setDoctor({}));
    Promise.all([
      listOf(appointmentsApi.getDoctorAppointments()),
      listOf(appointmentsApi.getDoctorCompletedAppointments(userId)),
      listOf(appointmentsApi.getDoctorPendingAppointments(userId)),
    ]).then(([all, completed, pending]) => {
      if (current) setCounts({ total: all.length, completed: completed.length, pending: pending.length });
    });
    return () => {
      current = false;
    };
  }, [userId]);

  if (!doctor || !counts) return <p className="text-ink-muted">Loading doctor dashboard...</p>;

  const handled = counts.completed + counts.pending;
  const share = handled === 0 ? 0 : Math.round((counts.completed / handled) * 100);

  return (
    <>
      <PageHeader
        title={doctor.name ? `Hello, Dr. ${doctor.name}` : "Hello"}
        description={specialityLabel(doctor.specialist) || null}
        actions={
          <Button as={Link} to="/doctor/queue">
            <ListOrdered size={16} aria-hidden="true" />
            Today's queue
          </Button>
        }
      />

      <div className="grid gap-5 sm:grid-cols-3">
        <Stat title="Total appointments" value={counts.total} icon={Users} />
        <Stat title="Completed" value={counts.completed} icon={CalendarCheck} />
        <Stat title="Pending" value={counts.pending} icon={Clock} />
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader title="Visits completed" description={`${counts.completed} of ${handled} visits assigned to you`} />
          <div
            role="img"
            aria-label={`${share}% of your visits are completed`}
            className="h-3 overflow-hidden rounded-full bg-primary-ink/15"
          >
            <div className="h-full rounded-full bg-success" style={{ width: `${share}%` }} />
          </div>
          <dl className="mt-4 flex flex-wrap gap-6 text-sm">
            <div className="flex items-center gap-2">
              <span aria-hidden="true" className="h-2.5 w-2.5 rounded-full bg-success" />
              <dt className="text-ink-muted">Completed</dt>
              <dd className="font-semibold tabular-nums">{counts.completed}</dd>
            </div>
            <div className="flex items-center gap-2">
              <span aria-hidden="true" className="h-2.5 w-2.5 rounded-full bg-primary-ink/30" />
              <dt className="text-ink-muted">Pending</dt>
              <dd className="font-semibold tabular-nums">{counts.pending}</dd>
            </div>
            <div className="flex items-center gap-2">
              <dt className="text-ink-muted">Done</dt>
              <dd className="font-semibold tabular-nums">{share}%</dd>
            </div>
          </dl>
        </Card>

        <Card padding="sm" className="lg:col-span-2">
          <nav aria-label="Quick links">
            <ul className="divide-y divide-line">
              {QUICK_LINKS.map(({ to, icon: Icon, label, hint }) => (
                <li key={to}>
                  <Link to={to} className="group flex items-center gap-3 rounded-lg px-2 py-3 hover:bg-surface-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40">
                    <span className="rounded-lg bg-primary-ink/10 p-2 text-primary-ink">
                      <Icon size={18} aria-hidden="true" />
                    </span>
                    <span className="flex-1">
                      <span className="block text-sm font-medium">{label}</span>
                      <span className="block text-xs text-ink-muted">{hint}</span>
                    </span>
                    <ChevronRight size={16} aria-hidden="true" className="text-ink-muted group-hover:text-ink" />
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </Card>
      </div>
    </>
  );
}

function Stat({ title, value, icon: Icon }) {
  return (
    <Card>
      <div className="flex items-center gap-4">
        <span className="rounded-xl bg-primary-ink/10 p-3 text-primary-ink">
          <Icon size={22} aria-hidden="true" />
        </span>
        <div>
          <p className="text-sm text-ink-muted">{title}</p>
          <p className="text-2xl font-semibold tabular-nums">{value}</p>
        </div>
      </div>
    </Card>
  );
}
