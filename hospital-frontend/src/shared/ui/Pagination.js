import React from "react";
import Button from "./Button";

// Previous / Next under a paged list, and "Page 2 of 5 · 93 patients" (page counts from 0; noun: "patients", ...)
export default function Pagination({ page, totalPages, totalEntries, noun, loading, onPage, previousLabel = "Previous", nextLabel = "Next" }) {
  return (
    <div className="flex flex-wrap items-center gap-3 mt-4">
      <Button variant="secondary" size="sm" onClick={() => onPage(page - 1)} disabled={loading || page === 0}>
        {previousLabel}
      </Button>
      <span className="text-sm text-ink-muted">
        {loading ? "Loading..." : `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} · ${totalEntries ?? 0} ${noun}`}
      </span>
      <Button variant="secondary" size="sm" onClick={() => onPage(page + 1)} disabled={loading || page + 1 >= totalPages}>
        {nextLabel}
      </Button>
    </div>
  );
}
