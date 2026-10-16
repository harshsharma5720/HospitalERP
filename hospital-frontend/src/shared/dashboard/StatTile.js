import React from "react";

// One figure on a dashboard: what it is, the value, and an optional note under it
export default function StatTile({ label, value, note }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
      <p className="text-sm text-ink-muted">{label}</p>
      <p className="dash-num mt-1 text-3xl font-semibold text-ink">{value}</p>
      {note && <p className="mt-1 text-sm text-ink-muted">{note}</p>}
    </div>
  );
}
