import React, { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import useAreaBase from "../../shared/useAreaBase";
import { getAllDoctors } from "../doctors/api";
import { dayLabel, doctorLabel, formatDate, timeRange, visitStatus } from "./appointmentFormat";
import { checkInAppointment, getFrontDeskAppointmentPage, undoCheckIn } from "./api";

// The front desk's appointment list (docs/PATIENT_SEARCH_PLAN.md): today by default; dates, doctor, status and
// patient filters; 20 per page, by date and time. The filters live in the address, so coming back from a patient
// shows the same list. Receptionists: /receptionist-appointments, admins: /admin/appointments.
// Today's queue (docs/TODAYS_QUEUE_PLAN.md): Check in today's patients as they arrive (they get their token); the
// list refreshes itself, so it shows who the doctors have called in.

const PAGE_SIZE = 20;
const REFRESH_MS = 15000;
const STATUS_OPTIONS = [
  ["all", "All"],
  ["upcoming", "Upcoming"],
  ["completed", "Completed"],
  ["cancelled", "Cancelled"],
];
const INPUT =
  "h-10 px-2 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2]";
const QUIET_BUTTON = "border dark:border-gray-600 rounded";

// "Today", "Tomorrow", "Fri 9 Oct" this year, "Tue 8 Oct 2025" other years
const dateLabel = (isoDate) =>
  isoDate.startsWith(String(new Date().getFullYear())) ? dayLabel(isoDate) : formatDate(isoDate, true);

// Today's still-booked appointments: Check in, or where the patient is in the queue
function QueueCell({ appointment, today, busy, onCheckIn, onUndo }) {
  const booked = appointment.status === "SCHEDULED" || appointment.status === "CONFIRMED";
  if (appointment.date !== today || !booked) return "—";
  if (appointment.calledAt) return `With the doctor · token ${appointment.tokenNumber}`;
  if (appointment.checkedInAt) {
    return (
      <span className="flex items-center gap-2">
        Waiting · token {appointment.tokenNumber}
        <button
          type="button"
          onClick={onUndo}
          disabled={busy}
          aria-label={`Undo the check-in of ${appointment.patientName}`}
          className="text-xs underline text-gray-600 dark:text-gray-300 disabled:opacity-50"
        >
          Undo
        </button>
      </span>
    );
  }
  return (
    <button
      type="button"
      onClick={onCheckIn}
      disabled={busy}
      aria-label={`Check in ${appointment.patientName}`}
      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs disabled:opacity-50"
    >
      Check in
    </button>
  );
}

export default function FrontDeskAppointments() {
  const base = useAreaBase();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const today = toLocalISODate();
  // No dates in the address: today. Empty dates ("from=&to="): any date.
  const from = params.has("from") ? params.get("from") : today;
  const to = params.has("to") ? params.get("to") : today;
  const doctorId = params.get("doctorId") ?? "";
  const status = params.get("status") ?? "all";
  const patient = params.get("patient") ?? "";
  const page = Math.max(0, Number(params.get("page")) || 0);

  const [draft, setDraft] = useState({ from, to, doctorId, status, patient });
  const [doctors, setDoctors] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => setDraft({ from, to, doctorId, status, patient }), [from, to, doctorId, status, patient]);

  useEffect(() => {
    getAllDoctors()
      .then((response) => setDoctors(response.data))
      .catch(() => setDoctors([]));
  }, []);

  // Loads the page, then quietly again every few seconds (no "Loading...", a failed refresh keeps the list)
  useEffect(() => {
    let current = true;
    const request = { page, size: PAGE_SIZE };
    if (from) request.from = from;
    if (to) request.to = to;
    if (doctorId) request.doctorId = doctorId;
    if (status !== "all") request.status = status;
    if (patient) request.patient = patient;
    const load = (quietly) => {
      if (!quietly) {
        setLoading(true);
        setError("");
      }
      getFrontDeskAppointmentPage(request)
        .then((response) => current && setResult(response.data))
        .catch((err) => {
          if (!current || quietly) return;
          setResult(null);
          setError(getErrorMessage(err, "Could not load the appointments."));
        })
        .finally(() => current && !quietly && setLoading(false));
    };
    load(false);
    const timer = setInterval(() => load(true), REFRESH_MS);
    return () => {
      current = false;
      clearInterval(timer);
    };
  }, [from, to, doctorId, status, patient, page]);

  const replaceRow = (updated) =>
    setResult((loaded) => ({
      ...loaded,
      entries: loaded.entries.map((a) => (a.appointmentID === updated.appointmentID ? updated : a)),
    }));

  const queueAction = async (appointment, action) => {
    setError("");
    setNotice("");
    setBusyId(appointment.appointmentID);
    try {
      const response = await action(appointment.appointmentID);
      replaceRow(response.data);
      setNotice(
        response.data.tokenNumber
          ? `${appointment.patientName} is checked in: token ${response.data.tokenNumber}.`
          : `Check-in of ${appointment.patientName} undone.`
      );
    } catch (err) {
      setError(getErrorMessage(err, "Could not change the check-in."));
    } finally {
      setBusyId(null);
    }
  };

  // The dates are always written, so cleared dates ("any date") are kept too
  const show = (filters, pageNumber = 0) => {
    const next = { from: filters.from, to: filters.to };
    if (filters.doctorId) next.doctorId = filters.doctorId;
    if (filters.status !== "all") next.status = filters.status;
    if (filters.patient.trim()) next.patient = filters.patient.trim();
    if (pageNumber > 0) next.page = String(pageNumber);
    setParams(next);
  };

  const change = (field) => (e) => setDraft({ ...draft, [field]: e.target.value });
  const apply = (e) => {
    e.preventDefault();
    show(draft);
  };
  const filters = { from, to, doctorId, status, patient };
  const entries = result?.entries ?? [];
  const totalPages = result?.totalPages ?? 0;
  const back = location.pathname + location.search; // the patient page links back here

  return (
    <div className="p-6">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h1 className="text-3xl font-bold">Appointments</h1>
        <Link to={`${base}/walk-in`} className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded">
          Walk-in booking
        </Link>
      </div>

      <form onSubmit={apply} aria-label="Filters" className="flex flex-wrap items-end gap-3 mb-4">
        <div className="flex flex-col text-sm">
          <label htmlFor="appointments-from">From</label>
          <input id="appointments-from" type="date" value={draft.from} onChange={change("from")} className={INPUT} />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="appointments-to">To</label>
          <input id="appointments-to" type="date" value={draft.to} onChange={change("to")} className={INPUT} />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="appointments-doctor">Doctor</label>
          <select id="appointments-doctor" value={draft.doctorId} onChange={change("doctorId")} className={INPUT}>
            <option value="">All doctors</option>
            {doctors.map((doctor) => (
              <option key={doctor.id} value={doctor.id}>
                {doctorLabel(doctor)}
                {doctor.active === false ? " (inactive)" : ""}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="appointments-status">Status</label>
          <select id="appointments-status" value={draft.status} onChange={change("status")} className={INPUT}>
            {STATUS_OPTIONS.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="appointments-patient">Patient</label>
          <input
            id="appointments-patient"
            value={draft.patient}
            onChange={change("patient")}
            placeholder="Part of the name"
            className={INPUT}
          />
        </div>
        <button type="submit" className="h-10 bg-blue-600 hover:bg-blue-700 text-white px-4 rounded">
          Show
        </button>
        <button type="button" onClick={() => show({ ...filters, from: today, to: today })} className={`${QUIET_BUTTON} h-10 px-4`}>
          Today
        </button>
        <button type="button" onClick={() => show({ ...filters, from: "", to: "" })} className={`${QUIET_BUTTON} h-10 px-4`}>
          Any date
        </button>
      </form>

      {notice && (
        <p role="status" className="text-green-700 dark:text-green-400 mb-4">
          {notice}
        </p>
      )}
      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full border-collapse bg-white dark:bg-[#1e293b] shadow-md rounded-lg text-sm">
          <thead>
            <tr className="bg-gray-100 dark:bg-gray-800 border-b dark:border-gray-600">
              <th className="p-3 text-left">Date</th>
              <th className="p-3 text-left">Time</th>
              <th className="p-3 text-left">Patient</th>
              <th className="p-3 text-left">Doctor</th>
              <th className="p-3 text-left">Status</th>
              <th className="p-3 text-left">Queue</th>
              <th className="p-3 text-left">Reason</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((appointment) => {
              const { label, style } = visitStatus(appointment, today);
              return (
                <tr key={appointment.appointmentID} className="border-b dark:border-gray-700">
                  <td className="p-3 whitespace-nowrap">{dateLabel(appointment.date)}</td>
                  <td className="p-3 whitespace-nowrap">{timeRange(appointment)}</td>
                  <td className="p-3">
                    {appointment.ptInfoId ? (
                      <Link
                        to={`${base}/patients/${appointment.ptInfoId}`}
                        state={{ from: back, fromLabel: "Appointments" }}
                        className="font-semibold text-blue-700 dark:text-[#50d4f2] hover:underline"
                      >
                        {appointment.patientName}
                      </Link>
                    ) : (
                      appointment.patientName
                    )}
                  </td>
                  <td className="p-3">Dr. {appointment.doctorName}</td>
                  <td className="p-3">
                    <span className={`px-2 py-1 rounded-full text-xs font-semibold ${style}`}>{label}</span>
                  </td>
                  <td className="p-3 whitespace-nowrap">
                    <QueueCell
                      appointment={appointment}
                      today={today}
                      busy={busyId === appointment.appointmentID}
                      onCheckIn={() => queueAction(appointment, checkInAppointment)}
                      onUndo={() => queueAction(appointment, undoCheckIn)}
                    />
                  </td>
                  <td className="p-3 max-w-xs truncate" title={appointment.message ?? ""}>
                    {appointment.message || "—"}
                  </td>
                </tr>
              );
            })}
            {!loading && !error && entries.length === 0 && (
              <tr>
                <td colSpan={7} className="p-6 text-center text-gray-500">
                  No appointments for these filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-3 mt-4">
        <button
          type="button"
          onClick={() => show(filters, page - 1)}
          disabled={loading || page === 0}
          className={`${QUIET_BUTTON} px-3 py-1 disabled:opacity-50`}
        >
          Previous
        </button>
        <span className="text-sm text-gray-700 dark:text-gray-300">
          {loading
            ? "Loading..."
            : `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} · ${result?.totalEntries ?? 0} appointments`}
        </span>
        <button
          type="button"
          onClick={() => show(filters, page + 1)}
          disabled={loading || page + 1 >= totalPages}
          className={`${QUIET_BUTTON} px-3 py-1 disabled:opacity-50`}
        >
          Next
        </button>
      </div>
    </div>
  );
}
