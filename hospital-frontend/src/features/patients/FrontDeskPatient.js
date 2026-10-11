import React, { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import useAreaBase from "../../shared/useAreaBase";
import { getFrontDeskAppointmentPage } from "../appointments/api";
import { dayLabel, formatDate, timeRange, visitStatus } from "../appointments/appointmentFormat";
import * as patientsApi from "./api";

// One patient at the front desk (docs/PATIENT_SEARCH_PLAN.md): details, upcoming appointments, every appointment
// newest first (10 per page), and Book -> Walk-in booking with this patient chosen. /patients/:patientId for
// receptionists, /admin/patients/:patientId for admins.

const HISTORY_PAGE_SIZE = 10;
const GENDER_LABELS = { FEMALE: "Female", MALE: "Male", OTHER: "Other" };
const CARD = "bg-white dark:bg-[#1e293b] shadow-md rounded-lg p-5 mb-5";

// Only the last 4 digits of the Aadhaar number are shown
const maskAadhaar = (number) => (number ? `XXXX XXXX ${String(number).slice(-4)}` : null);

function StatusBadge({ appointment }) {
  const { label, style } = visitStatus(appointment);
  return <span className={`px-2 py-1 rounded-full text-xs font-semibold ${style}`}>{label}</span>;
}

export default function FrontDeskPatient() {
  const { patientId } = useParams();
  const base = useAreaBase();
  const location = useLocation();
  const [patient, setPatient] = useState(null);
  const [error, setError] = useState("");
  const [upcoming, setUpcoming] = useState(null);
  const [history, setHistory] = useState(null);
  const [historyPage, setHistoryPage] = useState(0);

  useEffect(() => {
    setPatient(null);
    setError("");
    setUpcoming(null);
    setHistoryPage(0);
    patientsApi
      .getFrontDeskPatient(patientId)
      .then((response) => setPatient(response.data))
      .catch((err) => setError(getErrorMessage(err, "Could not load the patient.")));
    getFrontDeskAppointmentPage({ patientId, status: "upcoming", from: toLocalISODate(), size: 20 })
      .then((response) => setUpcoming(response.data.entries))
      .catch(() => setUpcoming([]));
  }, [patientId]);

  useEffect(() => {
    getFrontDeskAppointmentPage({ patientId, order: "desc", page: historyPage, size: HISTORY_PAGE_SIZE })
      .then((response) => setHistory(response.data))
      .catch(() => setHistory(null));
  }, [patientId, historyPage]);

  const backTo = location.state?.from || `${base}/patients`;

  if (error) {
    return (
      <div className="p-6">
        <Link to={backTo} className="text-sm text-blue-700 dark:text-[#50d4f2] hover:underline">
          ← Patients
        </Link>
        <p role="alert" className="text-red-600 mt-4">
          {error}
        </p>
      </div>
    );
  }
  if (!patient) {
    return <p className="p-6">Loading...</p>;
  }

  const details = [
    ["Gender", GENDER_LABELS[patient.gender]],
    ["Age", patient.dob ? `${calculateAgeFromDOB(patient.dob)} years (born ${formatDate(patient.dob, true)})` : null],
    ["Phone", patient.contactNo],
    ["Email", patient.email],
    ["Address", patient.patientAddress],
    ["Aadhaar", maskAadhaar(patient.patientAadharNo)],
  ];
  const historyEntries = history?.entries ?? [];
  const historyPages = history?.totalPages ?? 0;

  return (
    <div className="p-6 max-w-4xl">
      <Link to={backTo} className="text-sm text-blue-700 dark:text-[#50d4f2] hover:underline">
        ← Patients
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3 mt-2 mb-5">
        <div>
          <h1 className="text-3xl font-bold">{patient.patientName}</h1>
          <p className="text-gray-600 dark:text-gray-300">
            #{patient.patientId} · {patient.hasLogin ? "App account" : "Front-desk record"}
          </p>
        </div>
        <Link
          to={`${base}/walk-in?patientId=${patient.patientId}`}
          state={{ patient }}
          className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
        >
          Book an appointment
        </Link>
      </div>

      <section aria-labelledby="patient-details" className={CARD}>
        <h2 id="patient-details" className="font-semibold mb-3">
          Details
        </h2>
        <dl className="grid sm:grid-cols-2 gap-x-6 gap-y-2 text-sm">
          {details.map(([label, value]) => (
            <div key={label} className="flex gap-2">
              <dt className="text-gray-500 dark:text-gray-400 w-20 shrink-0">{label}</dt>
              <dd>{value || "—"}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section aria-labelledby="patient-upcoming" className={CARD}>
        <h2 id="patient-upcoming" className="font-semibold mb-3">
          Upcoming appointments
        </h2>
        {upcoming === null && <p className="text-sm">Loading...</p>}
        {upcoming?.length === 0 && <p className="text-sm text-gray-600 dark:text-gray-300">No upcoming appointments.</p>}
        {upcoming?.length > 0 && (
          <ul className="space-y-2 text-sm">
            {upcoming.map((appointment) => (
              <li key={appointment.appointmentID} className="flex flex-wrap items-center gap-x-3 gap-y-1">
                <span className="font-semibold">
                  {dayLabel(appointment.date)} {timeRange(appointment)}
                </span>
                <span>Dr. {appointment.doctorName}</span>
                {appointment.patientName !== patient.patientName && (
                  <span className="text-gray-600 dark:text-gray-300">for {appointment.patientName}</span>
                )}
                <StatusBadge appointment={appointment} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="patient-history" className={CARD}>
        <h2 id="patient-history" className="font-semibold mb-3">
          All appointments, newest first
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b dark:border-gray-600 text-left">
                <th className="py-2 pr-3">Date</th>
                <th className="py-2 pr-3">Time</th>
                <th className="py-2 pr-3">Doctor</th>
                <th className="py-2 pr-3">For</th>
                <th className="py-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {historyEntries.map((appointment) => (
                <tr key={appointment.appointmentID} className="border-b dark:border-gray-700">
                  <td className="py-2 pr-3 whitespace-nowrap">{formatDate(appointment.date, true)}</td>
                  <td className="py-2 pr-3 whitespace-nowrap">{timeRange(appointment)}</td>
                  <td className="py-2 pr-3">Dr. {appointment.doctorName}</td>
                  <td className="py-2 pr-3">{appointment.patientName}</td>
                  <td className="py-2">
                    <StatusBadge appointment={appointment} />
                  </td>
                </tr>
              ))}
              {history && historyEntries.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-4 text-center text-gray-500">
                    No appointments yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {historyPages > 1 && (
          <div className="flex items-center gap-3 mt-3">
            <button
              type="button"
              onClick={() => setHistoryPage(historyPage - 1)}
              disabled={historyPage === 0}
              className="border dark:border-gray-600 px-3 py-1 rounded disabled:opacity-50"
            >
              Newer
            </button>
            <span className="text-sm text-gray-700 dark:text-gray-300">
              Page {historyPage + 1} of {historyPages} · {history.totalEntries} appointments
            </span>
            <button
              type="button"
              onClick={() => setHistoryPage(historyPage + 1)}
              disabled={historyPage + 1 >= historyPages}
              className="border dark:border-gray-600 px-3 py-1 rounded disabled:opacity-50"
            >
              Older
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
