import React, { useEffect, useState } from "react";
import { CalendarCheck } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatDate } from "../appointments/appointmentFormat";
import * as adminApi from "./api";
import { Badge, Button, Card, EmptyState, PageHeader, Table, Td, Th, useConfirm, useToast } from "../../shared/ui";

const TABS = [
  { key: "PENDING", label: "Pending", load: adminApi.getPendingLeaves },
  { key: "APPROVED", label: "Approved", load: adminApi.getApprovedLeaves },
  { key: "REJECTED", label: "Rejected", load: adminApi.getRejectedLeaves },
];
const STATUS = {
  PENDING: { label: "Pending", tone: "warning" },
  APPROVED: { label: "Approved", tone: "success" },
  REJECTED: { label: "Rejected", tone: "danger" },
};
const ROLE = { ROLE_DOCTOR: "Doctor", ROLE_RECEPTIONIST: "Receptionist", ROLE_ADMIN: "Admin" };

// Days from start to end, both included
const dayCount = (start, end) => Math.round((new Date(end) - new Date(start)) / 86400000) + 1;

// Leave requests of the staff: approve or reject the pending ones
export default function LeaveApproval() {
  const toast = useToast();
  const confirm = useConfirm();
  const [leaves, setLeaves] = useState([]);
  const [filter, setFilter] = useState("PENDING"); // default tab
  const [busyId, setBusyId] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchLeaves();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const tab = TABS.find((t) => t.key === filter);
      const response = await tab.load();
      setLeaves(response.data);
    } catch (error) {
      setLeaves([]);
      toast.error(getErrorMessage(error, "Could not load leave requests."));
    } finally {
      setLoading(false);
    }
  };

  // Approving a doctor's leave also cancels their appointments in that period and notifies patients
  const decideLeave = async (leave, decision) => {
    const approve = decision === "approve";
    const ok = await confirm(
      approve
        ? {
            title: "Approve this leave?",
            message: "Appointments in this period will be cancelled and the patients notified.",
            confirmLabel: "Approve",
          }
        : { title: "Reject this leave request?", confirmLabel: "Reject", tone: "danger" }
    );
    if (!ok) return;
    try {
      setBusyId(leave.id);
      await adminApi.decideLeave(leave.id, decision);
      toast.success(approve ? "Leave approved." : "Leave rejected.");
      fetchLeaves();
    } catch (error) {
      toast.error(getErrorMessage(error, "Could not update the leave request."));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <PageHeader
        title="Leave approvals"
        description="Approving a doctor's leave cancels their appointments in that period and notifies the patients."
      />

      <div role="group" aria-label="Which requests" className="mb-4 inline-flex rounded-lg border border-line bg-surface p-1 shadow-card">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            aria-pressed={filter === tab.key}
            onClick={() => setFilter(tab.key)}
            className={`rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
              filter === tab.key ? "bg-primary text-white" : "text-ink-muted hover:text-ink"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-ink-muted">Loading...</p>
      ) : leaves.length === 0 ? (
        <Card padding="none">
          <EmptyState icon={CalendarCheck} title={`No ${filter.toLowerCase()} leave requests`} />
        </Card>
      ) : (
        <Table>
          <thead>
            <tr>
              <Th>Requested by</Th>
              <Th>From</Th>
              <Th>To</Th>
              <Th align="right">Days</Th>
              <Th>Reason</Th>
              <Th>Status</Th>
              {filter === "PENDING" && <Th align="right">Decision</Th>}
            </tr>
          </thead>
          <tbody>
            {leaves.map((leave) => {
              const status = STATUS[leave.status] ?? { label: leave.status, tone: "neutral" };
              return (
                <tr key={leave.id}>
                  <Td className="whitespace-nowrap">
                    <span className="font-medium">User #{leave.userId}</span>
                    <span className="ml-2 text-ink-muted">{ROLE[leave.role] ?? (leave.role || "").replace("ROLE_", "")}</span>
                  </Td>
                  <Td className="whitespace-nowrap">{formatDate(leave.startDate, true)}</Td>
                  <Td className="whitespace-nowrap">{formatDate(leave.endDate, true)}</Td>
                  <Td align="right" className="tabular-nums">{dayCount(leave.startDate, leave.endDate)}</Td>
                  <Td className="text-ink-muted">{leave.reason}</Td>
                  <Td>
                    <Badge tone={status.tone}>{status.label}</Badge>
                  </Td>
                  {filter === "PENDING" && (
                    <Td align="right">
                      <div className="flex justify-end gap-2">
                        <Button size="sm" variant="secondary" disabled={busyId === leave.id} onClick={() => decideLeave(leave, "reject")} aria-label={`Reject leave ${leave.id}`}>
                          Reject
                        </Button>
                        <Button size="sm" variant="success" disabled={busyId === leave.id} onClick={() => decideLeave(leave, "approve")} aria-label={`Approve leave ${leave.id}`}>
                          Approve
                        </Button>
                      </div>
                    </Td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}
    </>
  );
}
