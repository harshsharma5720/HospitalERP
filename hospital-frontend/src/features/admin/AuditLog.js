import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";

// Who viewed or changed which patient record, and when (docs/AUDIT_LOG_PLAN.md). Read-only: entries are never changed.
export const ACTION_LABELS = {
  CONSULTATION_VIEWED: "Viewed consultation",
  CONSULTATION_SAVED: "Saved consultation",
  PRESCRIPTION_DOWNLOADED: "Downloaded prescription",
  MEDICAL_HISTORY_VIEWED: "Viewed medical history",
  PATIENT_PROFILE_VIEWED: "Viewed patient profile",
  PATIENT_PROFILE_UPDATED: "Updated patient profile",
  PATIENT_LIST_VIEWED: "Viewed patient list",
  USER_CREATED: "Created user",
  ACCOUNT_DEACTIVATED: "Deactivated account",
  ACCOUNT_REACTIVATED: "Reactivated account",
  ACCOUNT_DELETED: "Deleted account",
};

const PAGE_SIZE = 50;
const NO_FILTERS = { patientId: "", username: "", action: "", from: "", to: "" };

// "2026-10-05T16:40:12.123456" (server time) -> "2026-10-05 16:40:12"
const formatTime = (occurredAt) => (occurredAt ? String(occurredAt).replace("T", " ").slice(0, 19) : "");

const targetLabel = (entry) => {
  if (entry.targetId == null) return "";
  if (entry.targetType === "APPOINTMENT") return `Appointment #${entry.targetId}`;
  if (entry.targetType === "USER") return `User #${entry.targetId}`;
  return ""; // a patient target is shown in the Patient column
};

export default function AuditLog() {
  const [filters, setFilters] = useState(NO_FILTERS);
  const [applied, setApplied] = useState(NO_FILTERS); // the filters of the shown result
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async (activeFilters, pageNumber) => {
    setLoading(true);
    setError("");
    // Only filled-in filters are sent
    const params = { page: pageNumber, size: PAGE_SIZE };
    Object.entries(activeFilters).forEach(([key, value]) => {
      if (String(value).trim() !== "") params[key] = String(value).trim();
    });
    try {
      const response = await adminApi.getAuditLog(params);
      setResult(response.data);
    } catch (err) {
      setResult(null);
      setError(getErrorMessage(err, "Could not load the audit log."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(applied, page);
  }, [load, applied, page]);

  const search = (e) => {
    e.preventDefault();
    setPage(0);
    setApplied({ ...filters });
  };

  const clear = () => {
    setFilters(NO_FILTERS);
    setPage(0);
    setApplied(NO_FILTERS);
  };

  const change = (field) => (e) => setFilters({ ...filters, [field]: e.target.value });

  const entries = result?.entries ?? [];
  const totalPages = result?.totalPages ?? 0;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-2">Audit Log</h1>
      <p className="text-gray-600 mb-4">
        Who viewed or changed patient records and accounts, newest first. Entries are kept forever and can't be changed.
      </p>

      <form onSubmit={search} className="bg-white shadow-md rounded-lg p-4 mb-4 grid grid-cols-1 md:grid-cols-6 gap-3 items-end">
        <div className="flex flex-col text-sm">
          <label htmlFor="audit-patient">Patient ID</label>
          <input id="audit-patient" type="number" min="1" value={filters.patientId} onChange={change("patientId")}
            className="border p-2 rounded" />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="audit-username">Username</label>
          <input id="audit-username" type="text" value={filters.username} onChange={change("username")}
            className="border p-2 rounded" />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="audit-action">Action</label>
          <select id="audit-action" value={filters.action} onChange={change("action")} className="border p-2 rounded">
            <option value="">All actions</option>
            {Object.entries(ACTION_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="audit-from">From</label>
          <input id="audit-from" type="date" value={filters.from} onChange={change("from")} className="border p-2 rounded" />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="audit-to">To</label>
          <input id="audit-to" type="date" value={filters.to} onChange={change("to")} className="border p-2 rounded" />
        </div>
        <div className="flex gap-2">
          <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded">
            Search
          </button>
          <button type="button" onClick={clear} className="border border-gray-400 px-4 py-2 rounded">
            Clear
          </button>
        </div>
      </form>

      {error && <p className="text-red-600 mb-4">{error}</p>}

      <div className="overflow-x-auto">
        <table className="w-full border-collapse bg-white shadow-md rounded-lg text-sm">
          <thead>
            <tr className="bg-gray-100 border-b">
              <th className="p-3 text-left">Time</th>
              <th className="p-3 text-left">User</th>
              <th className="p-3 text-left">Action</th>
              <th className="p-3 text-left">Patient</th>
              <th className="p-3 text-left">Record</th>
              <th className="p-3 text-left">Details</th>
              <th className="p-3 text-left">IP address</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr key={entry.id} className="border-b">
                <td className="p-3 whitespace-nowrap">{formatTime(entry.occurredAt)}</td>
                <td className="p-3">
                  {entry.actorUsername ? (
                    <>
                      {entry.actorUsername}
                      {entry.actorRole && (
                        <span className="ml-2 px-2 py-1 rounded-full text-xs bg-gray-200 text-gray-700">{entry.actorRole}</span>
                      )}
                    </>
                  ) : (
                    <span className="text-gray-500">System</span>
                  )}
                </td>
                <td className="p-3">{ACTION_LABELS[entry.action] ?? entry.action}</td>
                <td className="p-3">
                  {entry.patientId != null ? `${entry.patientName ?? "(deleted)"} #${entry.patientId}` : "—"}
                </td>
                <td className="p-3">{targetLabel(entry)}</td>
                <td className="p-3">{entry.details}</td>
                <td className="p-3">{entry.ipAddress}</td>
              </tr>
            ))}
            {!loading && !error && entries.length === 0 && (
              <tr>
                <td colSpan={7} className="p-6 text-center text-gray-500">
                  No entries match these filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-3 mt-4">
        <button
          type="button"
          onClick={() => setPage(page - 1)}
          disabled={loading || page === 0}
          className="border px-3 py-1 rounded disabled:opacity-50"
        >
          Previous
        </button>
        <span className="text-sm text-gray-700">
          {loading
            ? "Loading..."
            : `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} · ${result?.totalEntries ?? 0} entries`}
        </span>
        <button
          type="button"
          onClick={() => setPage(page + 1)}
          disabled={loading || page + 1 >= totalPages}
          className="border px-3 py-1 rounded disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  );
}
