import React from "react";

// The dashboards' periods: the last 7, 30 or 90 days, ending today
export const PERIODS = [7, 30, 90];

export default function PeriodSwitch({ days, onChange }) {
  return (
    <div role="group" aria-label="Period" className="inline-flex rounded-lg border border-line bg-surface p-1 shadow-card">
      {PERIODS.map((period) => (
        <button
          key={period}
          type="button"
          aria-pressed={days === period}
          onClick={() => onChange(period)}
          className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
            days === period ? "bg-primary text-white" : "text-ink-muted hover:text-ink"
          }`}
        >
          {period} days
        </button>
      ))}
    </div>
  );
}
