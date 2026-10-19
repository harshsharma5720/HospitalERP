import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";
import { Badge, Button, Card, Field, Input, Notice, PageHeader, Pagination, Select, Table, TableMessage, Td, Th } from "../../shared/ui";

// Who viewed or changed which patient record, and when (docs/AUDIT_LOG_PLAN.md). Read-only: entries are never changed.
export const ACTION_LABELS = {
  CONSULTATION_VIEWED: "Viewed consultation",
  CONSULTATION_SAVED: "Saved consultation",
  PRESCRIPTION_DOWNLOADED: "Downloaded prescription",
  MEDICAL_HISTORY_VIEWED: "Viewed medical history",
  PATIENT_PROFILE_VIEWED: "Viewed patient profile",
  PATIENT_PROFILE_UPDATED: "Updated patient profile",
  PATIENT_LIST_VIEWED: "Viewed patient list",
  PATIENTS_SEARCHED: "Searched patients by phone",
  WALK_IN_REGISTERED: "Registered walk-in patient",
  USER_CREATED: "Created user",
  ACCOUNT_DEACTIVATED: "Deactivated account",
  ACCOUNT_REACTIVATED: "Reactivated account",
  ACCOUNT_DELETED: "Deleted account",
  CASH_DAY_CLOSED: "Closed the day (cash)",
};

const PAGE_SIZE = 50;
const NO_FILTERS = { patientId: "", username: "", action: "", from: "", to: "" };

// "2026-10-05T16:40:12.123456" (server time) -> "2026-10-05 16:40:12"
const formatTime = (occurredAt) => (occurredAt ? String(occurredAt).replace("T", " ").slice(0, 19) : "");

const targetLabel = (entry) => {
  if (entry.targetId == null) return "";
  if (entry.targetType === "APPOINTMENT") return `Appointment #${entry.targetId}`;
  if (entry.targetType === "USER") return `User #${entry.targetId}`;
  if (entry.targetType === "CASH_CLOSE") return `Day close #${entry.targetId}`;
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
    <>
      <PageHeader
        title="Audit log"
        description="Who viewed or changed patient records and accounts, newest first. Entries are kept forever and can't be changed."
      />

      <Card padding="sm" className="mb-4">
        <form onSubmit={search} className="grid grid-cols-1 items-end gap-3 sm:grid-cols-2 lg:grid-cols-6">
          <Field label="Patient ID" htmlFor="audit-patient">
            <Input id="audit-patient" type="number" min="1" value={filters.patientId} onChange={change("patientId")} />
          </Field>
          <Field label="Username" htmlFor="audit-username">
            <Input id="audit-username" type="text" value={filters.username} onChange={change("username")} />
          </Field>
          <Field label="Action" htmlFor="audit-action">
            <Select id="audit-action" value={filters.action} onChange={change("action")}>
              <option value="">All actions</option>
              {Object.entries(ACTION_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="From" htmlFor="audit-from">
            <Input id="audit-from" type="date" value={filters.from} onChange={change("from")} />
          </Field>
          <Field label="To" htmlFor="audit-to">
            <Input id="audit-to" type="date" value={filters.to} onChange={change("to")} />
          </Field>
          <div className="flex gap-2">
            <Button type="submit">Search</Button>
            <Button variant="secondary" onClick={clear}>
              Clear
            </Button>
          </div>
        </form>
      </Card>

      <Notice tone="danger" className="mb-4">{error}</Notice>

      <Table>
        <thead>
          <tr>
            <Th>Time</Th>
            <Th>User</Th>
            <Th>Action</Th>
            <Th>Patient</Th>
            <Th>Record</Th>
            <Th>Details</Th>
            <Th>IP address</Th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id}>
              <Td className="whitespace-nowrap tabular-nums text-ink-muted">{formatTime(entry.occurredAt)}</Td>
              <Td className="whitespace-nowrap">
                {entry.actorUsername ? (
                  <>
                    <span className="font-medium">{entry.actorUsername}</span>
                    {entry.actorRole && (
                      <span className="ml-2">
                        <Badge>{entry.actorRole}</Badge>
                      </span>
                    )}
                  </>
                ) : (
                  <span className="text-ink-muted">System</span>
                )}
              </Td>
              <Td>{ACTION_LABELS[entry.action] ?? entry.action}</Td>
              <Td>{entry.patientId != null ? `${entry.patientName ?? "(deleted)"} #${entry.patientId}` : "—"}</Td>
              <Td className="whitespace-nowrap">{targetLabel(entry)}</Td>
              <Td className="text-ink-muted">{entry.details}</Td>
              <Td className="tabular-nums text-ink-muted">{entry.ipAddress}</Td>
            </tr>
          ))}
          {!loading && !error && entries.length === 0 && <TableMessage colSpan={7}>No entries match these filters.</TableMessage>}
        </tbody>
      </Table>

      <Pagination page={page} totalPages={totalPages} totalEntries={result?.totalEntries} noun="entries" loading={loading} onPage={setPage} />
    </>
  );
}
