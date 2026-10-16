import React, { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";
import AppointmentTrendChart, { SERIES } from "../../shared/dashboard/AppointmentTrendChart";
import { formatCount, formatDay, formatPercent, formatSpecialization } from "../../shared/dashboard/dashboardFormat";
import PeriodSwitch from "../../shared/dashboard/PeriodSwitch";
import StatTile from "../../shared/dashboard/StatTile";
import "../../shared/dashboard/dashboard.css";
import { Notice, PageHeader } from "../../shared/ui";

// The admin dashboard: real figures from GET /api/admin/dashboard (docs/ADMIN_DASHBOARD_PLAN.md).
const CARD = "rounded-xl border border-line bg-surface shadow-card";

// A ranked list with a thin bar per row (one hue) and the value at the bar's end
function RankedList({ title, rows }) {
  const max = Math.max(1, ...rows.map((row) => row.value));
  return (
    <div className={`${CARD} p-6`}>
      <h3 className="mb-4 text-base font-semibold text-ink">{title}</h3>
      {rows.length === 0 ? (
        <p className="text-sm text-ink-muted">No appointments in this period.</p>
      ) : (
        <ol className="space-y-3">
          {rows.map((row) => (
            <li key={row.key}>
              <div className="flex justify-between gap-3 text-sm text-ink">
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
    <div className="dash w-full">
      <PageHeader
        title="Dashboard"
        description="Today at the hospital, and how the last days went."
        actions={
          <PeriodSwitch days={days} onChange={setDays} />
        }
      />

      <Notice tone="danger" className="mb-4">{error}</Notice>
      {!data && loading && <p className="text-ink-muted">Loading dashboard...</p>}

      {data && (
        <>
          {/* ================= TODAY ================= */}
          <section aria-labelledby="dash-today" className="mb-8">
            <h2 id="dash-today" className="mb-3 text-lg font-semibold">
              Today · {formatDay(today.date)}
            </h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
                    <Link to="/admin/notifications" className="font-medium text-primary-ink hover:underline">
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
            <h2 id="dash-period" className="mb-3 text-lg font-semibold">
              Last {data.period.days} days · {formatDay(data.period.from)} – {formatDay(data.period.to)}
              {loading && <span className="ml-3 text-sm font-normal text-ink-muted">Updating…</span>}
            </h2>
            <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
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

            <div className={`${CARD} p-6`}>
              <h3 className="text-base font-semibold text-ink">Appointments per day</h3>
              <ul className="mb-4 mt-2 flex flex-wrap gap-4 text-sm" aria-label="Legend">
                {SERIES.map((s) => (
                  <li key={s.key} className={`flex items-center gap-2 ${s.className}`}>
                    <span className="dash-swatch" />
                    <span className="text-ink-muted">{s.label}</span>
                  </li>
                ))}
              </ul>
              <AppointmentTrendChart trend={data.trend} />
              <p className="mt-2 text-xs text-ink-muted">Missed: a past appointment that was neither completed nor cancelled.</p>

              <details className="mt-4">
                <summary className="cursor-pointer text-sm font-medium text-primary-ink">Show the numbers</summary>
                <div className="mt-3 overflow-x-auto">
                  <table className="w-full text-sm text-ink">
                    <thead>
                      <tr className="border-b border-line text-xs uppercase tracking-wide text-ink-muted">
                        <th className="p-2 text-left font-semibold">Day</th>
                        {SERIES.map((s) => (
                          <th key={s.key} className="p-2 text-right font-semibold">
                            {s.label}
                          </th>
                        ))}
                        <th className="p-2 text-right font-semibold">New patients</th>
                      </tr>
                    </thead>
                    <tbody>
                      {[...data.trend].reverse().map((day) => (
                        <tr key={day.date} className="border-b border-line last:border-b-0">
                          <td className="p-2">{formatDay(day.date)}</td>
                          {SERIES.map((s) => (
                            <td key={s.key} className="dash-num p-2 text-right">
                              {formatCount(day[s.key])}
                            </td>
                          ))}
                          <td className="dash-num p-2 text-right">{formatCount(day.newPatients)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>
            </div>
          </section>

          {/* ================= BUSIEST ================= */}
          <section className="grid grid-cols-1 gap-5 md:grid-cols-2">
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
