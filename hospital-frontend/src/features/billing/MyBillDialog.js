import React from "react";
import { CreditCard, Download } from "lucide-react";
import { formatRupees } from "../../shared/utils/money";
import { Button, Modal, Notice } from "../../shared/ui";
import { formatDate } from "../appointments/appointmentFormat";
import { BillStatusBadge, formatDateTime, hasReceipt, methodLabel, referenceNote } from "./billFormat";

// One of the patient's bills (docs/PATIENT_BILLS_PLAN.md), from My appointments or My account: the lines, the amounts,
// what was paid or refunded, and the receipt. An unpaid bill can be paid online when it has payOnline and the page
// passes onPay (docs/ONLINE_PAYMENT_PLAN.md)

// What the patient has to do, or what was done, in their words
function PaymentNote({ bill, online }) {
  const total = formatRupees(bill.total);
  return (
    <div className="mt-5 space-y-2 text-sm">
      {bill.status === "UNPAID" && (
        <p className="rounded-lg bg-warning-ink/10 p-3 font-medium text-warning-ink">
          {online ? `Pay ${total} online now, or at the front desk.` : `Please pay ${total} at the front desk.`}
        </p>
      )}
      {bill.paidAt && (
        <p>
          Paid {total} by {methodLabel(bill.paymentMethod)}
          {referenceNote(bill.paymentReference)} on {formatDateTime(bill.paidAt)}.
        </p>
      )}
      {bill.status === "REFUND_DUE" && (
        <p className="rounded-lg bg-danger-ink/10 p-3 font-medium text-danger-ink">
          The visit was cancelled: the front desk will refund {total}
          {bill.paymentMethod === "ONLINE" ? " to the account you paid from." : "."}
        </p>
      )}
      {bill.refundedAt && (
        <p className="font-medium">
          Refunded {total} by {methodLabel(bill.refundMethod)}
          {referenceNote(bill.refundReference)} on {formatDateTime(bill.refundedAt)}.
        </p>
      )}
    </div>
  );
}

export default function MyBillDialog({ bill, onClose, onReceipt, onPay, paying = false, error = "" }) {
  const online = Boolean(bill.payOnline && onPay);
  return (
    <Modal
      title={
        <span className="flex flex-wrap items-center gap-3">
          Bill {bill.billNumber} <BillStatusBadge status={bill.status} />
        </span>
      }
      description={[`For ${bill.patientName}`, bill.doctorName && `Dr. ${bill.doctorName}`, formatDate(bill.visitDate, true)]
        .filter(Boolean)
        .join(" · ")}
      onClose={onClose}
    >
      <ul aria-label="Items" className="divide-y divide-line">
        {bill.items.map((item, i) => (
          <li key={i} className="flex justify-between gap-4 py-2 first:pt-0">
            <span className="min-w-0">{item.description}</span>
            <span className="shrink-0 tabular-nums">{formatRupees(item.amount)}</span>
          </li>
        ))}
      </ul>
      <dl className="mt-2 space-y-1 border-t border-line pt-3 text-sm">
        {Number(bill.discount) > 0 && (
          <>
            <div className="flex justify-between gap-4 text-ink-muted">
              <dt>Subtotal</dt>
              <dd className="tabular-nums">{formatRupees(bill.subtotal)}</dd>
            </div>
            <div className="flex justify-between gap-4 text-ink-muted">
              <dt>Discount</dt>
              <dd className="tabular-nums">− {formatRupees(bill.discount)}</dd>
            </div>
          </>
        )}
        <div className="flex justify-between gap-4 text-base font-semibold">
          <dt>Total</dt>
          <dd className="tabular-nums">{formatRupees(bill.total)}</dd>
        </div>
      </dl>

      <PaymentNote bill={bill} online={online} />
      <Notice tone="danger" className="mt-4">
        {error}
      </Notice>

      {(online || hasReceipt(bill.status)) && (
        <div className="mt-6 flex justify-end">
          {online && (
            <Button onClick={() => onPay(bill)} disabled={paying}>
              <CreditCard size={16} aria-hidden="true" />
              {paying ? "Opening the payment..." : `Pay ${formatRupees(bill.total)} online`}
            </Button>
          )}
          {hasReceipt(bill.status) && (
            <Button onClick={() => onReceipt(bill)}>
              <Download size={16} aria-hidden="true" />
              Download receipt
            </Button>
          )}
        </div>
      )}
    </Modal>
  );
}
