import React, { useState } from "react";
import { BellRing } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Field, Notice, Select, useToast } from "../../shared/ui";
import { formatDate } from "../appointments/appointmentFormat";
import { joinWaitlist, leaveWaitlist } from "./api";
import { shiftLabel, WAIT_SHIFTS } from "./waitlistFormat";

// On a full day of the booking page (docs/WAITLIST_PLAN.md): join the waitlist for the person, the doctor and the day
// - or, already on it, see it and leave it. waits: the patient's waits; onChange() loads them again. The page gives it a
// key per person, doctor, day and shift, so each starts afresh (no reason left over from another).

export default function JoinWaitlist({ person, doctor, date, shift, waits, onChange }) {
  const toast = useToast();
  const [when, setWhen] = useState(shift);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const wait = (waits ?? []).find(
    (w) => String(w.doctorId) === String(doctor.id) && w.date === date && (w.relativeId ?? null) === (person.relativeId ?? null)
  );

  const act = async (call, done) => {
    setBusy(true);
    setError("");
    try {
      await call();
      toast.success(done);
      onChange();
    } catch (err) {
      setError(getErrorMessage(err, "That didn't work. Please try again."));
    } finally {
      setBusy(false);
    }
  };

  const join = () =>
    act(
      () => joinWaitlist({ doctorId: doctor.id, date, shift: when || null, relativeId: person.relativeId }),
      `${person.name} is on the waitlist. We'll tell you when a time frees up.`
    );

  if (wait) {
    return (
      <div className="mt-4 rounded-xl border border-primary/30 bg-primary-ink/5 p-4">
        <p className="flex items-center gap-2 font-medium">
          <BellRing size={16} aria-hidden="true" className="text-primary-ink" />
          {person.name} is on the waitlist
        </p>
        <p className="mt-1 text-sm text-ink-muted">
          Dr. {doctor.name}, {formatDate(date, true)}, {shiftLabel(wait.shift).toLowerCase()}. We'll tell you in the app and by
          SMS as soon as a time frees up: first come, first served.
        </p>
        <Button size="sm" variant="secondary" className="mt-3" disabled={busy} onClick={() => act(() => leaveWaitlist(wait.id), "You've left the waitlist.")}>
          Leave the waitlist
        </Button>
        <Notice tone="danger" className="mt-3">{error}</Notice>
      </div>
    );
  }

  return (
    <div className="mt-4 rounded-xl border border-line p-4">
      <p className="font-medium">Full? Join the waitlist</p>
      <p className="mt-1 text-sm text-ink-muted">
        We'll tell you in the app and by SMS when a time with Dr. {doctor.name} on {formatDate(date, true)} frees up. First come,
        first served.
      </p>
      <div className="mt-3 flex flex-wrap items-end gap-3">
        <Field label="When" htmlFor="wait-shift">
          <Select id="wait-shift" value={when} onChange={(e) => setWhen(e.target.value)}>
            {WAIT_SHIFTS.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </Field>
        <Button disabled={busy} onClick={join}>
          {busy ? "Joining..." : "Join the waitlist"}
        </Button>
      </div>
      <Notice tone="danger" className="mt-3">{error}</Notice>
    </div>
  );
}
