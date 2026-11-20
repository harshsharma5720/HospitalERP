import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { BellRing } from "lucide-react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { Button, Card, CardHeader, useConfirm, useToast } from "../../shared/ui";
import { formatDate } from "../appointments/appointmentFormat";
import { getMyWaits, leaveWaitlist } from "./api";
import { bookingLink, shiftLabel } from "./waitlistFormat";

// The patient's waits on My appointments (docs/WAITLIST_PLAN.md): each with its free times and Leave. Nothing at all
// while there are none (or they couldn't be loaded).

export default function MyWaitlist() {
  const toast = useToast();
  const confirm = useConfirm();
  const [waits, setWaits] = useState([]);

  useEffect(() => {
    getMyWaits()
      .then((res) => setWaits(res.data))
      .catch(() => setWaits([]));
  }, []);

  const leave = async (wait) => {
    const ok = await confirm({
      title: "Leave the waitlist?",
      message: `${wait.name}, Dr. ${wait.doctorName}, ${formatDate(wait.date, true)}. You won't be told when a time frees up.`,
      confirmLabel: "Leave",
      cancelLabel: "Stay on it",
      tone: "danger",
    });
    if (!ok) return;
    try {
      await leaveWaitlist(wait.id);
      setWaits((list) => list.filter((w) => w.id !== wait.id));
      toast.success("You've left the waitlist.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Could not leave the waitlist."));
    }
  };

  if (waits.length === 0) return null;

  return (
    <Card aria-labelledby="my-waitlist" className="mb-6">
      <CardHeader
        id="my-waitlist"
        title="On the waitlist"
        description="We'll tell you in the app and by SMS when a time frees up. First come, first served."
      />
      <ul className="divide-y divide-line">
        {waits.map((wait) => (
          <li key={wait.id} className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
            <div className="min-w-0">
              <p className="font-medium">
                Dr. {wait.doctorName} · {formatDate(wait.date, true)}
              </p>
              <p className="text-sm text-ink-muted">
                {shiftLabel(wait.shift)} · for {wait.name}
              </p>
              {wait.toldAt && (
                <p className="mt-0.5 flex items-center gap-1.5 text-sm font-medium text-primary-ink">
                  <BellRing size={14} aria-hidden="true" />A time freed up: see if it's still free
                </p>
              )}
            </div>
            <div className="flex gap-2">
              <Button as={Link} to={bookingLink(wait)} size="sm" variant="secondary">
                Free times
              </Button>
              <Button
                size="sm"
                variant="danger-ghost"
                onClick={() => leave(wait)}
                aria-label={`Leave the waitlist for ${wait.name} with Dr. ${wait.doctorName} on ${formatDate(wait.date, true)}`}
              >
                Leave
              </Button>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}
