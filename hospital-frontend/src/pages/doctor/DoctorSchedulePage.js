import React, { useEffect, useState } from "react";
import axios from "axios";
import { API_BASE_URL } from "../../config";
import { getErrorMessage } from "../../utils/apiError";
import useAuthStore from "../../Store/useAuthStore";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const SHIFTS = ["MORNING", "EVENING"];
const SLOT_OPTIONS = [5, 10, 15, 20, 30, 45, 60];

const pretty = (s) => s.charAt(0) + s.slice(1).toLowerCase();
const hhmm = (t) => (t ? String(t).slice(0, 5) : "");

// Weekly working hours: which days/shifts the doctor works, the hours and the slot length.
// Patients can only book slots inside these hours.
export default function DoctorSchedulePage() {
  const userId = useAuthStore((s) => s.userId);
  const [entries, setEntries] = useState([]); // 14 rows: 7 days × 2 shifts
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const res = await axios.get(`${API_BASE_URL}/api/doctor/${userId}/schedule`);
        setEntries(res.data.map((e) => ({ ...e, startTime: hhmm(e.startTime), endTime: hhmm(e.endTime) })));
      } catch (err) {
        setMessage(getErrorMessage(err, "Could not load your schedule."));
      } finally {
        setLoading(false);
      }
    };
    if (userId) load();
  }, [userId]);

  const find = (day, shift) => entries.find((e) => e.dayOfWeek === day && e.shift === shift);

  const update = (day, shift, changes) => {
    setMessage("");
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
    setMessage("");
    try {
      const payload = entries.map(({ dayOfWeek, shift, working, startTime, endTime, slotMinutes }) => ({
        dayOfWeek,
        shift,
        working,
        startTime: working ? startTime : null,
        endTime: working ? endTime : null,
        slotMinutes: Number(slotMinutes),
      }));
      const res = await axios.put(`${API_BASE_URL}/api/doctor/${userId}/schedule`, payload);
      setEntries(res.data.map((e) => ({ ...e, startTime: hhmm(e.startTime), endTime: hhmm(e.endTime) })));
      setMessage("✔ Schedule saved. Existing bookings are not affected.");
    } catch (err) {
      setMessage(getErrorMessage(err, "Could not save the schedule."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p className="text-gray-600 dark:text-gray-300">Loading schedule...</p>;

  return (
    <div className="bg-white dark:bg-[#111a3b] rounded-2xl shadow-xl p-6">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-2">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">My Weekly Schedule</h1>
        <div className="flex gap-2">
          <button
            onClick={copyMondayToWeekdays}
            className="px-4 py-2 rounded-lg border border-blue-600 text-blue-700 dark:text-[#50d4f2] dark:border-[#50d4f2] font-semibold hover:bg-blue-50 dark:hover:bg-[#16224a]"
          >
            Copy Monday to Tue–Fri
          </button>
          <button
            onClick={save}
            disabled={saving}
            className="px-5 py-2 rounded-lg bg-green-600 text-white font-semibold hover:bg-green-700 disabled:opacity-60"
          >
            {saving ? "Saving..." : "Save Schedule"}
          </button>
        </div>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
        Patients can book only inside these hours. Days marked “default” use the hospital hours
        (morning 09:00–12:00, evening 15:00–19:00, 10-minute slots).
      </p>

      {message && (
        <p className={`mb-4 font-semibold ${message.startsWith("✔") ? "text-green-600" : "text-red-600"}`}>{message}</p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full border rounded-xl overflow-hidden text-sm dark:text-gray-200">
          <thead className="bg-gray-100 dark:bg-[#16224a]">
            <tr>
              <th className="p-3 border text-left">Day</th>
              {SHIFTS.map((shift) => (
                <th key={shift} className="p-3 border text-left">{pretty(shift)}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {DAYS.map((day) => (
              <tr key={day}>
                <td className="p-3 border font-semibold">{pretty(day)}</td>
                {SHIFTS.map((shift) => {
                  const e = find(day, shift);
                  if (!e) return <td key={shift} className="p-3 border" />;
                  return (
                    <td key={shift} className={`p-3 border ${e.working ? "" : "bg-gray-50 dark:bg-[#0f172a]"}`}>
                      <div className="flex flex-wrap items-center gap-2">
                        <label className="flex items-center gap-1 font-medium">
                          <input
                            type="checkbox"
                            checked={e.working}
                            onChange={(ev) => update(day, shift, { working: ev.target.checked })}
                            aria-label={`${pretty(day)} ${shift.toLowerCase()} working`}
                          />
                          {e.working ? "Working" : "Off"}
                        </label>
                        {e.working && (
                          <>
                            <input
                              type="time"
                              value={e.startTime}
                              onChange={(ev) => update(day, shift, { startTime: ev.target.value })}
                              aria-label={`${pretty(day)} ${shift.toLowerCase()} start`}
                              className="border rounded p-1 dark:bg-[#0a1124]"
                            />
                            <span>–</span>
                            <input
                              type="time"
                              value={e.endTime}
                              onChange={(ev) => update(day, shift, { endTime: ev.target.value })}
                              aria-label={`${pretty(day)} ${shift.toLowerCase()} end`}
                              className="border rounded p-1 dark:bg-[#0a1124]"
                            />
                            <select
                              value={e.slotMinutes}
                              onChange={(ev) => update(day, shift, { slotMinutes: Number(ev.target.value) })}
                              aria-label={`${pretty(day)} ${shift.toLowerCase()} slot length`}
                              className="border rounded p-1 dark:bg-[#0a1124]"
                            >
                              {SLOT_OPTIONS.map((m) => (
                                <option key={m} value={m}>{m} min</option>
                              ))}
                            </select>
                          </>
                        )}
                        {e.usingDefault && <span className="text-xs text-gray-400">default</span>}
                      </div>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
