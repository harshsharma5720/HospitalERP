import React, { useEffect, useState } from "react";
import { getSchedule, saveSchedule } from "../api";
import { getErrorMessage } from "../../../shared/utils/apiError";
import useAuthStore from "../../auth/useAuthStore";
import { Badge, Button, Notice, PageHeader, Table, Td, Th } from "../../../shared/ui";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const SHIFTS = ["MORNING", "EVENING"];
const SLOT_OPTIONS = [5, 10, 15, 20, 30, 45, 60];

const pretty = (s) => s.charAt(0) + s.slice(1).toLowerCase();
const hhmm = (t) => (t ? String(t).slice(0, 5) : "");
const SMALL_CONTROL =
  "h-8 rounded-md border border-line bg-surface px-2 text-sm text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20";

// Weekly working hours: which days/shifts the doctor works, the hours and the slot length.
// Patients can only book slots inside these hours.
export default function DoctorSchedulePage() {
  const userId = useAuthStore((s) => s.userId);
  const [entries, setEntries] = useState([]); // 14 rows: 7 days × 2 shifts
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null); // { tone, text }

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getSchedule(userId);
        setEntries(res.data.map((e) => ({ ...e, startTime: hhmm(e.startTime), endTime: hhmm(e.endTime) })));
      } catch (err) {
        setMessage({ tone: "danger", text: getErrorMessage(err, "Could not load your schedule.") });
      } finally {
        setLoading(false);
      }
    };
    if (userId) load();
  }, [userId]);

  const find = (day, shift) => entries.find((e) => e.dayOfWeek === day && e.shift === shift);

  const update = (day, shift, changes) => {
    setMessage(null);
    setEntries((prev) =>
      prev.map((e) => (e.dayOfWeek === day && e.shift === shift ? { ...e, ...changes, usingDefault: false } : e))
    );
  };

  // Copies Monday's hours to Tuesday–Friday
  const copyMondayToWeekdays = () => {
    setEntries((prev) =>
      prev.map((e) => {
        if (!["TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"].includes(e.dayOfWeek)) return e;
        const monday = prev.find((m) => m.dayOfWeek === "MONDAY" && m.shift === e.shift);
        return { ...monday, dayOfWeek: e.dayOfWeek, usingDefault: false };
      })
    );
  };

  const save = async () => {
    setSaving(true);
    setMessage(null);
    try {
      const payload = entries.map(({ dayOfWeek, shift, working, startTime, endTime, slotMinutes }) => ({
        dayOfWeek,
        shift,
        working,
        startTime: working ? startTime : null,
        endTime: working ? endTime : null,
        slotMinutes: Number(slotMinutes),
      }));
      const res = await saveSchedule(userId, payload);
      setEntries(res.data.map((e) => ({ ...e, startTime: hhmm(e.startTime), endTime: hhmm(e.endTime) })));
      setMessage({ tone: "success", text: "Schedule saved. Existing bookings are not affected." });
    } catch (err) {
      setMessage({ tone: "danger", text: getErrorMessage(err, "Could not save the schedule.") });
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p className="text-ink-muted">Loading schedule...</p>;

  return (
    <>
      <PageHeader
        title="Weekly schedule"
        description="Patients can book only inside these hours. Days marked “default” use the hospital hours (morning 09:00–12:00, evening 15:00–19:00, 10-minute slots)."
        actions={
          <>
            <Button variant="secondary" onClick={copyMondayToWeekdays}>
              Copy Monday to Tue–Fri
            </Button>
            <Button variant="success" onClick={save} disabled={saving}>
              {saving ? "Saving..." : "Save Schedule"}
            </Button>
          </>
        }
      />

      {message && (
        <Notice tone={message.tone} className="mb-4">
          {message.text}
        </Notice>
      )}

      <Table>
        <thead>
          <tr>
            <Th>Day</Th>
            {SHIFTS.map((shift) => (
              <Th key={shift}>{pretty(shift)}</Th>
            ))}
          </tr>
        </thead>
        <tbody>
          {DAYS.map((day) => (
            <tr key={day}>
              <Td className="font-medium">{pretty(day)}</Td>
              {SHIFTS.map((shift) => {
                const e = find(day, shift);
                if (!e) return <Td key={shift} />;
                return (
                  <Td key={shift} className={e.working ? "" : "bg-surface-2/60"}>
                    <div className="flex flex-wrap items-center gap-2">
                      <label className="flex w-20 items-center gap-2 font-medium">
                        <input
                          type="checkbox"
                          checked={e.working}
                          onChange={(ev) => update(day, shift, { working: ev.target.checked })}
                          aria-label={`${pretty(day)} ${shift.toLowerCase()} working`}
                          className="h-4 w-4 accent-primary"
                        />
                        <span className={e.working ? "" : "text-ink-muted"}>{e.working ? "Working" : "Off"}</span>
                      </label>
                      {e.working && (
                        <>
                          <input
                            type="time"
                            value={e.startTime}
                            onChange={(ev) => update(day, shift, { startTime: ev.target.value })}
                            aria-label={`${pretty(day)} ${shift.toLowerCase()} start`}
                            className={SMALL_CONTROL}
                          />
                          <span className="text-ink-muted">–</span>
                          <input
                            type="time"
                            value={e.endTime}
                            onChange={(ev) => update(day, shift, { endTime: ev.target.value })}
                            aria-label={`${pretty(day)} ${shift.toLowerCase()} end`}
                            className={SMALL_CONTROL}
                          />
                          <select
                            value={e.slotMinutes}
                            onChange={(ev) => update(day, shift, { slotMinutes: Number(ev.target.value) })}
                            aria-label={`${pretty(day)} ${shift.toLowerCase()} slot length`}
                            className={SMALL_CONTROL}
                          >
                            {SLOT_OPTIONS.map((m) => (
                              <option key={m} value={m}>
                                {m} min
                              </option>
                            ))}
                          </select>
                        </>
                      )}
                      {e.usingDefault && <Badge>default</Badge>}
                    </div>
                  </Td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  );
}
