import React, { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CalendarPlus, ChevronRight, FileDown, Receipt, WifiOff } from "lucide-react";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import { PageIntro, PublicPage } from "../../shared/layout/PublicSections";
import { Badge, Button, Card, EmptyState, useConfirm, useToast } from "../../shared/ui";
import { downloadPrescription } from "../clinical/downloadPrescription";
import { getMyHistory } from "../clinical/api";
import { getMyBills, getMyReceiptPdf } from "../billing/api";
import { BillStatusBadge } from "../billing/billFormat";
import { downloadReceipt } from "../billing/downloadReceipt";
import MyBillDialog from "../billing/MyBillDialog";
import useOnlinePayment from "../billing/useOnlinePayment";
import { HOSPITAL, phoneHref } from "../../shared/hospitalInfo";
import { deadlineText, formatDate, timeRange, visitStatus } from "./appointmentFormat";
import * as appointmentsApi from "./api";

// The patient's appointments (docs/PATIENT_AREA_REFRESH_PLAN.md, P.3): upcoming ones to move or cancel, past ones
// with the doctor's notes, the prescription and Book again; a checked-in visit's bill (docs/PATIENT_BILLS_PLAN.md)

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

// Compares dates only, so an appointment later today is still upcoming
const isUpcoming = (a, today) => (a.status === "SCHEDULED" || a.status === "CONFIRMED") && a.date >= today;
const when = (a) => `${a.date} ${a.startTime ?? ""}`;
// Patients can't cancel or move a visit from its deadline on (docs/CANCELLATION_CUTOFF_PLAN.md); the server sends it
// as changeableUntil (none: no cut-off). The cut-off itself is the time from the deadline to the visit's start.
const pastDeadline = (a, now = new Date()) => Boolean(a.changeableUntil) && new Date(a.changeableUntil) <= now;
const cutoffText = (a) => {
  const hours = Math.round((new Date(`${a.date}T${a.startTime}`) - new Date(a.changeableUntil)) / 3600000);
  return hours === 1 ? "1 hour" : `${hours} hours`;
};

// Bills by the visit (appointment id) they are for
const byVisit = (bills) => Object.fromEntries(bills.map((b) => [b.appointmentId, b]));

function DateBlock({ date }) {
  const [year, month, day] = date.split("-").map(Number);
  return (
    <div aria-hidden="true" className="flex h-16 w-16 shrink-0 flex-col items-center justify-center rounded-xl bg-primary-ink/10 text-primary-ink">
      <span className="text-xs font-medium uppercase">{WEEKDAYS[new Date(year, month - 1, day).getDay()]}</span>
      <span className="text-xl font-semibold leading-none">{day}</span>
      <span className="text-xs">{MONTHS[month - 1]}</span>
    </div>
  );
}

// Diagnosis, medicines and follow-up from the doctor's consultation
function ConsultationSummary({ consultation }) {
  const [open, setOpen] = useState(false);
  const extras = consultation.medicines.length > 0 || consultation.symptoms || consultation.notes;
  return (
    <div className="mt-4 rounded-lg bg-surface-2 p-4 text-sm">
      <p>
        <span className="font-medium">Diagnosis:</span> {consultation.diagnosis}
      </p>
      {consultation.followUpDate && (
        <p className="mt-1">
          <span className="font-medium">Follow-up:</span> {formatDate(consultation.followUpDate, true)}
        </p>
      )}
      {extras && (
        <button
          type="button"
          onClick={() => setOpen(!open)}
          aria-expanded={open}
          className="mt-2 inline-flex items-center gap-1 font-medium text-primary-ink hover:underline"
        >
          <ChevronRight size={16} aria-hidden="true" className={`transition-transform ${open ? "rotate-90" : ""}`} />
          {open ? "Hide the details" : `The doctor's notes (${consultation.medicines.length} medicines)`}
        </button>
      )}
      {open && (
        <div className="mt-3 space-y-2">
          {consultation.symptoms && (
            <p>
              <span className="font-medium">Symptoms:</span> {consultation.symptoms}
            </p>
          )}
          {consultation.medicines.length > 0 && (
            <ul className="list-disc space-y-1 pl-5">
              {consultation.medicines.map((m, i) => (
                <li key={i}>
                  <span className="font-medium">{m.medicineName}</span>
                  {[m.dosage, m.frequency, m.duration, m.instructions].filter(Boolean).length > 0 &&
                    ` — ${[m.dosage, m.frequency, m.duration, m.instructions].filter(Boolean).join(", ")}`}
                </li>
              ))}
            </ul>
          )}
          {consultation.notes && (
            <p>
              <span className="font-medium">Advice:</span> {consultation.notes}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function AppointmentCard({ appointment, upcoming, consultation, bill, onReschedule, onCancel, onBookAgain, onPrescription, onBill }) {
  const { label, tone } = visitStatus(appointment);
  const locked = upcoming && pastDeadline(appointment);
  return (
    <article aria-label={`${formatDate(appointment.date, true)} with Dr. ${appointment.doctorName}`} className="rounded-2xl border border-line bg-surface p-5 shadow-card">
      <div className="flex gap-4">
        <DateBlock date={appointment.date} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="font-semibold">Dr. {appointment.doctorName || "—"}</p>
              <p className="text-sm text-ink-muted">
                {timeRange(appointment)} · for {appointment.patientName}
              </p>
            </div>
            <Badge tone={tone}>{label}</Badge>
          </div>
          {appointment.message && <p className="mt-2 text-sm text-ink-muted">“{appointment.message}”</p>}
          {upcoming && !locked && appointment.changeableUntil && (
            <p className="mt-2 text-xs text-ink-muted">You can cancel or move it until {deadlineText(appointment.changeableUntil)}.</p>
          )}
        </div>
      </div>

      {locked && (
        <p className="mt-4 rounded-lg bg-surface-2 p-3 text-sm">
          Less than {cutoffText(appointment)} to go: to cancel or move it, please call{" "}
          <a href={phoneHref} className="font-medium text-primary-ink hover:underline">
            {HOSPITAL.phone}
          </a>
          .
        </p>
      )}

      {consultation && <ConsultationSummary consultation={consultation} />}

      {(bill || !locked) && (
        <div className="mt-4 flex flex-wrap items-center justify-end gap-2 border-t border-line pt-4">
          {bill && (
            <>
              <p className="mr-auto flex w-full items-center gap-2 text-sm sm:w-auto">
                <span className="text-ink-muted">Bill</span>
                <span className="font-medium tabular-nums">{formatRupees(bill.total)}</span>
                <BillStatusBadge status={bill.status} />
              </p>
              <Button size="sm" variant="secondary" onClick={onBill} aria-label={`Bill ${bill.billNumber}`}>
                <Receipt size={16} aria-hidden="true" />
                Bill
              </Button>
            </>
          )}
          {upcoming ? (
            !locked && (
              <>
                <Button size="sm" variant="danger-ghost" onClick={onCancel}>
                  Cancel
                </Button>
                <Button size="sm" variant="secondary" onClick={onReschedule}>
                  Reschedule
                </Button>
              </>
            )
          ) : (
            <>
              {consultation && (
                <Button size="sm" variant="secondary" onClick={onPrescription}>
                  <FileDown size={16} aria-hidden="true" />
                  Prescription
                </Button>
              )}
              <Button size="sm" onClick={onBookAgain}>
                Book again
              </Button>
            </>
          )}
        </div>
      )}
    </article>
  );
}

export default function AppointmentDetails() {
  const navigate = useNavigate();
  const toast = useToast();
  const confirm = useConfirm();
  const [appointments, setAppointments] = useState(null);
  const [error, setError] = useState("");
  // Consultation notes by appointment id (only completed visits have one)
  const [consultations, setConsultations] = useState({});
  // Bills by appointment id (made at check-in), and the one shown
  const [bills, setBills] = useState({});
  const [openBill, setOpenBill] = useState(null);
  // After an online payment: the paid bill in place, or all bills again when it couldn't be applied
  const payment = useOnlinePayment({
    onPaid: (paid) => {
      setBills((map) => ({ ...map, [paid.appointmentId]: paid }));
      setOpenBill((open) => (open?.id === paid.id ? paid : open));
    },
    onStale: () =>
      getMyBills()
        .then((res) => {
          setBills(byVisit(res.data));
          setOpenBill((open) => (open && res.data.find((b) => b.id === open.id)) || open);
        })
        .catch(() => {}),
  });
  const [tab, setTab] = useState("upcoming");

  const load = useCallback(async () => {
    setError("");
    setAppointments(null);
    try {
      const response = await appointmentsApi.getMyAppointments();
      setAppointments(response.data);
      // Notes and bills are extras: the appointments show without them
      const [history, myBills] = await Promise.allSettled([getMyHistory(), getMyBills()]);
      setConsultations(history.status === "fulfilled" ? Object.fromEntries(history.value.data.map((c) => [c.appointmentId, c])) : {});
      setBills(myBills.status === "fulfilled" ? byVisit(myBills.value.data) : {});
    } catch (err) {
      setError(getErrorMessage(err, "Could not load your appointments."));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const cancel = async (appointment) => {
    const ok = await confirm({
      title: "Cancel this appointment?",
      message: `${formatDate(appointment.date, true)}, ${timeRange(appointment)} with Dr. ${appointment.doctorName}. The time is freed for someone else.`,
      confirmLabel: "Cancel appointment",
      cancelLabel: "Keep it",
      tone: "danger",
    });
    if (!ok) return;
    try {
      const response = await appointmentsApi.cancelAppointment(appointment.appointmentID);
      // The appointment stays in the history with a "Cancelled" status
      setAppointments((prev) => prev.map((a) => (a.appointmentID === appointment.appointmentID ? { ...a, status: "CANCELLED_BY_PATIENT" } : a)));
      toast.success(typeof response.data === "string" && response.data ? response.data : "Appointment cancelled.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to cancel the appointment. Please try again."));
    }
  };

  const today = toLocalISODate();
  const upcoming = (appointments ?? []).filter((a) => isUpcoming(a, today)).sort((a, b) => when(a).localeCompare(when(b)));
  const past = (appointments ?? []).filter((a) => !isUpcoming(a, today)).sort((a, b) => when(b).localeCompare(when(a)));
  const shown = tab === "upcoming" ? upcoming : past;

  return (
    <PublicPage>
      <PageIntro
        eyebrow="My appointments"
        title="Your appointments"
        actions={
          <Button as={Link} to="/appointments" size="lg">
            <CalendarPlus size={18} aria-hidden="true" />
            Book an appointment
          </Button>
        }
      >
        Move or cancel an upcoming visit, or see the doctor's notes, prescription and bill of a past one.
      </PageIntro>

      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="max-w-4xl">
        <div role="group" aria-label="Which appointments" className="mb-6 inline-flex rounded-lg border border-line bg-surface p-1 shadow-card">
          {[
            ["upcoming", "Upcoming", upcoming.length],
            ["past", "Past", past.length],
          ].map(([value, label, count]) => (
            <button
              key={value}
              type="button"
              aria-pressed={tab === value}
              onClick={() => setTab(value)}
              className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                tab === value ? "bg-primary text-white" : "text-ink-muted hover:text-ink"
              }`}
            >
              {label}
              {appointments && ` (${count})`}
            </button>
          ))}
        </div>

        {!appointments && !error && (
          <div aria-busy="true" className="space-y-4">
            <span className="sr-only">Loading your appointments...</span>
            {[0, 1].map((i) => (
              <div key={i} aria-hidden="true" className="h-36 animate-pulse rounded-2xl border border-line bg-surface" />
            ))}
          </div>
        )}

        {error && (
          <Card padding="none">
            <EmptyState
              icon={WifiOff}
              title="We couldn't load your appointments"
              action={
                <Button variant="secondary" onClick={load}>
                  Try again
                </Button>
              }
            >
              {error}
            </EmptyState>
          </Card>
        )}

        {appointments && shown.length === 0 && (
          <Card padding="none">
            <EmptyState
              icon={CalendarPlus}
              title={tab === "upcoming" ? "No upcoming appointments" : "No past appointments yet"}
              action={
                tab === "upcoming" && (
                  <Button as={Link} to="/appointments">
                    Book an appointment
                  </Button>
                )
              }
            >
              {tab === "upcoming" ? "Book a visit and it will show here." : "Visits you've had will show here with the doctor's notes."}
            </EmptyState>
          </Card>
        )}

        {shown.length > 0 && (
          <div className="space-y-4">
            {shown.map((appointment) => (
              <AppointmentCard
                key={appointment.appointmentID}
                appointment={appointment}
                upcoming={tab === "upcoming"}
                consultation={consultations[appointment.appointmentID]}
                bill={bills[appointment.appointmentID]}
                onCancel={() => cancel(appointment)}
                onReschedule={() => navigate("/appointments", { state: { rescheduleAppointment: appointment } })}
                onBookAgain={() => navigate("/appointments", { state: { doctorName: appointment.doctorName, doctorId: appointment.doctorId } })}
                onPrescription={() => downloadPrescription(appointment.appointmentID, toast.error)}
                onBill={() => setOpenBill(bills[appointment.appointmentID])}
              />
            ))}
          </div>
        )}
        {openBill && (
          <MyBillDialog
            bill={openBill}
            onClose={() => setOpenBill(null)}
            onReceipt={(bill) => downloadReceipt(() => getMyReceiptPdf(bill.id), bill.billNumber, toast.error)}
            onPay={payment.pay}
            paying={payment.paying === openBill.id}
            error={payment.errorFor(openBill.id)}
          />
        )}
        </div>
      </div>
    </PublicPage>
  );
}
