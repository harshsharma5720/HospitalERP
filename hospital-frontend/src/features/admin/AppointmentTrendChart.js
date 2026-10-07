import React from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from "recharts";
import { formatDay, formatShortDay } from "./dashboardFormat";

// The stack, bottom to top. Colors come from AdminDashboard.css (the order matters - see the note there).
export const SERIES = [
  { key: "completed", label: "Completed", className: "series-completed" },
  { key: "missed", label: "Missed", className: "series-missed" },
  { key: "upcoming", label: "Upcoming", className: "series-upcoming" },
  { key: "cancelled", label: "Cancelled", className: "series-cancelled" },
];

const GAP = 2; // px of card color between stacked segments

// The highest series with appointments that day gets the rounded end; the others leave a gap above them
const topSeriesOf = (row) => [...SERIES].reverse().find((s) => row[s.key] > 0)?.key;

function Segment({ x, y, width, height, payload, seriesKey }) {
  if (!height || height <= 0) return null;
  const top = topSeriesOf(payload) === seriesKey;
  if (!top) {
    const h = Math.max(height - GAP, 0);
    return h > 0 ? <rect x={x} y={y + GAP} width={width} height={h} fill="currentColor" /> : null;
  }
  const r = Math.min(4, width / 2, height); // 4px rounded data end, square at the baseline
  const path = `M${x},${y + height} V${y + r} Q${x},${y} ${x + r},${y} H${x + width - r} Q${x + width},${y} ${x + width},${y + r} V${y + height} Z`;
  return <path d={path} fill="currentColor" />;
}

function TrendTooltip({ active, payload }) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload;
  const total = SERIES.reduce((sum, s) => sum + row[s.key], 0);
  return (
    <div className="bg-white dark:bg-[#0f172a] text-gray-900 dark:text-gray-100 border border-gray-200 dark:border-gray-700 rounded-lg shadow px-3 py-2 text-sm">
      <p className="font-semibold mb-1">{formatDay(row.date)}</p>
      {SERIES.map((s) => (
        <p key={s.key} className="flex items-center gap-2">
          <span className={`dash-swatch ${s.className}`} />
          <span className="flex-1">{s.label}</span>
          <span className="dash-num font-medium">{row[s.key]}</span>
        </p>
      ))}
      <p className="flex justify-between gap-4 border-t border-gray-200 dark:border-gray-700 mt-1 pt-1">
        <span>Total</span>
        <span className="dash-num font-medium">{total}</span>
      </p>
    </div>
  );
}

// Appointments per day, stacked by outcome. The legend and the table view are in AdminDashboard.
export default function AppointmentTrendChart({ trend }) {
  return (
    <div className="h-72">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={trend} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
          <CartesianGrid vertical={false} />
          <XAxis dataKey="date" tickFormatter={formatShortDay} tickLine={false} minTickGap={16} />
          <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
          <Tooltip content={<TrendTooltip />} />
          {SERIES.map((s) => (
            <Bar
              key={s.key}
              dataKey={s.key}
              name={s.label}
              stackId="appointments"
              className={s.className}
              fill="currentColor"
              maxBarSize={24}
              isAnimationActive={false}
              shape={(props) => <Segment {...props} seriesKey={s.key} />}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
