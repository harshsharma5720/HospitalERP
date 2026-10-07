import React, { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";
import AppointmentTrendChart, { SERIES } from "./AppointmentTrendChart";
import { formatCount, formatDay, formatPercent, formatSpecialization } from "./dashboardFormat";
import "./AdminDashboard.css";

// The admin dashboard: real figures from GET /api/admin/dashboard (docs/ADMIN_DASHBOARD_PLAN.md).
const PERIODS = [7, 30, 90];

function StatTile({ label, value, note }) {
  return (
    <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-5">
      <p className="text-sm text-gray-600 dark:text-gray-300">{label}</p>
      <p className="text-3xl font-semibold mt-1 text-gray-900 dark:text-white">{value}</p>
      {note && <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{note}</p>}
    </div>
  );
}

// A ranked list with a thin bar per row (one hue) and the value at the bar's end
function RankedList({ title, rows }) {
  const max = Math.max(1, ...rows.map((row) => row.value));
  return (
    <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-6">
      <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">{title}</h3>
      {rows.length === 0 ? (
        <p className="text-sm text-gray-600 dark:text-gray-400">No appointments in this period.</p>
      ) : (
        <ol className="space-y-3">
          {rows.map((row) => (
            <li key={row.key}>
              <div className="flex justify-between gap-3 text-sm text-gray-800 dark:text-gray-200">
                <span>{row.label}</span>
                <span className="dash-num font-medium">{row.valueText}</span>
              </div>
              <div className="dash-rank-bar mt-1" style={{ width: `${(row.value / max) * 100}%` }} />
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

export default function AdminDashboard() {
  const [days, setDays] = useState(7);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const latestRequest = useRef(0);

  useEffect(() => {
    const request = ++latestRequest.current;
    setLoading(true);
    setError("");
    adminApi
      .getDashboard(days)
      .then((response) => {
        if (request === latestRequest.current) setData(response.data); // ignore answers to an older click
      })
      .catch((err) => {
        if (request === latestRequest.current) setError(getErrorMessage(err, "Could not load the dashboard."));
      })
      .finally(() => {
        if (request === latestRequest.current) setLoading(false);
      });
  }, [days]);

  const today = data?.today;
  const cancellations = data?.cancellations;

  return (
    <div className="admin-dash w-full">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <h1 className="text-3xl font-bold">Admin Dashboard</h1>
        <div role="group" aria-label="Period" className="flex gap-2">
          {PERIODS.map((period) => (
            <button
              key={period}
              type="button"
              aria-pressed={days === period}
              onClick={() => setDays(period)}
              className={`px-3 py-1 text-sm rounded-lg ${
                days === period ? "bg-[#0b1f36] dark:bg-blue-600 text-white" : "bg-gray-200 dark:bg-gray-700 dark:text-gray-100"
              }`}
            >
              {period} days
            </button>
          ))}
        </div>
      </div>

      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}
      {!data && loading && <p className="text-gray-600 dark:text-gray-300">Loading dashboard...</p>}

      {data && (
        <>
          {/* ================= TODAY ================= */}
          <section aria-labelledby="dash-today" className="mb-8">
            <h2 id="dash-today" className="text-xl font-semibold mb-3">
              Today · {formatDay(today.date)}
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
              <StatTile
                label="Appointments today"
                value={formatCount(today.appointments)}
                note={`${formatCount(today.completed)} completed · ${formatCount(today.upcoming)} upcoming · ${formatCount(today.cancelled)} cancelled`}
              />
              <StatTile label="Doctors on leave today" value={formatCount(today.doctorsOnLeave)} />
              <StatTile label="Active patients" value={formatCount(today.activePatients)} />
              <StatTile label="Active doctors" value={formatCount(today.activeDoctors)} />
              <StatTile label="Active receptionists" value={formatCount(today.activeReceptionists)} />
              <StatTile
                label="Undelivered notifications"
                value={formatCount(data.undeliveredNotifications)}
                note={
                  data.undeliveredNotifications > 0 ? (
                    <Link to="/admin/notifications" className="text-blue-700 dark:text-blue-300 underline">
                      View and resend
                    </Link>
                  ) : (
                    "Every email and SMS went out or is still being tried"
                  )
                }
              />
            </div>
          </section>

          {/* ================= PERIOD ================= */}
          <section aria-labelledby="dash-period" className="mb-8">
            <h2 id="dash-period" className="text-xl font-semibold mb-3">
              Last {data.period.days} days · {formatDay(data.period.from)} – {formatDay(data.period.to)}
              {loading && <span className="ml-3 text-sm font-normal text-gray-500">Updating…</span>}
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
              <StatTile label="Appointments" value={formatCount(cancellations.appointments)} />
              <StatTile
                label="Cancelled"
                value={formatPercent(cancellations.cancellationRate)}
                note={`${formatCount(cancellations.cancelled)} appointments: ${formatCount(cancellations.cancelledByPatient)} by patients, ${formatCount(cancellations.cancelledByDoctor)} by doctors`}
              />
              <StatTile
                label="Missed visits"
                value={formatPercent(cancellations.missedRate)}
                note={`${formatCount(cancellations.missed)} past appointments never completed`}
              />
              <StatTile
                label="New patients"
                value={formatCount(data.newPatients.count)}
                note={
                  data.newPatients.countedSince
                    ? `Counted since ${formatDay(data.newPatients.countedSince)}`
                    : "Counting starts with the next new account"
                }
              />
            </div>

            <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Appointments per day</h3>
              <ul className="flex flex-wrap gap-4 mt-2 mb-4 text-sm text-gray-700 dark:text-gray-300" aria-label="Legend">
                {SERIES.map((s) => (
                  <li key={s.key} className={`flex items-center gap-2 ${s.className}`}>
                    <span className="dash-swatch" />
                    <span className="text-gray-700 dark:text-gray-300">{s.label}</span>
                  </li>
                ))}
              </ul>
              <AppointmentTrendChart trend={data.trend} />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">
                Missed: a past appointment that was neither completed nor cancelled.
              </p>

              <details className="mt-4">
                <summary className="cursor-pointer text-sm text-blue-700 dark:text-blue-300">Show the numbers</summary>
                <div className="overflow-x-auto mt-3">
                  <table className="w-full text-sm text-gray-800 dark:text-gray-200">
                    <thead>
                      <tr className="border-b border-gray-200 dark:border-gray-700">
                        <th className="text-left p-2">Day</th>
                        {SERIES.map((s) => (
                          <th key={s.key} className="text-right p-2">
                            {s.label}
                          </th>
                        ))}
                        <th className="text-right p-2">New patients</th>
                      </tr>
                    </thead>
                    <tbody>
                      {[...data.trend].reverse().map((day) => (
                        <tr key={day.date} className="border-b border-gray-100 dark:border-gray-800">
                          <td className="p-2">{formatDay(day.date)}</td>
                          {SERIES.map((s) => (
                            <td key={s.key} className="dash-num text-right p-2">
                              {formatCount(day[s.key])}
                            </td>
                          ))}
                          <td className="dash-num text-right p-2">{formatCount(day.newPatients)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>
            </div>
          </section>

          {/* ================= BUSIEST ================= */}
          <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <RankedList
              title="Busiest specializations"
              rows={data.busiestSpecializations.map((s) => ({
                key: s.specialist ?? "none",
                label: formatSpecialization(s.specialist),
                value: s.appointments,
                valueText: formatCount(s.appointments),
              }))}
            />
            <RankedList
              title="Busiest doctors"
              rows={data.busiestDoctors.map((d) => ({
                key: d.doctorId,
                label: `${d.doctorName} · ${formatSpecialization(d.specialist)}`,
                value: d.appointments,
                valueText: `${formatCount(d.appointments)} (${formatCount(d.completed)} completed)`,
              }))}
            />
          </section>
        </>
      )}
    </div>
  );
}
