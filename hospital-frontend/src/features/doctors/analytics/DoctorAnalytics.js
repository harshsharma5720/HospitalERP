import React, { useEffect, useRef, useState } from "react";
import AppointmentTrendChart, { SERIES } from "../../../shared/dashboard/AppointmentTrendChart";
import BarSeriesChart from "../../../shared/dashboard/BarSeriesChart";
import PeriodSwitch from "../../../shared/dashboard/PeriodSwitch";
import StatTile from "../../../shared/dashboard/StatTile";
import { formatCount, formatDay, formatPercent, formatShortDay } from "../../../shared/dashboard/dashboardFormat";
import "../../../shared/dashboard/dashboard.css";
import { getErrorMessage } from "../../../shared/utils/apiError";
import { formatRupees, formatWholeRupees } from "../../../shared/utils/money";
import { Notice } from "../../../shared/ui";
import * as analyticsApi from "./api";

// A doctor's visits and earnings in numbers (docs/DOCTOR_ANALYTICS_PLAN.md): the logged-in doctor's (no doctorId) or
// any doctor's for an admin. onDoctor gets the doctor's name and speciality once loaded.

const CARD = "rounded-xl border border-line bg-surface p-6 shadow-card";
const WEEKDAYS = {
  MONDAY: ["Mon", "Monday"], TUESDAY: ["Tue", "Tuesday"], WEDNESDAY: ["Wed", "Wednesday"], THURSDAY: ["Thu", "Thursday"],
  FRIDAY: ["Fri", "Friday"], SATURDAY: ["Sat", "Saturday"], SUNDAY: ["Sun", "Sunday"],
};
const hourText = (hour) => `${String(hour).padStart(2, "0")}:00`;
const minutesText = (minutes) => `${Number(minutes).toLocaleString("en-IN", { maximumFractionDigits: 1 })} min`;
const visitsText = (n) => `${formatCount(n)} ${n === 1 ? "visit" : "visits"}`;

// A chart's numbers as a table, folded away (the charts' accessible view)
function NumbersTable({ columns, rows }) {
  return (
    <details className="mt-4">
      <summary className="cursor-pointer text-sm font-medium text-primary-ink">Show the numbers</summary>
      <div className="mt-3 overflow-x-auto">
        <table className="w-full text-sm text-ink">
          <thead>
            <tr className="border-b border-line text-xs uppercase tracking-wide text-ink-muted">
              {columns.map((column, i) => (
                <th key={column} className={`p-2 font-semibold ${i === 0 ? "text-left" : "text-right"}`}>
                  {column}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row[0]} className="border-b border-line last:border-b-0">
                {row.map((cell, i) => (
                  <td key={i} className={`p-2 ${i === 0 ? "" : "dash-num text-right"}`}>
                    {cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
}

function ChartCard({ id, title, note, children }) {
  return (
    <section aria-labelledby={id} className={CARD}>
      <h3 id={id} className="text-base font-semibold text-ink">
        {title}
      </h3>
      {note && <p className="mt-1 text-sm text-ink-muted">{note}</p>}
      <div className="mt-4">{children}</div>
    </section>
  );
}

export default function DoctorAnalytics({ doctorId, onDoctor }) {
  const [days, setDays] = useState(30);
  const [visits, setVisits] = useState(null);
  const [earnings, setEarnings] = useState(null);
  const [error, setError] = useState("");
  const [earningsError, setEarningsError] = useState("");
  const [loading, setLoading] = useState(true);
  const latestRequest = useRef(0);
  const onDoctorRef = useRef(onDoctor);
  onDoctorRef.current = onDoctor;

  useEffect(() => {
    const request = ++latestRequest.current;
    setLoading(true);
    setError("");
    setEarningsError("");
    // The earnings are a separate call: the visits still show when they fail
    Promise.allSettled([analyticsApi.getAnalytics(days, doctorId), analyticsApi.getEarnings(days, doctorId)]).then(
      ([visitsAnswer, earningsAnswer]) => {
        if (request !== latestRequest.current) return; // an older click's answer
        if (visitsAnswer.status === "fulfilled") {
          setVisits(visitsAnswer.value.data);
          onDoctorRef.current?.(visitsAnswer.value.data.doctor);
        } else {
          setError(getErrorMessage(visitsAnswer.reason, "Could not load the figures."));
        }
        if (earningsAnswer.status === "fulfilled") {
          setEarnings(earningsAnswer.value.data);
        } else {
          setEarnings(null);
          setEarningsError(getErrorMessage(earningsAnswer.reason, "Could not load the earnings."));
        }
        setLoading(false);
      }
    );
  }, [days, doctorId]);

  const totals = visits?.visits;
  const waiting = visits?.waiting;

  return (
    <section aria-labelledby="analytics-period" className="dash">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <h2 id="analytics-period" className="text-lg font-semibold">
          {visits ? `Last ${visits.period.days} days · ${formatDay(visits.period.from)} – ${formatDay(visits.period.to)}` : "Figures"}
          {loading && visits && <span className="ml-3 text-sm font-normal text-ink-muted">Updating…</span>}
        </h2>
        <PeriodSwitch days={days} onChange={setDays} />
      </div>

      <Notice tone="danger" className="mb-4">
        {error}
      </Notice>
      {!visits && loading && <p className="text-ink-muted">Loading the figures...</p>}

      {visits && (
        <div className="space-y-5">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
            <StatTile
              label="Visits"
              value={formatCount(totals.total)}
              note={`${formatCount(totals.completed)} completed · ${formatCount(totals.upcoming)} upcoming`}
            />
            <StatTile label="No-shows" value={formatPercent(totals.noShowRate)} note={`${visitsText(totals.missed)} missed`} />
            <StatTile label="Cancelled" value={formatPercent(totals.cancellationRate)} note={visitsText(totals.cancelled)} />
            <StatTile
              label="Average wait"
              value={waiting.averageMinutes == null ? "—" : minutesText(waiting.averageMinutes)}
              note={
                waiting.measured > 0
                  ? `${visitsText(waiting.measured)} measured · longest ${minutesText(waiting.longestMinutes)}`
                  : "From check-in to being called in"
              }
            />
            <StatTile
              label="Earned"
              value={earnings ? formatWholeRupees(earnings.earned) : "—"}
              note={
                earnings
                  ? earnings.paidVisits > 0
                    ? `${formatCount(earnings.paidVisits)} paid ${earnings.paidVisits === 1 ? "visit" : "visits"} · ${formatRupees(earnings.averagePerVisit)} each`
                    : "No paid visits in this period"
                  : earningsError || "Loading..."
              }
            />
          </div>

          <ChartCard id="analytics-per-day" title="Visits per day" note="Missed: a past visit that was neither completed nor cancelled.">
            <ul className="mb-4 flex flex-wrap gap-4 text-sm" aria-label="Legend">
              {SERIES.map((s) => (
                <li key={s.key} className={`flex items-center gap-2 ${s.className}`}>
                  <span className="dash-swatch" />
                  <span className="text-ink-muted">{s.label}</span>
                </li>
              ))}
            </ul>
            <AppointmentTrendChart trend={visits.perDay} />
            <NumbersTable
              columns={["Day", ...SERIES.map((s) => s.label)]}
              rows={[...visits.perDay].reverse().map((day) => [formatDay(day.date), ...SERIES.map((s) => formatCount(day[s.key]))])}
            />
          </ChartCard>

          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            <ChartCard id="analytics-hours" title="Busy hours" note="Visits by the hour they start; cancelled ones not counted.">
              {visits.byHour.length === 0 ? (
                <p className="text-sm text-ink-muted">No visits in this period.</p>
              ) : (
                <>
                  <BarSeriesChart
                    data={visits.byHour}
                    xKey="hour"
                    yKey="visits"
                    xTick={hourText}
                    labelWidth={44}
                    tooltipTitle={(hour) => `${hourText(hour)}–${hourText(hour + 1)}`}
                    valueText={(row) => visitsText(row.visits)}
                    label="Visits by the hour they start"
                  />
                  <NumbersTable
                    columns={["Hour", "Visits"]}
                    rows={visits.byHour.map((h) => [`${hourText(h.hour)}–${hourText(h.hour + 1)}`, formatCount(h.visits)])}
                  />
                </>
              )}
            </ChartCard>
            <ChartCard id="analytics-days" title="Busy days" note="Visits by weekday; cancelled ones not counted.">
              <BarSeriesChart
                data={visits.byWeekday}
                xKey="day"
                yKey="visits"
                xTick={(day) => WEEKDAYS[day]?.[0] ?? day}
                labelWidth={36}
                tooltipTitle={(day) => WEEKDAYS[day]?.[1] ?? day}
                valueText={(row) => visitsText(row.visits)}
                label="Visits by weekday"
              />
              <NumbersTable
                columns={["Day", "Visits"]}
                rows={visits.byWeekday.map((d) => [WEEKDAYS[d.day]?.[1] ?? d.day, formatCount(d.visits)])}
              />
            </ChartCard>
          </div>

          <ChartCard id="analytics-waiting" title="Waiting time" note="Average minutes from check-in to being called in, per day.">
            {waiting.measured === 0 ? (
              <p className="text-sm text-ink-muted">
                No waits measured in this period. They are measured when patients are checked in and called in through
                today's queue.
              </p>
            ) : (
              <>
                <BarSeriesChart
                  data={waiting.perDay}
                  xKey="date"
                  yKey="averageMinutes"
                  xTick={formatShortDay}
                  yTick={(m) => `${m}`}
                  tooltipTitle={formatDay}
                  valueText={(row) =>
                    row.averageMinutes == null ? "Not measured" : `${minutesText(row.averageMinutes)} average · ${visitsText(row.measured)}`
                  }
                  label="Average waiting minutes per day"
                  allowDecimals
                />
                <NumbersTable
                  columns={["Day", "Average wait", "Visits measured"]}
                  rows={[...waiting.perDay]
                    .reverse()
                    .map((day) => [formatDay(day.date), day.averageMinutes == null ? "—" : minutesText(day.averageMinutes), formatCount(day.measured)])}
                />
              </>
            )}
          </ChartCard>

          <ChartCard id="analytics-earnings" title="Earnings" note="Paid bills of the period's visits, by visit date; refunds and unpaid bills not included.">
            {!earnings ? (
              <Notice tone="danger">{earningsError || "Loading..."}</Notice>
            ) : (
              <>
                <dl className="mb-4 flex flex-wrap gap-x-8 gap-y-2 text-sm">
                  <div>
                    <dt className="text-ink-muted">Earned</dt>
                    <dd className="dash-num font-semibold">{formatRupees(earnings.earned)}</dd>
                  </div>
                  <div>
                    <dt className="text-ink-muted">Refunds</dt>
                    <dd className="dash-num font-semibold">
                      {formatRupees(earnings.refunds)} <span className="font-normal text-ink-muted">({visitsText(earnings.refundedVisits)})</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-ink-muted">Still unpaid</dt>
                    <dd className="dash-num font-semibold">
                      {formatRupees(earnings.unpaid)} <span className="font-normal text-ink-muted">({visitsText(earnings.unpaidVisits)})</span>
                    </dd>
                  </div>
                </dl>
                <BarSeriesChart
                  data={earnings.perDay.map((day) => ({ ...day, earned: Number(day.earned) }))}
                  xKey="date"
                  yKey="earned"
                  xTick={formatShortDay}
                  yTick={(n) => `₹${formatCount(n)}`}
                  tooltipTitle={formatDay}
                  valueText={(row) => formatRupees(row.earned)}
                  label="Earnings per day"
                />
                <NumbersTable
                  columns={["Day", "Earned"]}
                  rows={[...earnings.perDay].reverse().map((day) => [formatDay(day.date), formatRupees(day.earned)])}
                />
              </>
            )}
          </ChartCard>
        </div>
      )}
    </section>
  );
}
