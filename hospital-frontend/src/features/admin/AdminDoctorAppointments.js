import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { CalendarDays } from "lucide-react";
import { getDoctor } from "../doctors/api";
import * as adminApi from "./api";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatDate, specialityLabel, timeRange, visitStatus } from "../appointments/appointmentFormat";
import { Badge, Card, EmptyState, PageHeader, Table, Td, Th, useToast } from "../../shared/ui";

const VIEWS = [
  ["pending", "Upcoming"],
  ["completed", "Completed"],
];

// Admin view of one doctor's pending / completed appointments (route: /admin/doctor/:userId/appointments)
export default function AdminDoctorAppointments() {
  const { userId } = useParams();
  const toast = useToast();
  const [viewType, setViewType] = useState("pending");
  const [appointments, setAppointments] = useState([]);
  const [doctor, setDoctor] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getDoctor(userId)
      .then((res) => setDoctor(res.data))
      .catch(() => setDoctor(null));
  }, [userId]);

  useEffect(() => {
    let current = true;
    setLoading(true);
    const load = viewType === "pending" ? adminApi.getDoctorPendingAppointments : adminApi.getDoctorCompletedAppointments;
    load(userId)
      .then((res) => current && setAppointments(res.data))
      .catch((err) => {
        if (!current) return;
        setAppointments([]);
        toast.error(getErrorMessage(err, "Could not load appointments."));
      })
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [userId, viewType, toast]);

  return (
    <>
      <PageHeader
        back={
          <Link to="/admin/manage-doctor" className="text-primary-ink hover:underline">
            ← Doctors
          </Link>
        }
        title={doctor ? `Appointments of Dr. ${doctor.name}` : "Appointments"}
        description={doctor ? specialityLabel(doctor.specialist) || null : null}
      />

      <div role="group" aria-label="Which appointments" className="mb-4 inline-flex rounded-lg border border-line bg-surface p-1 shadow-card">
        {VIEWS.map(([value, label]) => (
          <button
            key={value}
            type="button"
            aria-pressed={viewType === value}
            onClick={() => setViewType(value)}
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
              viewType === value ? "bg-primary text-white" : "text-ink-muted hover:text-ink"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-ink-muted">Loading...</p>
      ) : appointments.length === 0 ? (
        <Card padding="none">
          <EmptyState icon={CalendarDays} title={viewType === "pending" ? "No upcoming appointments" : "No completed appointments"} />
        </Card>
      ) : (
        <Table>
          <thead>
            <tr>
              <Th>ID</Th>
              <Th>Date</Th>
              <Th>Time</Th>
              <Th>Patient</Th>
              <Th>Status</Th>
              <Th>Reason</Th>
            </tr>
          </thead>
          <tbody>
            {appointments.map((a) => {
              const { label, tone } = visitStatus(a);
              return (
                <tr key={a.appointmentID}>
                  <Td className="tabular-nums text-ink-muted">#{a.appointmentID}</Td>
                  <Td className="whitespace-nowrap">{a.date ? formatDate(a.date, true) : "—"}</Td>
                  <Td className="whitespace-nowrap tabular-nums">{a.startTime ? timeRange(a) : a.shift || "—"}</Td>
                  <Td className="font-medium">{a.patientName}</Td>
                  <Td>
                    <Badge tone={tone}>{label}</Badge>
                  </Td>
                  <Td className="max-w-xs truncate text-ink-muted" title={a.message ?? ""}>
                    {a.message || "—"}
                  </Td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}
    </>
  );
}
