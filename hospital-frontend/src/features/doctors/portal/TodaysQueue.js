import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../../shared/utils/apiError";
import ConsultationModal from "../../clinical/ConsultationModal";
import { formatDate, timeRange } from "../../appointments/appointmentFormat";
import { callNextPatient, callPatient, getMyQueue, patientNotHere } from "../../appointments/api";
import { Button, Card, CardHeader, Notice, PageHeader } from "../../../shared/ui";

// The doctor's queue today (docs/TODAYS_QUEUE_PLAN.md): who is with the doctor, who is waiting (by appointment
// time, with their tokens), who hasn't arrived and who is done. Call next / Call / Not here; the consultation form
// completes the visit as on the Appointments page. Refreshes itself, so patients checked in at the front desk appear.

const REFRESH_MS = 10000;
const GENDER = { FEMALE: "F", MALE: "M", OTHER: "" };

// "34 y · F" from the appointment's age and gender
const patientFacts = (appointment) =>
  [appointment.age ? `${appointment.age} y` : null, GENDER[appointment.gender] || null].filter(Boolean).join(" · ");

function Token({ number, large = false }) {
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded-lg font-semibold tabular-nums ${
        large ? "h-16 w-16 bg-primary text-3xl text-white" : "h-10 w-10 bg-primary-ink/10 text-lg text-primary-ink"
      }`}
      aria-label={`Token ${number}`}
    >
      {number}
    </span>
  );
}

export default function TodaysQueue() {
  const [queue, setQueue] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [consulting, setConsulting] = useState(null); // the appointment open in the consultation form

  // quietly: a refresh in the background - a failure keeps what is shown
  const load = useCallback(async (quietly) => {
    try {
      const response = await getMyQueue();
      setQueue(response.data);
      if (!quietly) setError("");
    } catch (err) {
      if (!quietly) setError(getErrorMessage(err, "Could not load your queue."));
    }
  }, []);

  useEffect(() => {
    load(false);
    const timer = setInterval(() => load(true), REFRESH_MS);
    return () => clearInterval(timer);
  }, [load]);

  const runAction = async (action, describe) => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await action();
      setNotice(describe(response.data));
    } catch (err) {
      setError(getErrorMessage(err, "Could not change the queue."));
    } finally {
      setBusy(false);
      load(true);
    }
  };

  const called = (appointment) => `Token ${appointment.tokenNumber} · ${appointment.patientName} called in.`;

  const consultationSaved = () => {
    setConsulting(null);
    setNotice("Consultation saved; the visit is done.");
    load(true);
  };

  if (!queue) {
    return (
      <>
        <PageHeader title="Today's queue" />
        {error ? <Notice tone="danger">{error}</Notice> : <p className="text-ink-muted">Loading...</p>}
      </>
    );
  }

  const { withDoctor, waiting, notArrived, done } = queue;

  return (
    <div className="max-w-5xl">
      <PageHeader
        title="Today's queue"
        description={formatDate(queue.date, true)}
        actions={
          <Button size="lg" variant="success" onClick={() => runAction(callNextPatient, called)} disabled={busy || waiting.length === 0}>
            Call next
          </Button>
        }
      />

      <Notice tone="success" className="mb-4">{notice}</Notice>
      <Notice tone="danger" className="mb-4">{error}</Notice>

      <Card aria-labelledby="queue-with-you" className="mb-5 border-l-4 border-l-primary">
        <CardHeader id="queue-with-you" title="With you" />
        {withDoctor.length === 0 && (
          <p className="text-sm text-ink-muted">{waiting.length > 0 ? "Nobody yet: call the next patient." : "Nobody is waiting."}</p>
        )}
        <ul className="space-y-4">
          {withDoctor.map((appointment) => (
            <li key={appointment.appointmentID} className="flex flex-wrap items-center gap-4">
              <Token number={appointment.tokenNumber} large />
              <div className="min-w-0 flex-1">
                <p className="text-xl font-semibold">{appointment.patientName}</p>
                <p className="text-sm text-ink-muted">
                  {[patientFacts(appointment), timeRange(appointment), appointment.message].filter(Boolean).join(" · ")}
                </p>
              </div>
              <Button onClick={() => setConsulting(appointment)}>Start consultation</Button>
              <Button
                variant="secondary"
                onClick={() =>
                  runAction(() => patientNotHere(appointment.appointmentID), (a) => `${a.patientName} is waiting again (token ${a.tokenNumber}).`)
                }
                disabled={busy}
                aria-label={`${appointment.patientName} is not here`}
              >
                Not here
              </Button>
            </li>
          ))}
        </ul>
      </Card>

      <Card aria-labelledby="queue-waiting" className="mb-5">
        <CardHeader id="queue-waiting" title={`Waiting (${waiting.length})`} />
        {waiting.length === 0 && <p className="text-sm text-ink-muted">Nobody has checked in.</p>}
        <ul className="divide-y divide-line">
          {waiting.map((appointment) => (
            <li key={appointment.appointmentID} className="flex flex-wrap items-center gap-4 py-2.5 first:pt-0 last:pb-0">
              <Token number={appointment.tokenNumber} />
              <span className="w-28 whitespace-nowrap text-sm tabular-nums text-ink-muted">{timeRange(appointment)}</span>
              <span className="min-w-0 flex-1">
                <span className="font-medium">{appointment.patientName}</span>
                <span className="text-sm text-ink-muted">
                  {" "}
                  {[patientFacts(appointment), appointment.message].filter(Boolean).join(" · ")}
                </span>
              </span>
              <Button
                size="sm"
                variant="secondary"
                onClick={() => runAction(() => callPatient(appointment.appointmentID), called)}
                disabled={busy}
                aria-label={`Call ${appointment.patientName}`}
              >
                Call
              </Button>
            </li>
          ))}
        </ul>
      </Card>

      <div className="grid gap-5 sm:grid-cols-2">
        <Card aria-labelledby="queue-not-arrived">
          <CardHeader id="queue-not-arrived" title={`Not arrived yet (${notArrived.length})`} />
          <ul className="space-y-1.5 text-sm">
            {notArrived.map((appointment) => (
              <li key={appointment.appointmentID}>
                <span className="tabular-nums text-ink-muted">{timeRange(appointment)}</span> {appointment.patientName}
              </li>
            ))}
          </ul>
        </Card>
        <Card aria-labelledby="queue-done">
          <CardHeader id="queue-done" title={`Done (${done.length})`} />
          <ul className="space-y-1.5 text-sm">
            {done.map((appointment) => (
              <li key={appointment.appointmentID}>
                <span className="tabular-nums text-ink-muted">{timeRange(appointment)}</span> {appointment.patientName}
              </li>
            ))}
          </ul>
        </Card>
      </div>

      {consulting && (
        <ConsultationModal appointment={consulting} onClose={() => setConsulting(null)} onSaved={consultationSaved} />
      )}
    </div>
  );
}
