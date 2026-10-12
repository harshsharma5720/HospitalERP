import React from "react";

// A message on the page: what just happened (tone "success", role="status") or what went wrong (tone "danger",
// role="alert"). For a passing confirmation that needn't stay on the page, use a toast (useToast) instead.
const TONES = {
  success: "border-success-ink/30 bg-success-ink/10 text-success-ink",
  danger: "border-danger-ink/30 bg-danger-ink/10 text-danger-ink",
  warning: "border-warning-ink/30 bg-warning-ink/10 text-warning-ink",
  info: "border-info-ink/30 bg-info-ink/10 text-info-ink",
};

export default function Notice({ tone = "success", className = "", children }) {
  if (!children) return null;
  return (
    <p
      role={tone === "danger" ? "alert" : "status"}
      className={`rounded-lg border px-4 py-3 text-sm ${TONES[tone] ?? TONES.info} ${className}`}
    >
      {children}
    </p>
  );
}
