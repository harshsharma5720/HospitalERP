import React, { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { formatRupees } from "../../shared/utils/money";
import useAreaBase from "../../shared/useAreaBase";
import { dayLabel, formatDate } from "../appointments/appointmentFormat";
import { BILL_STATUS, BillStatusBadge } from "./billFormat";
import { getBills } from "./api";
import { Button, Card, Field, Input, Notice, PageHeader, Pagination, Select, Table, TableMessage, Td, Th } from "../../shared/ui";

// The front desk's bills (docs/BILLING_PLAN.md): today's by default; dates, status and patient (or bill number)
// filters; newest first, 20 per page. The filters live in the address, so coming back from a bill shows the same
// list. Receptionists: /bills, admins: /admin/bills.

const PAGE_SIZE = 20;

const dateLabel = (isoDate) =>
  isoDate.startsWith(String(new Date().getFullYear())) ? dayLabel(isoDate) : formatDate(isoDate, true);

export default function BillsPage() {
  const base = useAreaBase();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const today = toLocalISODate();
  // No dates in the address: today. Empty dates ("from=&to="): any date.
  const from = params.has("from") ? params.get("from") : today;
  const to = params.has("to") ? params.get("to") : today;
  const status = params.get("status") ?? "all";
  const patient = params.get("patient") ?? "";
  const page = Math.max(0, Number(params.get("page")) || 0);

  const [draft, setDraft] = useState({ from, to, status, patient });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => setDraft({ from, to, status, patient }), [from, to, status, patient]);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError("");
    const request = { page, size: PAGE_SIZE };
    if (from) request.from = from;
    if (to) request.to = to;
    if (status !== "all") request.status = status;
    if (patient) request.patient = patient;
    getBills(request)
      .then((response) => current && setResult(response.data))
      .catch((err) => {
        if (!current) return;
        setResult(null);
        setError(getErrorMessage(err, "Could not load the bills."));
      })
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [from, to, status, patient, page]);

  // The dates are always written, so cleared dates ("any date") are kept too
  const show = (filters, pageNumber = 0) => {
    const next = { from: filters.from, to: filters.to };
    if (filters.status !== "all") next.status = filters.status;
    if (filters.patient.trim()) next.patient = filters.patient.trim();
    if (pageNumber > 0) next.page = String(pageNumber);
    setParams(next);
  };

  const change = (field) => (e) => setDraft({ ...draft, [field]: e.target.value });
  const apply = (e) => {
    e.preventDefault();
    show(draft);
  };
  const filters = { from, to, status, patient };
  const entries = result?.entries ?? [];
  const totalPages = result?.totalPages ?? 0;
  const back = location.pathname + location.search;

  return (
    <>
      <PageHeader title="Bills" description="Today's bills by default, newest first." />

      <Card padding="sm" className="mb-4">
        <form onSubmit={apply} aria-label="Filters" className="flex flex-wrap items-end gap-3">
          <Field label="From" htmlFor="bills-from">
            <Input id="bills-from" type="date" value={draft.from} onChange={change("from")} />
          </Field>
          <Field label="To" htmlFor="bills-to">
            <Input id="bills-to" type="date" value={draft.to} onChange={change("to")} />
          </Field>
          <Field label="Status" htmlFor="bills-status">
            <Select id="bills-status" value={draft.status} onChange={change("status")}>
              <option value="all">All</option>
              {Object.entries(BILL_STATUS).map(([value, { label }]) => (
                <option key={value} value={value.toLowerCase()}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Patient or bill number" htmlFor="bills-patient" className="min-w-[16rem] flex-1">
            <Input id="bills-patient" value={draft.patient} onChange={change("patient")} placeholder="e.g. Asha or B-2026-000123" />
          </Field>
          <div className="flex gap-2">
            <Button type="submit">Show</Button>
            <Button variant="secondary" onClick={() => show({ ...filters, from: today, to: today })}>
              Today
            </Button>
            <Button variant="secondary" onClick={() => show({ ...filters, from: "", to: "" })}>
              Any date
            </Button>
          </div>
        </form>
      </Card>

      <Notice tone="danger" className="mb-4">{error}</Notice>

      <Table>
        <thead>
          <tr>
            <Th>Bill</Th>
            <Th>Visit</Th>
            <Th>Patient</Th>
            <Th>Doctor</Th>
            <Th align="right">Total</Th>
            <Th>Status</Th>
          </tr>
        </thead>
        <tbody>
          {entries.map((bill) => (
            <tr key={bill.id}>
              <Td className="whitespace-nowrap">
                <Link to={`${base}/bills/${bill.id}`} state={{ from: back }} className="font-medium text-primary-ink hover:underline">
                  {bill.billNumber}
                </Link>
              </Td>
              <Td className="whitespace-nowrap">{dateLabel(bill.visitDate)}</Td>
              <Td>{bill.patientName}</Td>
              <Td className="text-ink-muted">{bill.doctorName ? `Dr. ${bill.doctorName}` : "—"}</Td>
              <Td align="right" className="whitespace-nowrap font-medium tabular-nums">{formatRupees(bill.total)}</Td>
              <Td>
                <BillStatusBadge status={bill.status} />
              </Td>
            </tr>
          ))}
          {!loading && !error && entries.length === 0 && (
            <TableMessage colSpan={6}>No bills for these filters. Bills are made when a patient checks in.</TableMessage>
          )}
        </tbody>
      </Table>

      <Pagination
        page={page}
        totalPages={totalPages}
        totalEntries={result?.totalEntries}
        noun="bills"
        loading={loading}
        onPage={(number) => show(filters, number)}
      />
    </>
  );
}
