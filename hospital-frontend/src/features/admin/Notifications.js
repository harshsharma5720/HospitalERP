import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";
import { Badge, Button, Card, Field, Notice, PageHeader, Pagination, Select, Table, TableMessage, Td, Th } from "../../shared/ui";

// Appointment emails and SMS in the outbox (docs/RELIABLE_NOTIFICATIONS_PLAN.md): what couldn't be delivered,
// with a Resend button. Failed messages are shown first; the filter shows the others.
export const STATUS_LABELS = {
  FAILED: "Failed",
  PENDING: "Waiting to retry",
  SENDING: "Sending",
  SENT: "Sent",
  SKIPPED: "Skipped",
};

// Badge tones: the badge always carries its label; the colour only supports it
const STATUS_TONES = {
  FAILED: "danger",
  PENDING: "warning",
  SENDING: "info",
  SENT: "success",
  SKIPPED: "neutral",
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
    <>
      <PageHeader
        title="Notifications"
        description="Appointment emails and SMS. A message that can't be sent is retried for up to 24 hours (never after the appointment); after that it is marked failed and can be sent again here."
      />

      <Card padding="sm" className="mb-4">
        <Field label="Show" htmlFor="notification-status" className="max-w-xs">
          <Select id="notification-status" value={status} onChange={changeStatus}>
            {Object.entries(STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
            <option value="">All messages</option>
          </Select>
        </Field>
      </Card>

      <Notice tone="success" className="mb-4">{notice}</Notice>
      <Notice tone="danger" className="mb-4">{error}</Notice>

      <Table>
        <thead>
          <tr>
            <Th>Queued</Th>
            <Th>Message</Th>
            <Th>To</Th>
            <Th>Status</Th>
            <Th align="right">Attempts</Th>
            <Th>Last error</Th>
            <Th align="right">Action</Th>
          </tr>
        </thead>
        <tbody>
          {entries.map((message) => (
            <tr key={message.id}>
              <Td className="whitespace-nowrap tabular-nums text-ink-muted">{formatTime(message.createdAt)}</Td>
              <Td>
                {message.description}
                <span className="ml-2 text-xs text-ink-muted">{message.channel === "SMS" ? "SMS" : "Email"}</span>
              </Td>
              <Td className="tabular-nums">{message.recipient}</Td>
              <Td>
                <Badge tone={STATUS_TONES[message.status] ?? "neutral"}>{STATUS_LABELS[message.status] ?? message.status}</Badge>
                {message.status === "PENDING" && (
                  <span className="mt-1 block text-xs text-ink-muted">next try {formatTime(message.nextAttemptAt)}</span>
                )}
                {message.status === "SENT" && <span className="mt-1 block text-xs text-ink-muted">{formatTime(message.sentAt)}</span>}
              </Td>
              <Td align="right" className="tabular-nums">{message.attempts}</Td>
              <Td className="max-w-xs truncate text-ink-muted" title={message.lastError ?? ""}>
                {message.lastError ?? "—"}
              </Td>
              <Td align="right">
                {RESENDABLE.has(message.status) && (
                  <Button size="sm" variant="secondary" onClick={() => resend(message)}>
                    Resend
                  </Button>
                )}
              </Td>
            </tr>
          ))}
          {!loading && !error && entries.length === 0 && (
            <TableMessage colSpan={7}>
              {status === "FAILED" ? "Nothing failed — every message was delivered or is still being tried." : "No messages here."}
            </TableMessage>
          )}
        </tbody>
      </Table>

      <Pagination page={page} totalPages={totalPages} totalEntries={result?.totalEntries} noun="messages" loading={loading} onPage={setPage} />
    </>
  );
}
