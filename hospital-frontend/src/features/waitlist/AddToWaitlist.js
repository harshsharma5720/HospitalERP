import React, { useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { Button, Field, Input, Modal, Notice, Select } from "../../shared/ui";
import { getAllDoctors } from "../doctors/api";
import { doctorLabel } from "../appointments/appointmentFormat";
import { addToWaitlist } from "./api";
import { WAIT_SHIFTS } from "./waitlistFormat";

// The front desk puts a patient - or one of their family members - on a waitlist (docs/WAITLIST_PLAN.md), from the
// patient's page. relatives: the family members ([] for none). onAdded(wait) after it is saved; the server's reason
// shows here when it is refused (free times, already booked, the limit...).

const WINDOW_DAYS = 30;

export default function AddToWaitlist({ patient, relatives, onAdded, onClose }) {
  const [doctors, setDoctors] = useState(null);
  const [form, setForm] = useState({ who: "", doctorId: "", date: toLocalISODate(addDays(new Date(), 1)), shift: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getAllDoctors()
      .then((res) => setDoctors(res.data.filter((d) => d.active !== false)))
      .catch(() => setDoctors([]));
  }, []);

  const change = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    if (!form.doctorId || !form.date) {
      setError("Please choose a doctor and a day.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const { data } = await addToWaitlist({
        patientId: patient.patientId,
        relativeId: form.who ? Number(form.who) : null,
        doctorId: Number(form.doctorId),
        date: form.date,
        shift: form.shift || null,
      });
      onAdded(data);
    } catch (err) {
      setError(getErrorMessage(err, "Could not add them to the waitlist."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title="Add to a waitlist"
      description="For a full day: they're told by SMS (and in the app, with an account) when a time frees up."
      onClose={onClose}
      closeOnBackdrop={false}
    >
      <form onSubmit={save} className="space-y-4">
        <Field label="Who" htmlFor="wait-who">
          <Select id="wait-who" value={form.who} onChange={change("who")}>
            <option value="">{patient.patientName}</option>
            {relatives.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name} (family member)
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Doctor" htmlFor="wait-doctor">
          <Select id="wait-doctor" value={form.doctorId} onChange={change("doctorId")}>
            <option value="">{doctors ? "Choose a doctor" : "Loading doctors..."}</option>
            {(doctors ?? []).map((d) => (
              <option key={d.id} value={d.id}>
                {doctorLabel(d)}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Day" htmlFor="wait-date">
            <Input
              id="wait-date"
              type="date"
              value={form.date}
              min={toLocalISODate()}
              max={toLocalISODate(addDays(new Date(), WINDOW_DAYS - 1))}
              onChange={change("date")}
            />
          </Field>
          <Field label="When" htmlFor="wait-when">
            <Select id="wait-when" value={form.shift} onChange={change("shift")}>
              {WAIT_SHIFTS.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Notice tone="danger">{error}</Notice>
        <div className="flex justify-end gap-2 border-t border-line pt-4">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Adding..." : "Add to the waitlist"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
