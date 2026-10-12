import React, { useEffect, useRef, useState } from "react";
import { specialityLabel } from "./appointmentFormat";
import { getQueueScreen } from "./api";

// The waiting-room screen (docs/TODAYS_QUEUE_PLAN.md), for a TV: per doctor with a queue today, the token being
// served and the next ones. Tokens only - no names - so it runs without a login at /queue-screen. Refreshes every few
// seconds; a doctor whose token just changed is highlighted until the next refresh, and announced to screen readers.
// If the server can't be reached, the last tokens stay up with a "Reconnecting" note.

const REFRESH_MS = 5000;

const clock = (date) => `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;

export default function QueueScreen() {
  const [screen, setScreen] = useState(null);
  const [offline, setOffline] = useState(false);
  const [now, setNow] = useState(new Date());
  const [justCalled, setJustCalled] = useState([]); // doctor names whose token changed with the last refresh
  const serving = useRef(null); // doctor name -> token, from the last refresh

  useEffect(() => {
    let current = true;
    const load = async () => {
      try {
        const response = await getQueueScreen();
        if (!current) return;
        const previous = serving.current;
        const next = new Map(response.data.doctors.map((d) => [d.doctorName, d.nowServing]));
        setJustCalled(
          previous === null
            ? []
            : response.data.doctors
                .filter((d) => d.nowServing != null && previous.get(d.doctorName) !== d.nowServing)
                .map((d) => d.doctorName)
        );
        serving.current = next;
        setScreen(response.data);
        setOffline(false);
      } catch {
        if (current) setOffline(true);
      }
      if (current) setNow(new Date());
    };
    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => {
      current = false;
      clearInterval(timer);
    };
  }, []);

  const fullScreen = () => document.documentElement.requestFullscreen?.();
  const doctors = screen?.doctors ?? [];
  const announcement = doctors
    .filter((d) => justCalled.includes(d.doctorName))
    .map((d) => `Dr. ${d.doctorName}: now serving ${d.nowServing}`)
    .join(". ");

  return (
    <div className="min-h-screen bg-[#0b1220] text-white p-8">
      <header className="flex flex-wrap items-baseline justify-between gap-4 mb-8">
        <h1 className="text-5xl font-bold">Now serving</h1>
        <div className="flex items-baseline gap-6">
          {offline && <span className="text-amber-300 text-xl">Reconnecting…</span>}
          <span className="text-5xl font-semibold tabular-nums" aria-label="Time">
            {clock(now)}
          </span>
          {document.fullscreenEnabled && !document.fullscreenElement && (
            <button type="button" onClick={fullScreen} className="text-sm underline text-gray-400">
              Full screen
            </button>
          )}
        </div>
      </header>

      <p aria-live="polite" className="sr-only">
        {announcement}
      </p>

      {screen && doctors.length === 0 && (
        <p className="text-3xl text-gray-300 text-center mt-24">No patients in the queue right now.</p>
      )}
      {!screen && !offline && <p className="text-2xl text-gray-400">Loading…</p>}

      <ul className="grid gap-6 grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
        {doctors.map((doctor) => {
          const highlighted = justCalled.includes(doctor.doctorName);
          return (
            <li
              key={doctor.doctorName}
              aria-label={`Dr. ${doctor.doctorName}`}
              data-just-called={highlighted}
              className={`rounded-2xl bg-[#16223a] p-6 ${highlighted ? "ring-4 ring-yellow-400" : ""}`}
            >
              <p className="text-3xl font-semibold">Dr. {doctor.doctorName}</p>
              <p className="text-xl text-gray-300 min-h-[1.75rem]">{specialityLabel(doctor.specialist)}</p>
              <p className="mt-4 text-lg uppercase tracking-wide text-gray-400">Now serving</p>
              <p className="text-8xl font-bold tabular-nums text-yellow-300" aria-label="Now serving">
                {doctor.nowServing ?? "—"}
              </p>
              <p className="mt-4 text-lg uppercase tracking-wide text-gray-400">Next</p>
              <p className="flex flex-wrap gap-x-8 text-4xl font-semibold tabular-nums" aria-label="Next">
                {doctor.next.length > 0 ? doctor.next.map((token) => <span key={token}>{token}</span>) : "—"}
              </p>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
