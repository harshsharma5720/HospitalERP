import React, { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import useAreaBase from "../../shared/useAreaBase";
import { formatDate } from "../appointments/appointmentFormat";
import { BillStatusBadge, PAYMENT_METHODS, methodLabel } from "./billFormat";
import * as billingApi from "./api";

// One bill at the front desk (docs/BILLING_PLAN.md): its lines, extra items and a discount while unpaid, the payment
// (cash, card or UPI), the refund of a cancelled paid visit, and the receipt. /bills/:billId, or
// /bills/visit/:appointmentId from the appointment list; under /admin for admins.

const CARD = "bg-white dark:bg-[#1e293b] shadow-md rounded-lg p-5 mb-5";
const INPUT =
  "h-10 px-2 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2]";

// "2026-10-12T10:05:31" -> "Mon 12 Oct 2026, 10:05"
const formatDateTime = (time) => (time ? `${formatDate(time.slice(0, 10), true)}, ${time.slice(11, 16)}` : "");

const reference = (ref) => (ref ? ` (ref. ${ref})` : "");

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
      className="flex flex-wrap items-end gap-4"
    >
      <fieldset>
        <legend className="text-sm mb-1">Paid with</legend>
        <div className="flex gap-2">
          {PAYMENT_METHODS.map(([value, label]) => (
            <label
              key={value}
              className={`cursor-pointer border rounded px-3 py-2 text-sm ${
                method === value ? "bg-blue-600 text-white border-blue-600" : "border-gray-300 dark:border-gray-600"
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
      <div className="flex flex-col text-sm">
        <label htmlFor={`${idPrefix}-reference`}>Reference (optional)</label>
        <input
          id={`${idPrefix}-reference`}
          value={ref}
          onChange={(e) => setRef(e.target.value)}
          maxLength={100}
          placeholder="UPI transaction id, card slip no."
          className={`${INPUT} w-64`}
        />
      </div>
      <button type="submit" disabled={busy} className="h-10 bg-green-600 hover:bg-green-700 text-white px-4 rounded disabled:opacity-50">
        {action}
      </button>
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

  const downloadReceipt = async () => {
    setError("");
    try {
      const response = await billingApi.getReceiptPdf(bill.id);
      const url = URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `receipt-${bill.billNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err) {
      setError(getErrorMessage(err, "Could not download the receipt."));
    }
  };

  const backTo = location.state?.from || `${base}/bills`;
  const backLabel = `← ${location.state?.fromLabel ?? "Bills"}`;
  const back = (
    <Link to={backTo} className="text-sm text-blue-700 dark:text-[#50d4f2] hover:underline">
      {backLabel}
    </Link>
  );

  if (!bill) {
    return (
      <div className="p-6">
        {back}
        {error ? (
          <p role="alert" className="text-red-600 mt-4">
            {error}
          </p>
        ) : (
          <p className="mt-4">Loading...</p>
        )}
      </div>
    );
  }

  const unpaid = bill.status === "UNPAID";
  const hasReceipt = ["PAID", "REFUND_DUE", "REFUNDED"].includes(bill.status);

  return (
    <div className="p-6 max-w-4xl">
      {back}
      <div className="flex flex-wrap items-center justify-between gap-3 mt-2 mb-5">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-3">
            Bill {bill.billNumber} <BillStatusBadge status={bill.status} />
          </h1>
          <p className="text-gray-600 dark:text-gray-300">
            {bill.patientId ? (
              <Link to={`${base}/patients/${bill.patientId}`} className="text-blue-700 dark:text-[#50d4f2] hover:underline">
                {bill.patientName}
              </Link>
            ) : (
              bill.patientName
            )}
            {bill.doctorName && ` · Dr. ${bill.doctorName}`} · {formatDate(bill.visitDate, true)}
          </p>
        </div>
        {hasReceipt && (
          <button type="button" onClick={downloadReceipt} className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded">
            Download receipt
          </button>
        )}
      </div>

      {notice && (
        <p role="status" className="text-green-700 dark:text-green-400 mb-4">
          {notice}
        </p>
      )}
      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}

      <section aria-labelledby="bill-lines" className={CARD}>
        <h2 id="bill-lines" className="font-semibold mb-3">
          Items
        </h2>
        <table className="w-full text-sm">
          <tbody>
            {bill.items.map((line) => (
              <tr key={line.id} className="border-b dark:border-gray-700">
                <td className="py-2">{line.description}</td>
                <td className="py-2 text-right tabular-nums whitespace-nowrap">{formatRupees(line.amount)}</td>
                <td className="py-2 pl-3 w-20 text-right">
                  {unpaid && !line.consultation && (
                    <button
                      type="button"
                      onClick={() => run(() => billingApi.removeBillItem(bill.id, line.id), () => `${line.description} removed.`)}
                      disabled={busy}
                      aria-label={`Remove ${line.description}`}
                      className="text-xs underline text-red-700 dark:text-red-400 disabled:opacity-50"
                    >
                      Remove
                    </button>
                  )}
                </td>
              </tr>
            ))}
            <tr>
              <td className="pt-3">Subtotal</td>
              <td className="pt-3 text-right tabular-nums">{formatRupees(bill.subtotal)}</td>
              <td />
            </tr>
            {(Number(bill.discount) > 0 || unpaid) && (
              <tr>
                <td>Discount</td>
                <td className="text-right tabular-nums">− {formatRupees(bill.discount)}</td>
                <td />
              </tr>
            )}
            <tr className="font-bold text-lg">
              <td className="pt-2">Total</td>
              <td className="pt-2 text-right tabular-nums" aria-label="Total">
                {formatRupees(bill.total)}
              </td>
              <td />
            </tr>
          </tbody>
        </table>

        {unpaid && (
          <div className="grid sm:grid-cols-2 gap-6 mt-5">
            <form onSubmit={addItem} aria-label="Add an item" className="flex flex-wrap items-end gap-2">
              <div className="flex flex-col text-sm flex-1 min-w-[10rem]">
                <label htmlFor="item-description">Item</label>
                <input
                  id="item-description"
                  value={item.description}
                  onChange={(e) => setItem({ ...item, description: e.target.value })}
                  maxLength={200}
                  placeholder="e.g. Dressing"
                  className={INPUT}
                />
              </div>
              <div className="flex flex-col text-sm">
                <label htmlFor="item-amount">Amount (₹)</label>
                <input
                  id="item-amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={item.amount}
                  onChange={(e) => setItem({ ...item, amount: e.target.value })}
                  className={`${INPUT} w-28 text-right`}
                />
              </div>
              <button type="submit" disabled={busy} className="h-10 border dark:border-gray-600 px-3 rounded disabled:opacity-50">
                Add
              </button>
            </form>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                run(() => billingApi.setBillDiscount(bill.id, Number(discount)), (b) => `Discount of ${formatRupees(b.discount)} given.`);
              }}
              aria-label="Discount"
              className="flex items-end gap-2"
            >
              <div className="flex flex-col text-sm">
                <label htmlFor="bill-discount">Discount (₹)</label>
                <input
                  id="bill-discount"
                  type="number"
                  min="0"
                  step="0.01"
                  value={discount}
                  onChange={(e) => setDiscount(e.target.value)}
                  className={`${INPUT} w-28 text-right`}
                />
              </div>
              <button type="submit" disabled={busy} className="h-10 border dark:border-gray-600 px-3 rounded disabled:opacity-50">
                Apply
              </button>
            </form>
          </div>
        )}
      </section>

      <section aria-labelledby="bill-payment" className={CARD}>
        <h2 id="bill-payment" className="font-semibold mb-3">
          Payment
        </h2>
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
          <p>
            Paid {formatRupees(bill.total)} by {methodLabel(bill.paymentMethod)}
            {reference(bill.paymentReference)} on {formatDateTime(bill.paidAt)}
            {bill.paidBy && `, received by ${bill.paidBy}`}.
          </p>
        )}
        {bill.status === "REFUND_DUE" && (
          <div className="mt-4 border-l-4 border-red-500 pl-4">
            <p className="font-semibold mb-3">Refund due: {formatRupees(bill.total)}. The visit was cancelled.</p>
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
          <p className="mt-2 font-semibold">
            Refunded {formatRupees(bill.total)} by {methodLabel(bill.refundMethod)}
            {reference(bill.refundReference)} on {formatDateTime(bill.refundedAt)}
            {bill.refundedBy && `, by ${bill.refundedBy}`}.
          </p>
        )}
        {bill.status === "CANCELLED" && (
          <p className="text-gray-600 dark:text-gray-300">
            Cancelled: the patient left the queue or the visit was cancelled. Checking in again reopens it.
          </p>
        )}
      </section>
    </div>
  );
}
