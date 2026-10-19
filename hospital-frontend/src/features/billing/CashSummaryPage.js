import React, { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Download, LockKeyhole } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { formatRupees } from "../../shared/utils/money";
import useAreaBase from "../../shared/useAreaBase";
import { formatDate } from "../appointments/appointmentFormat";
import { downloadPdf } from "./downloadReceipt";
import { methodLabel } from "./billFormat";
import * as billingApi from "./api";
import {
  Badge, Button, Card, CardHeader, Field, Input, Modal, Notice, PageHeader, Table, TableMessage, Td, Textarea, Th, useToast,
} from "../../shared/ui";

// The daily cash summary (docs/DAILY_CASH_SUMMARY_PLAN.md): what the desk took and gave back on a day, per method and
// per person, every payment and refund, the visits still unpaid, and closing the day: the cash counted against what
// the drawer should hold since the last close. The desk sees today (/cash-summary); admins any day (/admin/cash-summary).

const METHOD_NAMES = { CASH: "Cash", CARD: "Card", UPI: "UPI", ONLINE: "Online" };
const time = (iso) => (iso ? iso.slice(11, 16) : "");
const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

// A close's difference (counted - expected) as the desk reads it, never by colour alone
export const differenceOf = (difference) => {
  const amount = Number(difference);
  if (amount === 0) return { label: "Matches", tone: "success" };
  return amount < 0
    ? { label: `${formatRupees(-amount)} short`, tone: "danger" }
    : { label: `${formatRupees(amount)} over`, tone: "warning" };
};

// "since 09:30" today, "since Mon 18 Oct, 19:05" on an earlier day
const sinceLabel = (from, today) => (from.slice(0, 10) === today ? `since ${time(from)}` : `since ${formatDate(from.slice(0, 10))}, ${time(from)}`);

export default function CashSummaryPage() {
  const base = useAreaBase();
  const admin = base === "/admin";
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const today = toLocalISODate();
  const date = (admin && params.get("date")) || today;

  const [summary, setSummary] = useState(null);
  const [error, setError] = useState("");
  const [closing, setClosing] = useState(false);
  const [pickedDay, setPickedDay] = useState(date);

  useEffect(() => setPickedDay(date), [date]);

  const load = useCallback(() => {
    let current = true;
    setError("");
    (admin ? billingApi.getCashSummaryFor(date) : billingApi.getCashSummaryToday())
      .then((res) => current && setSummary(res.data))
      .catch((err) => {
        if (!current) return;
        setSummary(null);
        setError(getErrorMessage(err, "Could not load the day's summary."));
      });
    return () => {
      current = false;
    };
  }, [admin, date]);

  useEffect(() => load(), [load]);

  const downloadSummary = () =>
    downloadPdf(
      admin ? () => billingApi.getCashSummaryPdfFor(date) : billingApi.getCashSummaryPdfToday,
      `day-summary-${date}.pdf`,
      toast.error,
      "Could not download the day's summary."
    );

  const showDay = (e) => {
    e.preventDefault();
    if (pickedDay) setParams(pickedDay === today ? {} : { date: pickedDay });
  };

  const closed = (close) => {
    setClosing(false);
    const { label } = differenceOf(close.difference);
    toast.success(Number(close.difference) === 0 ? "The day is closed: the cash matches." : `The day is closed: ${label}.`);
    load();
  };

  return (
    <>
      <PageHeader
        title="Day summary"
        description={`${date === today ? "Today, " : ""}${formatDate(date, true)}: what came in and went out at the desk.`}
        actions={
          <Button variant="secondary" onClick={downloadSummary} disabled={!summary}>
            <Download size={16} aria-hidden="true" />
            Download PDF
          </Button>
        }
      />

      {admin && (
        <Card className="mb-6">
          <form onSubmit={showDay} className="flex flex-wrap items-end gap-3">
            <Field label="Day" htmlFor="cash-day">
              <Input id="cash-day" type="date" max={today} value={pickedDay} onChange={(e) => setPickedDay(e.target.value)} />
            </Field>
            <Button type="submit">Show</Button>
            {date !== today && (
              <Button variant="secondary" onClick={() => setParams({})}>
                Today
              </Button>
            )}
          </form>
        </Card>
      )}

      {error && (
        <div className="mb-6">
          <Notice tone="danger">{error}</Notice>
          <Button variant="secondary" className="mt-3" onClick={load}>
            Try again
          </Button>
        </div>
      )}
      {!summary && !error && <p className="text-ink-muted">Loading the day's summary...</p>}

      {summary && (
        <div className="space-y-6">
          <Totals totals={summary.totals} />
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <div className="min-w-0 space-y-6 lg:col-span-2">
              <Methods methods={summary.methods} totals={summary.totals} />
              <Lines lines={summary.lines} base={base} today={date === today} />
            </div>
            <div className="min-w-0 space-y-6">
              <CloseCard summary={summary} today={today} onClose={() => setClosing(true)} />
              <People people={summary.people} />
              {summary.unpaid.bills > 0 && (
                <Notice tone="warning">
                  Still unpaid: {plural(summary.unpaid.bills, "bill")} of {date === today ? "today's" : "the day's"} visits,{" "}
                  {formatRupees(summary.unpaid.amount)}.{" "}
                  <Link to={`${base}/bills?from=${date}&to=${date}&status=unpaid`} className="font-medium underline">
                    See them
                  </Link>
                </Notice>
              )}
            </div>
          </div>
        </div>
      )}

      {closing && summary?.sinceLastClose && (
        <CloseDialog since={summary.sinceLastClose} today={today} onClosed={closed} onCancel={() => setClosing(false)} />
      )}
    </>
  );
}

function Totals({ totals }) {
  const tiles = [
    { label: "Taken", value: formatRupees(totals.taken), note: plural(totals.payments, "payment") },
    { label: "Given back", value: formatRupees(totals.refunded), note: plural(totals.refunds, "refund") },
    { label: "Net", value: formatRupees(totals.net), note: "taken less given back" },
  ];
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {tiles.map((tile) => (
        <div key={tile.label} className="rounded-xl border border-line bg-surface p-5 shadow-card">
          <p className="text-sm text-ink-muted">{tile.label}</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums text-ink">{tile.value}</p>
          <p className="mt-1 text-sm text-ink-muted">{tile.note}</p>
        </div>
      ))}
    </div>
  );
}

function Methods({ methods, totals }) {
  return (
    <Card aria-labelledby="cash-methods" padding="none">
      <CardHeader id="cash-methods" title="By method" className="px-5 pt-5" />
      <Table plain>
        <thead>
          <tr>
            <Th>Method</Th>
            <Th align="right">Taken</Th>
            <Th align="right">Given back</Th>
            <Th align="right">Net</Th>
          </tr>
        </thead>
        <tbody>
          {methods.map((m) => (
            <tr key={m.method}>
              <Td className="font-medium">{METHOD_NAMES[m.method] ?? m.method}</Td>
              <Td align="right" className="whitespace-nowrap tabular-nums">
                {formatRupees(m.taken)} <span className="text-ink-muted">({m.payments})</span>
              </Td>
              <Td align="right" className="whitespace-nowrap tabular-nums">
                {formatRupees(m.refunded)} <span className="text-ink-muted">({m.refunds})</span>
              </Td>
              <Td align="right" className="whitespace-nowrap font-medium tabular-nums">{formatRupees(m.net)}</Td>
            </tr>
          ))}
          <tr className="bg-surface-2">
            <Td className="font-semibold">Total</Td>
            <Td align="right" className="whitespace-nowrap font-semibold tabular-nums">{formatRupees(totals.taken)}</Td>
            <Td align="right" className="whitespace-nowrap font-semibold tabular-nums">{formatRupees(totals.refunded)}</Td>
            <Td align="right" className="whitespace-nowrap font-semibold tabular-nums">{formatRupees(totals.net)}</Td>
          </tr>
        </tbody>
      </Table>
    </Card>
  );
}

function Lines({ lines, base, today }) {
  return (
    <Card aria-labelledby="cash-lines" padding="none">
      <CardHeader id="cash-lines" title="Payments and refunds" description="In the order they happened." className="px-5 pt-5" />
      <Table plain>
        <thead>
          <tr>
            <Th>Time</Th>
            <Th>Bill</Th>
            <Th>Patient</Th>
            <Th>Method</Th>
            <Th>By</Th>
            <Th align="right">Amount</Th>
          </tr>
        </thead>
        <tbody>
          {lines.map((line) => {
            const refund = line.kind === "REFUND";
            return (
              <tr key={`${line.kind}-${line.billId}`}>
                <Td className="whitespace-nowrap tabular-nums">{time(line.at)}</Td>
                <Td className="whitespace-nowrap">
                  <Link to={`${base}/bills/${line.billId}`} className="font-medium text-primary-ink hover:underline">
                    {line.billNumber}
                  </Link>
                  {refund && (
                    <Badge tone="neutral" className="ml-2">
                      Refund
                    </Badge>
                  )}
                </Td>
                <Td>{line.patientName}</Td>
                <Td className="whitespace-nowrap">
                  {METHOD_NAMES[line.method] ?? methodLabel(line.method)}
                  {line.reference && <span className="text-ink-muted"> ({line.reference})</span>}
                </Td>
                <Td className="text-ink-muted">{line.by ?? "Online"}</Td>
                <Td align="right" className="whitespace-nowrap font-medium tabular-nums">
                  {refund ? `-${formatRupees(line.amount)}` : formatRupees(line.amount)}
                </Td>
              </tr>
            );
          })}
          {lines.length === 0 && (
            <TableMessage colSpan={6}>{today ? "No payments or refunds yet today." : "No payments or refunds on this day."}</TableMessage>
          )}
        </tbody>
      </Table>
    </Card>
  );
}

function People({ people }) {
  return (
    <Card aria-labelledby="cash-people">
      <CardHeader id="cash-people" title="Who took it" />
      {people.length === 0 && <p className="text-sm text-ink-muted">Nobody yet.</p>}
      <ul className="divide-y divide-line">
        {people.map((person) => (
          <li key={person.by ?? "online"} className="py-3 first:pt-0 last:pb-0">
            <p className="font-medium">{person.by ?? "Online (the patient)"}</p>
            <p className="text-sm text-ink-muted">
              {plural(person.payments, "payment")}, {formatRupees(person.taken)}
              {person.refunds > 0 && ` · ${plural(person.refunds, "refund")}, ${formatRupees(person.refunded)}`}
            </p>
          </li>
        ))}
      </ul>
    </Card>
  );
}

// What the drawer should hold since the last close (today only), the button to close, and the day's closes
function CloseCard({ summary, today, onClose }) {
  const since = summary.sinceLastClose;
  if (!since && summary.closes.length === 0) {
    return (
      <Card aria-labelledby="cash-closes">
        <CardHeader id="cash-closes" title="Closes" />
        <p className="text-sm text-ink-muted">The day wasn't closed.</p>
      </Card>
    );
  }
  return (
    <Card aria-labelledby="cash-closes">
      <CardHeader id="cash-closes" title={since ? "Close the day" : "Closes"} />
      {since && (
        <div className="rounded-xl bg-primary-ink/10 p-4">
          <p className="text-sm text-ink-muted">Cash in the drawer {sinceLabel(since.from, today)}</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{formatRupees(since.expected)}</p>
          <p className="mt-1 text-sm text-ink-muted">
            {formatRupees(since.cashIn)} taken, {formatRupees(since.cashOut)} given back
          </p>
          <Button className="mt-4 w-full" onClick={onClose}>
            <LockKeyhole size={16} aria-hidden="true" />
            Close the day...
          </Button>
        </div>
      )}
      {summary.closes.length > 0 && (
        <ul className={`divide-y divide-line ${since ? "mt-4" : ""}`} aria-label="The day's closes">
          {summary.closes.map((close) => {
            const { label, tone } = differenceOf(close.difference);
            return (
              <li key={close.id} className="py-3 first:pt-0 last:pb-0">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="font-medium">
                    Closed at {time(close.closedAt)} by {close.closedBy}
                  </p>
                  <Badge tone={tone}>{label}</Badge>
                </div>
                <p className="text-sm text-ink-muted">
                  Expected {formatRupees(close.expected)}, counted {formatRupees(close.counted)}
                </p>
                {close.note && <p className="mt-1 text-sm">“{close.note}”</p>}
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}

// Counting the cash: the difference shows as it is typed, and a note is asked for when it isn't zero
function CloseDialog({ since, today, onClosed, onCancel }) {
  const [counted, setCounted] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const entered = counted !== "" && !Number.isNaN(Number(counted));
  const difference = entered ? Math.round((Number(counted) - Number(since.expected)) * 100) / 100 : 0;
  const needsNote = entered && difference !== 0;

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (needsNote && !note.trim()) {
      setError("The cash counted isn't what was expected. Please say why.");
      return;
    }
    setBusy(true);
    try {
      const res = await billingApi.closeTheDay({ counted: Number(counted), note: note.trim() || null });
      onClosed(res.data);
    } catch (err) {
      setBusy(false);
      setError(getErrorMessage(err, "The day couldn't be closed. Please try again."));
    }
  };

  return (
    <Modal title="Close the day" description={`Count the cash taken ${sinceLabel(since.from, today)}, without the change float.`} onClose={onCancel} closeOnBackdrop={false}>
      <form onSubmit={submit} className="space-y-4">
        <p className="text-sm">
          The drawer should hold <strong className="tabular-nums">{formatRupees(since.expected)}</strong>: {formatRupees(since.cashIn)} taken,{" "}
          {formatRupees(since.cashOut)} given back.
        </p>
        <Field label="Cash counted (₹)" htmlFor="cash-counted">
          <Input
            id="cash-counted"
            type="number"
            inputMode="decimal"
            min="0"
            step="0.01"
            required
            value={counted}
            onChange={(e) => setCounted(e.target.value)}
          />
        </Field>
        {entered && (
          <p role="status" className="text-sm">
            <Badge tone={differenceOf(difference).tone}>{differenceOf(difference).label}</Badge>
          </p>
        )}
        {needsNote && (
          <Field label="Why is it different?" htmlFor="cash-note">
            <Textarea id="cash-note" rows={3} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
          </Field>
        )}
        <Notice tone="danger">{error}</Notice>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy || !entered}>
            {busy ? "Closing..." : "Close the day"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
