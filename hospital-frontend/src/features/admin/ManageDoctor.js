import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Search } from "lucide-react";
import { getAllDoctors } from "../doctors/api";
import * as adminApi from "./api";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { formatSpecialization } from "../../shared/dashboard/dashboardFormat";
import { Badge, Button, Card, Input, PageHeader, Table, TableMessage, Td, Th, useConfirm, useToast } from "../../shared/ui";

// The doctors, ranked by completed appointments, who is on leave today, and deactivation for a doctor who has
// left (docs/ACCOUNT_DEACTIVATION_PLAN.md).
export default function ManageDoctor() {
  const toast = useToast();
  const confirm = useConfirm();
  const [doctors, setDoctors] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  // userIds of doctors with an approved leave covering today
  const [onLeaveUserIds, setOnLeaveUserIds] = useState(new Set());
  const [presenceFilter, setPresenceFilter] = useState(null); // null | "present" | "absent"

  const fetchDoctors = async () => {
    try {
      const response = await getAllDoctors();
      const doctorsWithCounts = await Promise.all(
        response.data.map(async (doc) => {
          if (!doc.userId) return doc;
          try {
            const countRes = await adminApi.getDoctorAppointmentCount(doc.id);
            return { ...doc, pendingAppointmentsCount: countRes.data.pending, completedAppointmentsCount: countRes.data.completed };
          } catch {
            return { ...doc, pendingAppointmentsCount: 0, completedAppointmentsCount: 0 };
          }
        })
      );
      doctorsWithCounts.sort((a, b) => (b.completedAppointmentsCount || 0) - (a.completedAppointmentsCount || 0));
      setDoctors(doctorsWithCounts);

      const today = toLocalISODate();
      const leavesRes = await adminApi.getApprovedLeaves();
      setOnLeaveUserIds(new Set(leavesRes.data.filter((l) => l.startDate <= today && l.endDate >= today).map((l) => l.userId)));
    } catch (error) {
      toast.error(getErrorMessage(error, "Could not load the doctors."));
    } finally {
      setLoading(false);
    }
  };

  // A deactivated doctor can't log in and disappears from booking; upcoming appointments are cancelled and the
  // patients notified. All history is kept.
  const deactivateDoctor = async (doc) => {
    const ok = await confirm({
      title: `Deactivate Dr. ${doc.name}?`,
      message:
        "For a doctor who has left. They can't log in any more and are hidden from booking; their upcoming appointments are cancelled and the patients notified. All history is kept, and you can reactivate the doctor later.",
      confirmLabel: "Deactivate",
      tone: "danger",
    });
    if (!ok) return;
    try {
      if (doc.userId) {
        await adminApi.deactivateUser(doc.userId);
      } else {
        await adminApi.deactivateDoctor(doc.id); // old doctor row without a login account
      }
      toast.success(`Dr. ${doc.name} deactivated.`);
      fetchDoctors();
    } catch (error) {
      toast.error(getErrorMessage(error, "Deactivation failed!"));
    }
  };

  const reactivateDoctor = async (doc) => {
    try {
      await adminApi.reactivateUser(doc.userId);
      toast.success(`Dr. ${doc.name} reactivated: bookable again.`);
      fetchDoctors();
    } catch (error) {
      toast.error(getErrorMessage(error, "Reactivation failed!"));
    }
  };

  useEffect(() => {
    fetchDoctors();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) return <p className="text-ink-muted">Loading doctors...</p>;

  const needle = search.trim().toLowerCase();
  const matching = doctors.filter((doc) => `${doc.name} ${doc.specialist} ${doc.userId}`.toLowerCase().includes(needle));
  // Present / on leave today only counts active doctors (a deactivated doctor is neither)
  const isAbsent = (d) => onLeaveUserIds.has(d.userId);
  const activeDoctors = matching.filter((d) => d.active !== false);
  const presentDoctors = activeDoctors.filter((d) => !isAbsent(d));
  const absentDoctors = activeDoctors.filter(isAbsent);
  const visibleDoctors = presenceFilter === "absent" ? absentDoctors : presenceFilter === "present" ? presentDoctors : matching;

  const filters = [
    [null, `All (${matching.length})`],
    ["present", `Working today (${presentDoctors.length})`],
    ["absent", `On leave today (${absentDoctors.length})`],
  ];

  return (
    <>
      <PageHeader title="Doctors" description="Ranked by completed appointments. Deactivate a doctor who has left; their history is kept." />

      <Card padding="sm" className="mb-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative min-w-[14rem] flex-1">
            <Search size={16} aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted" />
            <Input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Name, speciality or user ID"
              aria-label="Search doctors"
              className="pl-9"
            />
          </div>
          <div role="group" aria-label="Today" className="inline-flex rounded-lg border border-line bg-surface-2 p-1">
            {filters.map(([value, label]) => (
              <button
                key={label}
                type="button"
                aria-pressed={presenceFilter === value}
                onClick={() => setPresenceFilter(value)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                  presenceFilter === value ? "bg-surface text-ink shadow-sm" : "text-ink-muted hover:text-ink"
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      </Card>

      <Table>
        <thead>
          <tr>
            <Th>Rank</Th>
            <Th>Doctor</Th>
            <Th>Speciality</Th>
            <Th align="right">Completed</Th>
            <Th align="right">Pending</Th>
            <Th align="right">Actions</Th>
          </tr>
        </thead>
        <tbody>
          {visibleDoctors.map((doc, index) => (
            <tr key={doc.id} className={doc.active === false ? "text-ink-muted" : ""}>
              <Td className="font-semibold tabular-nums">#{index + 1}</Td>
              <Td>
                <span className="font-medium">Dr. {doc.name}</span>
                <span className="ml-2 text-xs text-ink-muted">#{doc.id}</span>
                {doc.active === false && (
                  <span className="ml-2">
                    <Badge>Deactivated</Badge>
                  </span>
                )}
                {doc.active !== false && isAbsent(doc) && (
                  <span className="ml-2">
                    <Badge tone="warning">On leave today</Badge>
                  </span>
                )}
              </Td>
              <Td className="text-ink-muted">{formatSpecialization(doc.specialist)}</Td>
              <Td align="right" className="tabular-nums">{doc.completedAppointmentsCount || 0}</Td>
              <Td align="right" className="tabular-nums">{doc.pendingAppointmentsCount || 0}</Td>
              <Td align="right">
                <div className="flex flex-wrap justify-end gap-2">
                  <Button as={Link} to={`/admin/doctor/${doc.id}/analytics`} size="sm" variant="secondary" aria-label={`Analytics of Dr. ${doc.name}`}>
                    Analytics
                  </Button>
                  {doc.userId && (
                    <Button as={Link} to={`/admin/doctor/${doc.userId}/appointments`} size="sm" variant="secondary" aria-label={`Appointments of Dr. ${doc.name}`}>
                      Appointments
                    </Button>
                  )}
                  {doc.active === false ? (
                    <Button size="sm" variant="secondary" onClick={() => reactivateDoctor(doc)} aria-label={`Reactivate Dr. ${doc.name}`}>
                      Reactivate
                    </Button>
                  ) : (
                    <Button size="sm" variant="danger-ghost" onClick={() => deactivateDoctor(doc)} aria-label={`Deactivate Dr. ${doc.name}`}>
                      Deactivate
                    </Button>
                  )}
                </div>
              </Td>
            </tr>
          ))}
          {visibleDoctors.length === 0 && <TableMessage colSpan={6}>No doctors match.</TableMessage>}
        </tbody>
      </Table>
    </>
  );
}
