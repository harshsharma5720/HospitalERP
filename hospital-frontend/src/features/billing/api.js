import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// Backend calls of the billing feature (docs/BILLING_PLAN.md): the front desk's bills. The login token is added by
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

export const getReceiptPdf = (billId) => axios.get(`${BILLS}/${billId}/receipt.pdf`, { responseType: "blob" });
