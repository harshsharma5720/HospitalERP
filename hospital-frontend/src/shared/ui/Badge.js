import React from "react";

// A small status label. The label always says it; the colour only supports it.
// tone: neutral, primary, success, warning, danger, info
const TONES = {
  neutral: "bg-surface-2 text-ink-muted ring-line",
  primary: "bg-primary-ink/10 text-primary-ink ring-primary-ink/20",
  success: "bg-success-ink/10 text-success-ink ring-success-ink/20",
  warning: "bg-warning-ink/10 text-warning-ink ring-warning-ink/25",
  danger: "bg-danger-ink/10 text-danger-ink ring-danger-ink/20",
  info: "bg-info-ink/10 text-info-ink ring-info-ink/20",
};

export default function Badge({ tone = "neutral", className = "", children }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset whitespace-nowrap ${
        TONES[tone] ?? TONES.neutral
      } ${className}`}
    >
      {children}
    </span>
  );
}
