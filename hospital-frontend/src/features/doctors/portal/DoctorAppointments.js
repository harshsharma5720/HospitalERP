import React, { useEffect, useState } from "react";
import { CalendarDays } from "lucide-react";
import useAuthStore from "../../auth/useAuthStore";
import { getErrorMessage } from "../../../shared/utils/apiError";
import * as appointmentsApi from "../../appointments/api";
import { formatDate, timeRange } from "../../appointments/appointmentFormat";
import ConsultationModal from "../../clinical/ConsultationModal";
import { downloadPrescription } from "../../clinical/downloadPrescription";
import { Button, Card, EmptyState, PageHeader, Table, Td, Th, useConfirm, useToast } from "../../../shared/ui";

// The doctor's appointments: pending ones (start the consultation, or complete without notes) and completed ones
// (their notes and the prescription PDF).
const VIEWS = [
  ["pending", "Pending"],
  ["completed", "Completed"],
];

export default function DoctorAppointments() {
  const userId = useAuthStore((s) => s.userId);
  const toast = useToast();
  const confirm = useConfirm();
  const [view, setView] = useState("pending");
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [consulting, setConsulting] = useState(null); // appointment open in the consultation form

  useEffect(() => {
    let current = true;
    setLoading(true);
    const load = view === "pending" ? appointmentsApi.getDoctorPendingAppointments : appointmentsApi.getDoctorCompletedAppointments;
    load(userId)
      .then((response) => current && setAppointments(response.data))
      .catch((err) => {
        if (!current) return;
        setAppointments([]);
        toast.error(getErrorMessage(err, "Could not load your appointments."));
      })
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [userId, view, toast]);

  const markCompleted = async (appointment) => {
    const yes = await confirm({
      title: `Complete the visit of ${appointment.patientName}?`,
      message: "No consultation notes are saved with it.",
      confirmLabel: "Complete visit",
    });
    if (!yes) return;
    try {
      const response = await appointmentsApi.completeAppointment(appointment.appointmentID);
      setAppointments((prev) => prev.filter((a) => a.appointmentID !== appointment.appointmentID));
      toast.success(typeof response.data === "string" && response.data ? response.data : "Visit completed.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to update appointment status."));
    }
  };

  // After a consultation is saved the appointment is completed, so it leaves the pending list
  const consultationSaved = (appointmentID) => {
    if (view === "pending") {
      setAppointments((prev) => prev.filter((a) => a.appointmentID !== appointmentID));
      setConsulting(null);
      toast.success("Consultation saved and appointment completed.");
    } else {
      toast.success("Consultation updated.");
    }
  };

  return (
    <>
      <PageHeader
        title="Appointments"
        description="Start the consultation from a pending visit; completed visits keep their notes and prescription."
      />

      <div role="group" aria-label="Which appointments" className="mb-4 inline-flex rounded-lg border border-line bg-surface p-1 shadow-card">
        {VIEWS.map(([value, label]) => (
          <button
            key={value}
            type="button"
            aria-pressed={view === value}
            onClick={() => setView(value)}
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
              view === value ? "bg-primary text-white" : "text-ink-muted hover:text-ink"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-ink-muted">Loading appointments...</p>
      ) : appointments.length === 0 ? (
        <Card padding="none">
          <EmptyState icon={CalendarDays} title={view === "pending" ? "No pending appointments" : "No completed appointments yet"}>
            {view === "pending" ? "New bookings appear here." : "Visits you complete appear here with their notes."}
          </EmptyState>
        </Card>
      ) : (
        <Table>
          <thead>
            <tr>
              <Th>Date</Th>
              <Th>Time</Th>
              <Th>Patient</Th>
              <Th>Reason</Th>
              <Th align="right">Actions</Th>
            </tr>
          </thead>
          <tbody>
            {appointments.map((appt) => (
              <tr key={appt.appointmentID}>
                <Td className="whitespace-nowrap">{appt.date ? formatDate(appt.date, true) : "—"}</Td>
                <Td className="whitespace-nowrap tabular-nums">{timeRange(appt)}</Td>
                <Td>
                  <span className="font-medium">{appt.patientName}</span>
                  {appt.age ? <span className="text-ink-muted"> · {appt.age} y</span> : null}
                </Td>
                <Td className="max-w-xs truncate text-ink-muted" title={appt.message ?? ""}>
                  {appt.message || "—"}
                </Td>
                <Td align="right">
                  <div className="flex justify-end gap-2">
                    {view === "pending" ? (
                      <>
                        <Button size="sm" variant="ghost" onClick={() => markCompleted(appt)} aria-label={`Complete without notes: ${appt.patientName}`}>
                          Complete without notes
                        </Button>
                        <Button size="sm" variant="success" onClick={() => setConsulting(appt)} aria-label={`Start consultation with ${appt.patientName}`}>
                          Start consultation
                        </Button>
                      </>
                    ) : (
                      <>
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => downloadPrescription(appt.appointmentID, toast.error)}
                          aria-label={`Prescription PDF of ${appt.patientName}`}
                        >
                          Prescription PDF
                        </Button>
                        <Button size="sm" onClick={() => setConsulting(appt)} aria-label={`View / edit notes of ${appt.patientName}`}>
                          View / edit notes
                        </Button>
                      </>
                    )}
                  </div>
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      {consulting && <ConsultationModal appointment={consulting} onClose={() => setConsulting(null)} onSaved={consultationSaved} />}
    </>
  );
}
