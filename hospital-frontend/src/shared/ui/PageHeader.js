import React from "react";

// A page's title, an optional line under it, actions on the right and an optional back link above it
export default function PageHeader({ title, description, actions, back, className = "" }) {
  return (
    <header className={`mb-6 ${className}`}>
      {back && <div className="mb-2 text-sm">{back}</div>}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold tracking-tight text-ink">{title}</h1>
          {description && <div className="mt-1 text-sm text-ink-muted">{description}</div>}
        </div>
        {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  );
}
