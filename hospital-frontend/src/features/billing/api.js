import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the billing feature (docs/BILLING_PLAN.md): the front desk's bills, and the patient's own
// (docs/PATIENT_BILLS_PLAN.md). The login token is added by
// app/setupAxios. Every change answers the whole bill: { id, billNumber, appointmentId, patientId, patientName,
// doctorName, visitDate, status, items: [{ id, description, amount, consultation }], subtotal, discount, total,
// createdAt, paidAt, paymentMethod, paymentReference, paidBy, refundedAt, refundMethod, refundReference, refundedBy }.

const BILLS = `${API_BASE_URL}/api/receptionist/bills`;

// params, all optional: from, to (yyyy-MM-dd), status (unpaid, paid, refund_due, refunded, cancelled, all),
// patient (part of the name, or a bill number), page (from 0), size. Newest first.
export const getBills = (params) => axios.get(BILLS, { params });

export const getBill = (billId) => axios.get(`${BILLS}/${billId}`);

// The bill of a checked-in visit; 404 before check-in
export const getBillForAppointment = (appointmentId) => axios.get(`${BILLS}/by-appointment/${appointmentId}`);

// Unpaid bills only. item: { description, amount }
export const addBillItem = (billId, item) => axios.post(`${BILLS}/${billId}/items`, item);

export const removeBillItem = (billId, itemId) => axios.delete(`${BILLS}/${billId}/items/${itemId}`);

export const setBillDiscount = (billId, discount) => axios.put(`${BILLS}/${billId}/discount`, { discount });

// payment: { method: CASH | CARD | UPI, reference (optional) }
export const payBill = (billId, payment) => axios.post(`${BILLS}/${billId}/pay`, payment);

// A cancelled visit's money given back: { method, reference }
export const refundBill = (billId, refund) => axios.post(`${BILLS}/${billId}/refund`, refund);

// A cancelled visit paid online (paymentMethod ONLINE): refunded through Razorpay to the account it came from
// (docs/ONLINE_PAYMENT_PLAN.md)
export const refundBillOnline = (billId) => axios.post(`${BILLS}/${billId}/refund-online`);

export const getReceiptPdf = (billId) => axios.get(`${BILLS}/${billId}/receipt.pdf`, { responseType: "blob" });

// ---- The daily cash summary (docs/DAILY_CASH_SUMMARY_PLAN.md): { date, methods: [{ method, payments, taken, refunds,
// refunded, net }] (CASH, CARD, UPI, ONLINE), totals: { payments, taken, refunds, refunded, net }, people: [{ by (null:
// online, by the patient), payments, taken, refunds, refunded }], lines: [{ at, kind: PAYMENT | REFUND, billId,
// billNumber, patientName, doctorName, method, reference, by, amount }], unpaid: { bills, amount }, closes: [close],
// sinceLastClose (today only): { from, payments, cashIn, refunds, cashOut, expected } }. A close: { id, periodStart,
// closedAt, closedBy, cashIn, cashOut, expected, counted, difference (counted - expected), note }.
const DESK_CASH = `${API_BASE_URL}/api/receptionist`;

export const getCashSummaryToday = () => axios.get(`${DESK_CASH}/cash-summary`);

export const getCashSummaryPdfToday = () => axios.get(`${DESK_CASH}/cash-summary/today.pdf`, { responseType: "blob" });

// close: { counted, note } - a note is needed when the count isn't what was expected; the close made
export const closeTheDay = (close) => axios.post(`${DESK_CASH}/cash-closes`, close);

// Admins: any day up to today (yyyy-MM-dd)
const ADMIN_CASH = `${API_BASE_URL}/api/admin/cash-summary`;

export const getCashSummaryFor = (date) => axios.get(ADMIN_CASH, { params: { date } });

export const getCashSummaryPdfFor = (date) => axios.get(`${ADMIN_CASH}/pdf`, { params: { date }, responseType: "blob" });

// ---- The logged-in patient's own bills: their visits' and their family members', newest first, cancelled ones left
// out. Each bill as above, without patientId, paidBy, refundedBy, createdAt and the items' id and consultation.
const MY_BILLS = `${API_BASE_URL}/api/bills/my`;

export const getMyBills = () => axios.get(MY_BILLS);

export const getMyReceiptPdf = (billId) => axios.get(`${MY_BILLS}/${billId}/receipt.pdf`, { responseType: "blob" });

// Paying one of them online (docs/ONLINE_PAYMENT_PLAN.md; bills have payOnline). First the Razorpay order for Checkout:
// { keyId, orderId, amount (paise), currency, name, description, prefill: { name, email, contact } }; then Checkout's
// answer { orderId, paymentId, signature } → the paid bill (409 with the reason when it couldn't be applied).
export const startOnlinePayment = (billId) => axios.post(`${MY_BILLS}/${billId}/online-payment`);

export const confirmOnlinePayment = (billId, answer) => axios.post(`${MY_BILLS}/${billId}/online-payment/confirm`, answer);
