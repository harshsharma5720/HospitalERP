import React from "react";
import { BarChart, Bar, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from "recharts";
import FittedXAxis from "./FittedXAxis";

// One measure as bars in one hue (docs/DOCTOR_ANALYTICS_PLAN.md): per day, hour or weekday. The card's title names
// the measure, so there is no legend; each bar has a hover tooltip; the numbers are also in the card's table view.
// A value of null (nothing measured that day) draws no bar.

function SeriesTooltip({ active, payload, xKey, tooltipTitle, valueText }) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload;
  return (
    <div className="rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink shadow-overlay">
      <p className="mb-1 font-semibold">{tooltipTitle(row[xKey])}</p>
      <p className="dash-num">{valueText(row)}</p>
    </div>
  );
}

// labelWidth: the px an axis label needs (a date about 80, an hour 44, a weekday 36)
export default function BarSeriesChart({ data, xKey, yKey, xTick, labelWidth = 80, tooltipTitle, valueText, yTick, label, allowDecimals = false }) {
  return (
    <div className="h-56" role="img" aria-label={label}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
          <CartesianGrid vertical={false} />
          <FittedXAxis count={data.length} labelWidth={labelWidth} dataKey={xKey} tickFormatter={xTick} tickLine={false} />
          <YAxis allowDecimals={allowDecimals} tickFormatter={yTick} tickLine={false} axisLine={false} />
          <Tooltip content={<SeriesTooltip xKey={xKey} tooltipTitle={tooltipTitle} valueText={valueText} />} />
          <Bar dataKey={yKey} className="series-single" fill="currentColor" radius={[4, 4, 0, 0]} maxBarSize={28} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
