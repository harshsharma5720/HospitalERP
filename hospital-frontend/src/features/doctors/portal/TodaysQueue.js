import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../../shared/utils/apiError";
import ConsultationModal from "../../clinical/ConsultationModal";
import { formatDate, timeRange } from "../../appointments/appointmentFormat";
import { callNextPatient, callPatient, getMyQueue, patientNotHere } from "../../appointments/api";

// The doctor's queue today (docs/TODAYS_QUEUE_PLAN.md): who is with the doctor, who is waiting (by appointment
// time, with their tokens), who hasn't arrived and who is done. Call next / Call / Not here; the consultation form
// completes the visit as on the Appointments page. Refreshes itself, so patients checked in at the front desk appear.

const REFRESH_MS = 10000;
const CARD = "bg-white dark:bg-[#1e293b] shadow-md rounded-lg p-5 mb-5";
const GENDER = { FEMALE: "F", MALE: "M", OTHER: "" };

// "34 y · F" from the appointment's age and gender
const patientFacts = (appointment) =>
  [appointment.age ? `${appointment.age} y` : null, GENDER[appointment.gender] || null].filter(Boolean).join(" · ");

function Token({ number, large = false }) {
  return (
    <span
      className={`inline-flex items-center justify-center rounded-lg bg-blue-600 text-white font-bold ${
        large ? "w-16 h-16 text-3xl" : "w-10 h-10 text-lg"
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
      <div className="p-6">
        <h1 className="text-3xl font-bold mb-4">Today's queue</h1>
        {error ? (
          <p role="alert" className="text-red-600">
            {error}
          </p>
        ) : (
          <p>Loading...</p>
        )}
      </div>
    );
  }

  const { withDoctor, waiting, notArrived, done } = queue;

  return (
    <div className="p-6 max-w-4xl">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div>
          <h1 className="text-3xl font-bold">Today's queue</h1>
          <p className="text-gray-600 dark:text-gray-300">{formatDate(queue.date, true)}</p>
        </div>
        <button
          type="button"
          onClick={() => runAction(callNextPatient, called)}
          disabled={busy || waiting.length === 0}
          className="bg-green-600 hover:bg-green-700 text-white text-lg px-6 py-3 rounded-lg disabled:opacity-50"
        >
          Call next
        </button>
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

      <section aria-labelledby="queue-with-you" className={CARD}>
        <h2 id="queue-with-you" className="font-semibold mb-3">
          With you
        </h2>
        {withDoctor.length === 0 && (
          <p className="text-sm text-gray-600 dark:text-gray-300">
            {waiting.length > 0 ? "Nobody yet: call the next patient." : "Nobody is waiting."}
          </p>
        )}
        <ul className="space-y-3">
          {withDoctor.map((appointment) => (
            <li key={appointment.appointmentID} className="flex flex-wrap items-center gap-4">
              <Token number={appointment.tokenNumber} large />
              <div className="flex-1 min-w-0">
                <p className="text-xl font-semibold">{appointment.patientName}</p>
                <p className="text-sm text-gray-600 dark:text-gray-300">
                  {[patientFacts(appointment), timeRange(appointment), appointment.message].filter(Boolean).join(" · ")}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setConsulting(appointment)}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded"
              >
                Start consultation
              </button>
              <button
                type="button"
                onClick={() =>
                  runAction(() => patientNotHere(appointment.appointmentID), (a) => `${a.patientName} is waiting again (token ${a.tokenNumber}).`)
                }
                disabled={busy}
                aria-label={`${appointment.patientName} is not here`}
                className="border dark:border-gray-600 px-4 py-2 rounded disabled:opacity-50"
              >
                Not here
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="queue-waiting" className={CARD}>
        <h2 id="queue-waiting" className="font-semibold mb-3">
          Waiting ({waiting.length})
        </h2>
        {waiting.length === 0 && <p className="text-sm text-gray-600 dark:text-gray-300">Nobody has checked in.</p>}
        <ul className="divide-y dark:divide-gray-700">
          {waiting.map((appointment) => (
            <li key={appointment.appointmentID} className="flex flex-wrap items-center gap-4 py-2">
              <Token number={appointment.tokenNumber} />
              <span className="w-28 text-sm whitespace-nowrap">{timeRange(appointment)}</span>
              <span className="flex-1 min-w-0">
                <span className="font-semibold">{appointment.patientName}</span>
                <span className="text-sm text-gray-600 dark:text-gray-300">
                  {" "}
                  {[patientFacts(appointment), appointment.message].filter(Boolean).join(" · ")}
                </span>
              </span>
              <button
                type="button"
                onClick={() => runAction(() => callPatient(appointment.appointmentID), called)}
                disabled={busy}
                aria-label={`Call ${appointment.patientName}`}
                className="border border-blue-600 text-blue-700 dark:text-[#50d4f2] px-3 py-1 rounded text-sm disabled:opacity-50"
              >
                Call
              </button>
            </li>
          ))}
        </ul>
      </section>

      <div className="grid sm:grid-cols-2 gap-5">
        <section aria-labelledby="queue-not-arrived" className={CARD}>
          <h2 id="queue-not-arrived" className="font-semibold mb-3">
            Not arrived yet ({notArrived.length})
          </h2>
          <ul className="text-sm space-y-1">
            {notArrived.map((appointment) => (
              <li key={appointment.appointmentID}>
                <span className="text-gray-600 dark:text-gray-300">{timeRange(appointment)}</span> {appointment.patientName}
              </li>
            ))}
          </ul>
        </section>
        <section aria-labelledby="queue-done" className={CARD}>
          <h2 id="queue-done" className="font-semibold mb-3">
            Done ({done.length})
          </h2>
          <ul className="text-sm space-y-1">
            {done.map((appointment) => (
              <li key={appointment.appointmentID}>
                <span className="text-gray-600 dark:text-gray-300">{timeRange(appointment)}</span> {appointment.patientName}
              </li>
            ))}
          </ul>
        </section>
      </div>

      {consulting && (
        <ConsultationModal appointment={consulting} onClose={() => setConsulting(null)} onSaved={consultationSaved} />
      )}
    </div>
  );
}
