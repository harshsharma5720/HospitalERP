import React from "react";

// A white panel with a soft border and shadow. Card padding="none" for tables that reach the edges.
export default function Card({ as: Component = "section", padding = "md", className = "", children, ...props }) {
  const pad = padding === "none" ? "" : padding === "sm" ? "p-4" : "p-5";
  return (
    <Component className={`rounded-xl border border-line bg-surface shadow-card ${pad} ${className}`} {...props}>
      {children}
    </Component>
  );
}

// A card's title row: heading (with an id for aria-labelledby), optional description and actions on the right
export function CardHeader({ id, title, description, actions, className = "" }) {
  return (
    <div className={`flex flex-wrap items-start justify-between gap-3 mb-4 ${className}`}>
      <div>
        <h2 id={id} className="text-base font-semibold text-ink">
          {title}
        </h2>
        {description && <p className="text-sm text-ink-muted mt-0.5">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}
