import React from "react";

// Tables with one look: a bordered card that scrolls sideways on a small screen, a quiet header row and
// separated rows. Use <Table>, <Th>, <Td> with ordinary <thead>/<tbody>/<tr>.

export function Table({ className = "", children, ...props }) {
  return (
    <div className={`overflow-x-auto rounded-xl border border-line bg-surface shadow-card ${className}`}>
      <table className="w-full border-collapse text-sm" {...props}>
        {children}
      </table>
    </div>
  );
}

export function Th({ align = "left", className = "", children, ...props }) {
  return (
    <th
      className={`bg-surface-2 border-b border-line px-4 py-3 text-xs font-semibold uppercase tracking-wide text-ink-muted ${
        align === "right" ? "text-right" : "text-left"
      } ${className}`}
      {...props}
    >
      {children}
    </th>
  );
}

export function Td({ align = "left", className = "", children, ...props }) {
  return (
    <td className={`border-b border-line px-4 py-3 align-middle ${align === "right" ? "text-right" : ""} ${className}`} {...props}>
      {children}
    </td>
  );
}

// A row's last cell spanning the table, for "nothing found" and similar
export function TableMessage({ colSpan, children }) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-4 py-10 text-center text-sm text-ink-muted">
        {children}
      </td>
    </tr>
  );
}
