import React, { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import useAreaBase from "../../shared/useAreaBase";
import { formatDate } from "../appointments/appointmentFormat";
import { BillStatusBadge, PAYMENT_METHODS, formatDateTime, hasReceipt, methodLabel, referenceNote } from "./billFormat";
import { downloadReceipt } from "./downloadReceipt";
import * as billingApi from "./api";
import { Download } from "lucide-react";
import { Button, Card, CardHeader, Field, Input, Notice, PageHeader } from "../../shared/ui";

// One bill at the front desk (docs/BILLING_PLAN.md): its lines, extra items and a discount while unpaid, the payment
// (cash, card or UPI), the refund of a cancelled paid visit, and the receipt. /bills/:billId, or
// /bills/visit/:appointmentId from the appointment list; under /admin for admins.


// Cash, card or UPI and an optional reference; used for the payment and for the refund
function MoneyForm({ idPrefix, action, busy, onSubmit }) {
  const [method, setMethod] = useState("CASH");
  const [ref, setRef] = useState("");
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit({ method, reference: ref.trim() || null });
      }}
      className="space-y-4"
    >
      <fieldset>
        <legend className="mb-1.5 text-sm font-medium">Paid with</legend>
        <div className="grid grid-cols-3 gap-2">
          {PAYMENT_METHODS.map(([value, label]) => (
            <label
              key={value}
              className={`cursor-pointer rounded-lg border px-3 py-2 text-center text-sm font-medium transition-colors focus-within:ring-2 focus-within:ring-primary/40 ${
                method === value ? "border-primary bg-primary text-white" : "border-line hover:bg-surface-2"
              }`}
            >
              <input
                type="radio"
                name={`${idPrefix}-method`}
                value={value}
                checked={method === value}
                onChange={() => setMethod(value)}
                className="sr-only"
              />
              {label}
            </label>
          ))}
        </div>
      </fieldset>
      <Field label="Reference (optional)" htmlFor={`${idPrefix}-reference`}>
        <Input
          id={`${idPrefix}-reference`}
          value={ref}
          onChange={(e) => setRef(e.target.value)}
          maxLength={100}
          placeholder="UPI transaction id, card slip no."
        />
      </Field>
      <Button type="submit" variant="success" disabled={busy} className="w-full">
        {action}
      </Button>
    </form>
  );
}

export default function BillPage() {
  const { billId, appointmentId } = useParams();
  const base = useAreaBase();
  const location = useLocation();
  const [bill, setBill] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [item, setItem] = useState({ description: "", amount: "" });
  const [discount, setDiscount] = useState("");

  useEffect(() => {
    setBill(null);
    setError("");
    const load = billId ? billingApi.getBill(billId) : billingApi.getBillForAppointment(appointmentId);
    load
      .then((response) => {
        setBill(response.data);
        setDiscount(String(response.data.discount));
      })
      .catch((err) =>
        setError(
          !billId && err?.response?.status === 404
            ? "This visit has no bill yet: bills are made when the patient checks in."
            : getErrorMessage(err, "Could not load the bill.")
        )
      );
  }, [billId, appointmentId]);

  const run = async (action, describe) => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await action();
      setBill(response.data);
      setDiscount(String(response.data.discount));
      setNotice(describe(response.data));
      return true;
    } catch (err) {
      setError(getErrorMessage(err, "Could not change the bill."));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const addItem = async (e) => {
    e.preventDefault();
    const added = await run(
      () => billingApi.addBillItem(bill.id, { description: item.description, amount: Number(item.amount) }),
      () => `${item.description.trim()} added.`
    );
    if (added) setItem({ description: "", amount: "" });
  };

  const saveReceipt = () => {
    setError("");
    downloadReceipt(() => billingApi.getReceiptPdf(bill.id), bill.billNumber, setError);
  };

  const backTo = location.state?.from || `${base}/bills`;
  const backLabel = `← ${location.state?.fromLabel ?? "Bills"}`;
  const back = (
    <Link to={backTo} className="text-primary-ink hover:underline">
      {backLabel}
    </Link>
  );

  if (!bill) {
    return (
      <>
        <div className="mb-4 text-sm">{back}</div>
        {error ? <Notice tone="danger">{error}</Notice> : <p className="text-ink-muted">Loading...</p>}
      </>
    );
  }

  const unpaid = bill.status === "UNPAID";

  return (
    <div className="max-w-5xl">
      <PageHeader
        back={back}
        title={
          <span className="flex flex-wrap items-center gap-3">
            Bill {bill.billNumber} <BillStatusBadge status={bill.status} />
          </span>
        }
        description={
          <>
            {bill.patientId ? (
              <Link to={`${base}/patients/${bill.patientId}`} className="font-medium text-primary-ink hover:underline">
                {bill.patientName}
              </Link>
            ) : (
              bill.patientName
            )}
            {bill.doctorName && ` · Dr. ${bill.doctorName}`} · {formatDate(bill.visitDate, true)}
          </>
        }
        actions={
          hasReceipt(bill.status) && (
            <Button variant="secondary" onClick={saveReceipt}>
              <Download size={16} aria-hidden="true" />
              Download receipt
            </Button>
          )
        }
      />

      <Notice tone="success" className="mb-4">{notice}</Notice>
      <Notice tone="danger" className="mb-4">{error}</Notice>

      <div className="grid items-start gap-5 lg:grid-cols-5">
        <Card aria-labelledby="bill-lines" className="lg:col-span-3">
          <CardHeader id="bill-lines" title="Items" />
          <table className="w-full text-sm">
            <tbody>
              {bill.items.map((line) => (
                <tr key={line.id} className="border-b border-line">
                  <td className="py-2.5">{line.description}</td>
                  <td className="py-2.5 text-right tabular-nums whitespace-nowrap">{formatRupees(line.amount)}</td>
                  <td className="w-20 py-2.5 pl-3 text-right">
                    {unpaid && !line.consultation && (
                      <button
                        type="button"
                        onClick={() => run(() => billingApi.removeBillItem(bill.id, line.id), () => `${line.description} removed.`)}
                        disabled={busy}
                        aria-label={`Remove ${line.description}`}
                        className="text-xs font-medium text-danger-ink hover:underline disabled:opacity-50"
                      >
                        Remove
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              <tr className="text-ink-muted">
                <td className="pt-3">Subtotal</td>
                <td className="pt-3 text-right tabular-nums">{formatRupees(bill.subtotal)}</td>
                <td />
              </tr>
              {(Number(bill.discount) > 0 || unpaid) && (
                <tr className="text-ink-muted">
                  <td className="pt-1">Discount</td>
                  <td className="pt-1 text-right tabular-nums">− {formatRupees(bill.discount)}</td>
                  <td />
                </tr>
              )}
              <tr className="text-lg font-semibold">
                <td className="pt-3">Total</td>
                <td className="pt-3 text-right tabular-nums" aria-label="Total">
                  {formatRupees(bill.total)}
                </td>
                <td />
              </tr>
            </tbody>
          </table>

          {unpaid && (
            <div className="mt-5 space-y-4 border-t border-line pt-5">
              <form onSubmit={addItem} aria-label="Add an item" className="flex flex-wrap items-end gap-2">
                <Field label="Item" htmlFor="item-description" className="min-w-[10rem] flex-1">
                  <Input
                    id="item-description"
                    value={item.description}
                    onChange={(e) => setItem({ ...item, description: e.target.value })}
                    maxLength={200}
                    placeholder="e.g. Dressing"
                  />
                </Field>
                <Field label="Amount (₹)" htmlFor="item-amount" className="w-32">
                  <Input
                    id="item-amount"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={item.amount}
                    onChange={(e) => setItem({ ...item, amount: e.target.value })}
                    className="text-right"
                  />
                </Field>
                <Button type="submit" variant="secondary" disabled={busy}>
                  Add
                </Button>
              </form>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  run(() => billingApi.setBillDiscount(bill.id, Number(discount)), (b) => `Discount of ${formatRupees(b.discount)} given.`);
                }}
                aria-label="Discount"
                className="flex items-end gap-2"
              >
                <Field label="Discount (₹)" htmlFor="bill-discount" className="w-32">
                  <Input
                    id="bill-discount"
                    type="number"
                    min="0"
                    step="0.01"
                    value={discount}
                    onChange={(e) => setDiscount(e.target.value)}
                    className="text-right"
                  />
                </Field>
                <Button type="submit" variant="secondary" disabled={busy}>
                  Apply
                </Button>
              </form>
            </div>
          )}
        </Card>

        <Card aria-labelledby="bill-payment" className="lg:col-span-2">
          <CardHeader id="bill-payment" title="Payment" />
          {unpaid && (
            <MoneyForm
              idPrefix="payment"
              action={`Record payment of ${formatRupees(bill.total)}`}
              busy={busy}
              onSubmit={(payment) =>
                run(() => billingApi.payBill(bill.id, payment), (b) => `Paid: ${formatRupees(b.total)} by ${methodLabel(b.paymentMethod)}.`)
              }
            />
          )}
          {bill.paidAt && (
            <p className="text-sm">
              Paid {formatRupees(bill.total)} by {methodLabel(bill.paymentMethod)}
              {referenceNote(bill.paymentReference)} on {formatDateTime(bill.paidAt)}
              {bill.paidBy && `, received by ${bill.paidBy}`}.
            </p>
          )}
          {bill.status === "REFUND_DUE" && (
            <div className="mt-4 rounded-lg border border-danger-ink/30 bg-danger-ink/5 p-4">
              <p className="mb-3 font-semibold text-danger-ink">Refund due: {formatRupees(bill.total)}. The visit was cancelled.</p>
              <MoneyForm
                idPrefix="refund"
                action={`Record refund of ${formatRupees(bill.total)}`}
                busy={busy}
                onSubmit={(refund) =>
                  run(() => billingApi.refundBill(bill.id, refund), (b) => `Refunded: ${formatRupees(b.total)} by ${methodLabel(b.refundMethod)}.`)
                }
              />
            </div>
          )}
          {bill.refundedAt && (
            <p className="mt-2 text-sm font-semibold">
              Refunded {formatRupees(bill.total)} by {methodLabel(bill.refundMethod)}
              {referenceNote(bill.refundReference)} on {formatDateTime(bill.refundedAt)}
              {bill.refundedBy && `, by ${bill.refundedBy}`}.
            </p>
          )}
          {bill.status === "CANCELLED" && (
            <p className="text-sm text-ink-muted">
              Cancelled: the patient left the queue or the visit was cancelled. Checking in again reopens it.
            </p>
          )}
        </Card>
      </div>
    </div>
  );
}
