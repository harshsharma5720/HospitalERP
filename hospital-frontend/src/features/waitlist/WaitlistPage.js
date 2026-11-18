import React, { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import useAreaBase from "../../shared/useAreaBase";
import { Badge, Button, Card, Field, Input, Notice, PageHeader, Select, Table, TableMessage, Td, Th, useConfirm, useToast } from "../../shared/ui";
import { getAllDoctors } from "../doctors/api";
import { doctorLabel, formatDate } from "../appointments/appointmentFormat";
import { getDeskWaitlist, removeFromWaitlist } from "./api";
import { shiftLabel } from "./waitlistFormat";

// The front desk's waitlist (docs/WAITLIST_PLAN.md): a day (today by default) and a doctor (all by default), in the
// address; who is waiting, by doctor and in the order they joined, with the phone and when they were last told.
// Book opens Walk-in with the patient and the doctor chosen (a family member books in the app); Remove. Someone is
// added from their page under Patients. /waitlist for receptionists, /admin/waitlist for admins.

const STATUS = { WAITING: ["Waiting", "primary"], BOOKED: ["Booked", "success"], ENDED: ["Ended", "neutral"] };

// "10:05" of a time today, else "18 Nov, 10:05"
const when = (dateTime, today) =>
  dateTime.slice(0, 10) === today ? dateTime.slice(11, 16) : `${formatDate(dateTime.slice(0, 10))}, ${dateTime.slice(11, 16)}`;

export default function WaitlistPage() {
  const base = useAreaBase();
  const toast = useToast();
  const confirm = useConfirm();
  const [params, setParams] = useSearchParams();
  const today = toLocalISODate();
  const date = params.get("date") || today;
  const doctorId = params.get("doctorId") || "";
  const [doctors, setDoctors] = useState([]);
  const [waits, setWaits] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getAllDoctors()
      .then((res) => setDoctors(res.data.filter((d) => d.active !== false)))
      .catch(() => setDoctors([]));
  }, []);

  const load = useCallback(() => {
    setError("");
    setWaits(null);
    getDeskWaitlist({ date, doctorId: doctorId || undefined })
      .then((res) => setWaits(res.data))
      .catch((err) => {
        setWaits([]);
        setError(getErrorMessage(err, "Could not load the waitlist."));
      });
  }, [date, doctorId]);

  useEffect(() => {
    load();
  }, [load]);

  const choose = (key) => (e) => {
    const next = new URLSearchParams(params);
    if (e.target.value) next.set(key, e.target.value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  const remove = async (wait) => {
    const ok = await confirm({
      title: `Remove ${wait.name} from the waitlist?`,
      message: `Dr. ${wait.doctorName}, ${formatDate(wait.date, true)}. They won't be told when a time frees up.`,
      confirmLabel: "Remove",
      tone: "danger",
    });
    if (!ok) return;
    try {
      await removeFromWaitlist(wait.id);
      toast.success(`${wait.name} is off the waitlist.`);
      load();
    } catch (err) {
      toast.error(getErrorMessage(err, "Could not remove them from the waitlist."));
    }
  };

  return (
    <div className="max-w-6xl">
      <PageHeader
        title="Waitlist"
        description="Patients waiting for a full day. When a time frees up they're told in the app and by SMS; whoever books first gets it. To add someone, open their page under Patients."
      />

      <Card className="mb-5">
        <div className="grid gap-4 sm:grid-cols-[12rem_1fr]">
          <Field label="Day" htmlFor="waitlist-date">
            <Input id="waitlist-date" type="date" value={date} onChange={choose("date")} />
          </Field>
          <Field label="Doctor" htmlFor="waitlist-doctor">
            <Select id="waitlist-doctor" value={doctorId} onChange={choose("doctorId")}>
              <option value="">All doctors</option>
              {doctors.map((d) => (
                <option key={d.id} value={d.id}>
                  {doctorLabel(d)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
      </Card>

      <Notice tone="danger" className="mb-5">{error}</Notice>

      <Card padding="none">
        <Table plain>
          <thead>
            <tr>
              <Th>Doctor</Th>
              <Th>Waiting</Th>
              <Th>Phone</Th>
              <Th>When</Th>
              <Th>Joined</Th>
              <Th>Told</Th>
              <Th>Status</Th>
              <Th>
                <span className="sr-only">Actions</span>
              </Th>
            </tr>
          </thead>
          <tbody>
            {waits === null && <TableMessage colSpan={8}>Loading...</TableMessage>}
            {waits?.length === 0 && !error && <TableMessage colSpan={8}>Nobody is waiting for {formatDate(date, true)}.</TableMessage>}
            {(waits ?? []).map((wait) => {
              const [label, tone] = STATUS[wait.status] ?? [wait.status, "neutral"];
              return (
                <tr key={wait.id}>
                  <Td className="whitespace-nowrap">Dr. {wait.doctorName}</Td>
                  <Td>
                    <Link to={`${base}/patients/${wait.patientId}`} className="font-medium text-primary-ink hover:underline">
                      {wait.name}
                    </Link>
                    {wait.relativeId && <span className="block text-xs text-ink-muted">family of {wait.patientName}</span>}
                    {!wait.hasLogin && <span className="block text-xs text-ink-muted">front-desk record: SMS only</span>}
                  </Td>
                  <Td className="whitespace-nowrap tabular-nums">{wait.phone || "—"}</Td>
                  <Td className="whitespace-nowrap">{shiftLabel(wait.shift)}</Td>
                  <Td className="whitespace-nowrap text-ink-muted">
                    {when(wait.joinedAt, today)}
                    <span className="block text-xs">{wait.joinedBy}</span>
                  </Td>
                  <Td className="whitespace-nowrap tabular-nums">{wait.toldAt ? when(wait.toldAt, today) : "—"}</Td>
                  <Td>
                    <Badge tone={tone}>{label}</Badge>
                  </Td>
                  <Td className="whitespace-nowrap text-right">
                    {wait.status === "WAITING" && (
                      <div className="flex justify-end gap-2">
                        {!wait.relativeId && (
                          <Button as={Link} to={`${base}/walk-in?patientId=${wait.patientId}&doctorId=${wait.doctorId}`} size="sm" variant="secondary" aria-label={`Book ${wait.name}`}>
                            Book
                          </Button>
                        )}
                        <Button size="sm" variant="danger-ghost" onClick={() => remove(wait)} aria-label={`Remove ${wait.name}`}>
                          Remove
                        </Button>
                      </div>
                    )}
                  </Td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      </Card>
    </div>
  );
}
