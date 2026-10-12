import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CalendarOff, Plus } from "lucide-react";
import { getRoleFromToken, getUserIdFromToken } from "../../../shared/utils/jwtUtils";
import { getErrorMessage } from "../../../shared/utils/apiError";
import { toLocalISODate } from "../../../shared/utils/dateUtils";
import { formatDate } from "../../appointments/appointmentFormat";
import { getMyLeaves, applyForLeave } from "../api";
import {
  Badge, Button, Card, EmptyState, Field, Input, Modal, PageHeader, Table, Td, Textarea, Th, useToast,
} from "../../../shared/ui";

// The doctor's leave: requests pending an admin's approval, approved ones, and a form to ask for more.
const VIEWS = [
  ["pending", "Pending Leaves"],
  ["approved", "Approved Leaves"],
];
const STATUS = {
  PENDING: { label: "Pending", tone: "warning" },
  APPROVED: { label: "Approved", tone: "success" },
  REJECTED: { label: "Rejected", tone: "danger" },
};
const EMPTY_FORM = { startDate: "", endDate: "", reason: "" };

// Days from start to end, both included
const dayCount = (start, end) => Math.round((new Date(end) - new Date(start)) / 86400000) + 1;

export default function LeaveManagementPage() {
  const toast = useToast();
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewType, setViewType] = useState("pending");
  const [showApplyForm, setShowApplyForm] = useState(false);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [applying, setApplying] = useState(false);

  const navigate = useNavigate();
  const token = localStorage.getItem("jwtToken");
  const role = getRoleFromToken(token);
  const userId = getUserIdFromToken(token);

  // Stable between renders; only changes with the tab (viewType) or the user
  const fetchLeaves = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getMyLeaves(userId, viewType);
      setLeaves(response.data);
    } catch (err) {
      setLeaves([]);
      toast.error(getErrorMessage(err, "Could not load your leave."));
    }
    setLoading(false);
  }, [userId, viewType, toast]);

  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }
    fetchLeaves();
  }, [token, navigate, fetchLeaves]);

  const openForm = () => {
    setFormData(EMPTY_FORM);
    setShowApplyForm(true);
  };

  // Apply for leave; a rejected request (e.g. overlapping dates) ends up in catch with the server's reason
  const handleSubmit = async (e) => {
    e.preventDefault();
    setApplying(true);
    try {
      await applyForLeave({ userId, role, ...formData, reason: formData.reason.trim() });
      toast.success("Leave applied. It's pending until an admin approves it.");
      setShowApplyForm(false);
      fetchLeaves();
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to apply leave"));
    } finally {
      setApplying(false);
    }
  };

  const change = (field) => (e) => setFormData({ ...formData, [field]: e.target.value });

  return (
    <>
      <PageHeader
        title="Leave"
        description="Ask for days off; a request stays pending until an admin approves it."
        actions={
          <Button onClick={openForm}>
            <Plus size={16} aria-hidden="true" />
            Apply for leave
          </Button>
        }
      />

      <div role="group" aria-label="Which leave" className="mb-4 inline-flex rounded-lg border border-line bg-surface p-1 shadow-card">
        {VIEWS.map(([value, label]) => (
          <button
            key={value}
            type="button"
            aria-pressed={viewType === value}
            onClick={() => setViewType(value)}
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
              viewType === value ? "bg-primary text-white" : "text-ink-muted hover:text-ink"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-ink-muted">Loading leaves...</p>
      ) : leaves.length === 0 ? (
        <Card padding="none">
          <EmptyState icon={CalendarOff} title={viewType === "pending" ? "No pending requests" : "No approved leave"} />
        </Card>
      ) : (
        <Table>
          <thead>
            <tr>
              <Th>From</Th>
              <Th>To</Th>
              <Th align="right">Days</Th>
              <Th>Reason</Th>
              <Th>Status</Th>
            </tr>
          </thead>
          <tbody>
            {leaves.map((leave) => {
              const status = STATUS[leave.status] ?? { label: leave.status, tone: "neutral" };
              return (
                <tr key={leave.id}>
                  <Td className="whitespace-nowrap">{formatDate(leave.startDate, true)}</Td>
                  <Td className="whitespace-nowrap">{formatDate(leave.endDate, true)}</Td>
                  <Td align="right" className="tabular-nums">{dayCount(leave.startDate, leave.endDate)}</Td>
                  <Td className="text-ink-muted">{leave.reason}</Td>
                  <Td>
                    <Badge tone={status.tone}>{status.label}</Badge>
                  </Td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}

      {showApplyForm && (
        <Modal
          title="Apply for leave"
          description="The request stays pending until an admin approves it; you're notified when it changes."
          onClose={() => setShowApplyForm(false)}
          closeOnBackdrop={false}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="From" htmlFor="leave-start">
                <Input id="leave-start" type="date" min={toLocalISODate()} value={formData.startDate} onChange={change("startDate")} required />
              </Field>
              <Field label="To" htmlFor="leave-end">
                <Input
                  id="leave-end"
                  type="date"
                  min={formData.startDate || toLocalISODate()}
                  value={formData.endDate}
                  onChange={change("endDate")}
                  required
                />
              </Field>
            </div>
            <Field label="Reason" htmlFor="leave-reason">
              <Textarea id="leave-reason" rows={3} value={formData.reason} onChange={change("reason")} required />
            </Field>
            <div className="flex justify-end gap-2 border-t border-line pt-4">
              <Button variant="secondary" onClick={() => setShowApplyForm(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={applying}>
                {applying ? "Applying..." : "Apply"}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
