import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import { formatDate } from "../appointments/appointmentFormat";
import { Badge, Card, CardHeader, Notice, Table, TableMessage, Td, Th } from "../../shared/ui";
import { Methods, Totals, differenceOf } from "./cashParts";
import * as billingApi from "./api";

// Several days for admins (docs/DAILY_CASH_SUMMARY_PLAN.md): the range's totals and methods, a row per day (newest
// first) with what the drawer gained and the day's closes, and the closes whose cash didn't match. Each day links to
// its own summary.
export default function CashRange({ from, to }) {
  const [range, setRange] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let current = true;
    setRange(null);
    setError("");
    billingApi
      .getCashRange(from, to)
      .then((res) => current && setRange(res.data))
      .catch((err) => current && setError(getErrorMessage(err, "Could not load these days.")));
    return () => {
      current = false;
    };
  }, [from, to]);

  if (error) return <Notice tone="danger">{error}</Notice>;
  if (!range) return <p className="text-ink-muted">Loading these days...</p>;

  const mismatched = range.closes.filter((close) => Number(close.difference) !== 0);
  return (
    <div className="space-y-6">
      <Totals totals={range.totals} />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="min-w-0 space-y-6 lg:col-span-2">
          <Card aria-labelledby="cash-days" padding="none">
            <CardHeader id="cash-days" title="Day by day" description="Newest first; a day opens its own summary." className="px-5 pt-5" />
            <Table plain>
              <thead>
                <tr>
                  <Th>Day</Th>
                  <Th align="right">Taken</Th>
                  <Th align="right">Given back</Th>
                  <Th align="right">Net</Th>
                  <Th align="right">Cash</Th>
                  <Th>Closes</Th>
                </tr>
              </thead>
              <tbody>
                {[...range.days].reverse().map((day) => (
                  <tr key={day.date}>
                    <Td className="whitespace-nowrap">
                      <Link to={`?date=${day.date}`} className="font-medium text-primary-ink hover:underline">
                        {formatDate(day.date, true)}
                      </Link>
                    </Td>
                    <Td align="right" className="whitespace-nowrap tabular-nums">{formatRupees(day.taken)}</Td>
                    <Td align="right" className="whitespace-nowrap tabular-nums">{formatRupees(day.refunded)}</Td>
                    <Td align="right" className="whitespace-nowrap font-medium tabular-nums">{formatRupees(day.net)}</Td>
                    <Td align="right" className="whitespace-nowrap tabular-nums">{formatRupees(day.cashNet)}</Td>
                    <Td className="whitespace-nowrap">
                      {day.closes === 0 ? (
                        <span className="text-ink-muted">{day.payments + day.refunds > 0 ? "Not closed" : "—"}</span>
                      ) : (
                        <span className="inline-flex items-center gap-2">
                          {day.closes > 1 && <span className="text-sm text-ink-muted">{day.closes} closes</span>}
                          <Badge tone={differenceOf(day.difference).tone}>{differenceOf(day.difference).label}</Badge>
                        </span>
                      )}
                    </Td>
                  </tr>
                ))}
                {range.days.length === 0 && <TableMessage colSpan={6}>No days.</TableMessage>}
              </tbody>
            </Table>
          </Card>
          <Methods methods={range.methods} totals={range.totals} />
        </div>
        <div className="min-w-0 space-y-6">
          <Card aria-labelledby="cash-mismatched">
            <CardHeader id="cash-mismatched" title="Cash that didn't match" />
            {mismatched.length === 0 && <p className="text-sm text-ink-muted">Every close matched.</p>}
            <ul className="divide-y divide-line">
              {mismatched.map((close) => {
                const { label, tone } = differenceOf(close.difference);
                return (
                  <li key={close.id} className="py-3 first:pt-0 last:pb-0">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="font-medium">
                        {formatDate(close.closedAt.slice(0, 10))}, {close.closedAt.slice(11, 16)} · {close.closedBy}
                      </p>
                      <Badge tone={tone}>{label}</Badge>
                    </div>
                    {close.note && <p className="mt-1 text-sm">“{close.note}”</p>}
                  </li>
                );
              })}
            </ul>
          </Card>
        </div>
      </div>
    </div>
  );
}
