import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";

// Appointment emails and SMS in the outbox (docs/RELIABLE_NOTIFICATIONS_PLAN.md): what couldn't be delivered,
// with a Resend button. Failed messages are shown first; the filter shows the others.
export const STATUS_LABELS = {
  FAILED: "Failed",
  PENDING: "Waiting to retry",
  SENDING: "Sending",
  SENT: "Sent",
  SKIPPED: "Skipped",
};

// The badge always carries its label; the color only supports it
const STATUS_STYLES = {
  FAILED: "bg-red-100 text-red-800",
  PENDING: "bg-amber-100 text-amber-800",
  SENDING: "bg-blue-100 text-blue-800",
  SENT: "bg-green-100 text-green-800",
  SKIPPED: "bg-gray-200 text-gray-700",
};

const RESENDABLE = new Set(["FAILED", "SKIPPED"]);
const PAGE_SIZE = 50;

// "2026-10-07T16:40:12.123456" (server time) -> "2026-10-07 16:40"
const formatTime = (time) => (time ? String(time).replace("T", " ").slice(0, 16) : "—");

export default function Notifications() {
  const [status, setStatus] = useState("FAILED"); // "" = all
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const load = useCallback(async (statusFilter, pageNumber) => {
    setLoading(true);
    setError("");
    const params = { page: pageNumber, size: PAGE_SIZE };
    if (statusFilter) params.status = statusFilter;
    try {
      const response = await adminApi.getNotifications(params);
      setResult(response.data);
    } catch (err) {
      setResult(null);
      setError(getErrorMessage(err, "Could not load the notifications."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(status, page);
  }, [load, status, page]);

  const changeStatus = (e) => {
    setNotice("");
    setPage(0);
    setStatus(e.target.value);
  };

  const resend = async (message) => {
    setNotice("");
    try {
      await adminApi.resendNotification(message.id);
      setNotice(`The ${message.channel === "SMS" ? "SMS" : "email"} to ${message.recipient} is queued again and will be sent in a moment.`);
      load(status, page);
    } catch (err) {
      setError(getErrorMessage(err, "Could not resend the message."));
    }
  };

  const entries = result?.entries ?? [];
  const totalPages = result?.totalPages ?? 0;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-2">Notifications</h1>
      <p className="text-gray-600 dark:text-gray-300 mb-4">
        Appointment emails and SMS. A message that can't be sent is retried for up to 24 hours (never after the
        appointment); after that it is marked failed and can be sent again here.
      </p>

      <div className="flex flex-wrap items-end gap-3 mb-4">
        <div className="flex flex-col text-sm">
          <label htmlFor="notification-status">Show</label>
          <select id="notification-status" value={status} onChange={changeStatus} className="border p-2 rounded">
            {Object.entries(STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
            <option value="">All messages</option>
          </select>
        </div>
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

      <div className="overflow-x-auto">
        <table className="w-full border-collapse bg-white dark:bg-[#1e293b] shadow-md rounded-lg text-sm">
          <thead>
            <tr className="bg-gray-100 dark:bg-gray-800 border-b">
              <th className="p-3 text-left">Queued</th>
              <th className="p-3 text-left">Message</th>
              <th className="p-3 text-left">To</th>
              <th className="p-3 text-left">Status</th>
              <th className="p-3 text-right">Attempts</th>
              <th className="p-3 text-left">Last error</th>
              <th className="p-3 text-left">Action</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((message) => (
              <tr key={message.id} className="border-b">
                <td className="p-3 whitespace-nowrap">{formatTime(message.createdAt)}</td>
                <td className="p-3">
                  {message.description}
                  <span className="ml-2 text-xs text-gray-500">{message.channel === "SMS" ? "SMS" : "Email"}</span>
                </td>
                <td className="p-3">{message.recipient}</td>
                <td className="p-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-semibold ${STATUS_STYLES[message.status] ?? ""}`}>
                    {STATUS_LABELS[message.status] ?? message.status}
                  </span>
                  {message.status === "PENDING" && (
                    <span className="block text-xs text-gray-500 mt-1">next try {formatTime(message.nextAttemptAt)}</span>
                  )}
                  {message.status === "SENT" && (
                    <span className="block text-xs text-gray-500 mt-1">{formatTime(message.sentAt)}</span>
                  )}
                </td>
                <td className="p-3 text-right">{message.attempts}</td>
                <td className="p-3 max-w-xs truncate" title={message.lastError ?? ""}>
                  {message.lastError ?? "—"}
                </td>
                <td className="p-3">
                  {RESENDABLE.has(message.status) && (
                    <button
                      type="button"
                      onClick={() => resend(message)}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs"
                    >
                      Resend
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {!loading && !error && entries.length === 0 && (
              <tr>
                <td colSpan={7} className="p-6 text-center text-gray-500">
                  {status === "FAILED" ? "Nothing failed — every message was delivered or is still being tried." : "No messages here."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-3 mt-4">
        <button
          type="button"
          onClick={() => setPage(page - 1)}
          disabled={loading || page === 0}
          className="border px-3 py-1 rounded disabled:opacity-50"
        >
          Previous
        </button>
        <span className="text-sm text-gray-700 dark:text-gray-300">
          {loading
            ? "Loading..."
            : `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} · ${result?.totalEntries ?? 0} messages`}
        </span>
        <button
          type="button"
          onClick={() => setPage(page + 1)}
          disabled={loading || page + 1 >= totalPages}
          className="border px-3 py-1 rounded disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  );
}
