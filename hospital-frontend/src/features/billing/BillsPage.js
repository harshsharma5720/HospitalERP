import React, { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { formatRupees } from "../../shared/utils/money";
import useAreaBase from "../../shared/useAreaBase";
import { dayLabel, formatDate } from "../appointments/appointmentFormat";
import { BILL_STATUS, BillStatusBadge } from "./billFormat";
import { getBills } from "./api";

// The front desk's bills (docs/BILLING_PLAN.md): today's by default; dates, status and patient (or bill number)
// filters; newest first, 20 per page. The filters live in the address, so coming back from a bill shows the same
// list. Receptionists: /bills, admins: /admin/bills.

const PAGE_SIZE = 20;
const INPUT =
  "h-10 px-2 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2]";
const QUIET_BUTTON = "border dark:border-gray-600 rounded";

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
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-4">Bills</h1>

      <form onSubmit={apply} aria-label="Filters" className="flex flex-wrap items-end gap-3 mb-4">
        <div className="flex flex-col text-sm">
          <label htmlFor="bills-from">From</label>
          <input id="bills-from" type="date" value={draft.from} onChange={change("from")} className={INPUT} />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="bills-to">To</label>
          <input id="bills-to" type="date" value={draft.to} onChange={change("to")} className={INPUT} />
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="bills-status">Status</label>
          <select id="bills-status" value={draft.status} onChange={change("status")} className={INPUT}>
            <option value="all">All</option>
            {Object.entries(BILL_STATUS).map(([value, { label }]) => (
              <option key={value} value={value.toLowerCase()}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col text-sm">
          <label htmlFor="bills-patient">Patient or bill number</label>
          <input
            id="bills-patient"
            value={draft.patient}
            onChange={change("patient")}
            placeholder="e.g. Asha or B-2026-000123"
            className={`${INPUT} w-64`}
          />
        </div>
        <button type="submit" className="h-10 bg-blue-600 hover:bg-blue-700 text-white px-4 rounded">
          Show
        </button>
        <button type="button" onClick={() => show({ ...filters, from: today, to: today })} className={`${QUIET_BUTTON} h-10 px-4`}>
          Today
        </button>
        <button type="button" onClick={() => show({ ...filters, from: "", to: "" })} className={`${QUIET_BUTTON} h-10 px-4`}>
          Any date
        </button>
      </form>

      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full border-collapse bg-white dark:bg-[#1e293b] shadow-md rounded-lg text-sm">
          <thead>
            <tr className="bg-gray-100 dark:bg-gray-800 border-b dark:border-gray-600">
              <th className="p-3 text-left">Bill</th>
              <th className="p-3 text-left">Visit</th>
              <th className="p-3 text-left">Patient</th>
              <th className="p-3 text-left">Doctor</th>
              <th className="p-3 text-right">Total</th>
              <th className="p-3 text-left">Status</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((bill) => (
              <tr key={bill.id} className="border-b dark:border-gray-700">
                <td className="p-3 whitespace-nowrap">
                  <Link
                    to={`${base}/bills/${bill.id}`}
                    state={{ from: back }}
                    className="font-semibold text-blue-700 dark:text-[#50d4f2] hover:underline"
                  >
                    {bill.billNumber}
                  </Link>
                </td>
                <td className="p-3 whitespace-nowrap">{dateLabel(bill.visitDate)}</td>
                <td className="p-3">{bill.patientName}</td>
                <td className="p-3">{bill.doctorName ? `Dr. ${bill.doctorName}` : "—"}</td>
                <td className="p-3 text-right whitespace-nowrap tabular-nums">{formatRupees(bill.total)}</td>
                <td className="p-3">
                  <BillStatusBadge status={bill.status} />
                </td>
              </tr>
            ))}
            {!loading && !error && entries.length === 0 && (
              <tr>
                <td colSpan={6} className="p-6 text-center text-gray-500">
                  No bills for these filters. Bills are made when a patient checks in.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-3 mt-4">
        <button
          type="button"
          onClick={() => show(filters, page - 1)}
          disabled={loading || page === 0}
          className={`${QUIET_BUTTON} px-3 py-1 disabled:opacity-50`}
        >
          Previous
        </button>
        <span className="text-sm text-gray-700 dark:text-gray-300">
          {loading ? "Loading..." : `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} · ${result?.totalEntries ?? 0} bills`}
        </span>
        <button
          type="button"
          onClick={() => show(filters, page + 1)}
          disabled={loading || page + 1 >= totalPages}
          className={`${QUIET_BUTTON} px-3 py-1 disabled:opacity-50`}
        >
          Next
        </button>
      </div>
    </div>
  );
}
