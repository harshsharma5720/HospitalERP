import React from "react";
import { formatRupees } from "../../shared/utils/money";
import { Card, CardHeader, Table, Td, Th } from "../../shared/ui";

// Pieces of the daily cash summary (docs/DAILY_CASH_SUMMARY_PLAN.md) shared by a day's page and the admins' range of
// days, so both read the same.

export const METHOD_NAMES = { CASH: "Cash", CARD: "Card", UPI: "UPI", ONLINE: "Online" };
export const time = (iso) => (iso ? iso.slice(11, 16) : "");
export const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

// A close's difference (counted - expected) as the desk reads it, never by colour alone
export const differenceOf = (difference) => {
  const amount = Number(difference);
  if (amount === 0) return { label: "Matches", tone: "success" };
  return amount < 0
    ? { label: `${formatRupees(-amount)} short`, tone: "danger" }
    : { label: `${formatRupees(amount)} over`, tone: "warning" };
};

export function Totals({ totals }) {
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

export function Methods({ methods, totals }) {
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
