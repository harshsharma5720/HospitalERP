import React from "react";

// "Nothing here yet" inside a card: an optional icon, a title, a line of help and an optional action
export default function EmptyState({ icon: Icon, title, children, action }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-10 text-center">
      {Icon && (
        <div className="mb-3 rounded-full bg-surface-2 p-3 text-ink-muted">
          <Icon size={22} aria-hidden="true" />
        </div>
      )}
      <p className="font-medium text-ink">{title}</p>
      {children && <p className="mt-1 max-w-sm text-sm text-ink-muted">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
