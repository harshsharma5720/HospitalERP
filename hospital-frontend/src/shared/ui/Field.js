import React from "react";

// Form controls with one look (docs/UI_REFRESH_PLAN.md). Field puts a label, the control and a hint or an error
// together; the control keeps its own id, so the label points at it (htmlFor).

export const controlClasses =
  "w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink placeholder:text-ink-muted/70 " +
  "focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 " +
  "disabled:bg-surface-2 disabled:text-ink-muted";

export function Field({ label, htmlFor, hint, error, className = "", children }) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      {label && (
        <label htmlFor={htmlFor} className="text-sm font-medium text-ink">
          {label}
        </label>
      )}
      {children}
      {error ? (
        <p className="text-xs text-danger-ink">{error}</p>
      ) : (
        hint && <p className="text-xs text-ink-muted">{hint}</p>
      )}
    </div>
  );
}

export const Input = React.forwardRef(function Input({ className = "", ...props }, ref) {
  return <input ref={ref} className={`h-10 ${controlClasses} ${className}`} {...props} />;
});

export const Select = React.forwardRef(function Select({ className = "", children, ...props }, ref) {
  return (
    <select ref={ref} className={`h-10 ${controlClasses} pr-8 ${className}`} {...props}>
      {children}
    </select>
  );
});

export const Textarea = React.forwardRef(function Textarea({ className = "", ...props }, ref) {
  return <textarea ref={ref} className={`py-2 ${controlClasses} ${className}`} {...props} />;
});
