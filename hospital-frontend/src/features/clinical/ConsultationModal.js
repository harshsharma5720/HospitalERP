import React, { useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as clinicalApi from "./api";
import { downloadPrescription } from "./downloadPrescription";
import { ChevronRight, X } from "lucide-react";
import { formatDate } from "../appointments/appointmentFormat";
import { Button, Field, Input, Modal, Notice, Textarea, useToast } from "../../shared/ui";

const emptyMedicine = { medicineName: "", dosage: "", frequency: "", duration: "", instructions: "" };
const emptyForm = {
  symptoms: "",
  diagnosis: "",
  notes: "",
  bloodPressure: "",
  pulse: "",
  temperature: "",
  weightKg: "",
  followUpDate: "",
  medicines: [],
};

// Doctor's consultation form for one appointment: vitals, findings, diagnosis and prescription.
// Saving it marks the appointment as completed. Also shows the patient's previous visits.
export default function ConsultationModal({ appointment, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(emptyForm);
  const [exists, setExists] = useState(false);
  const [history, setHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const res = await clinicalApi.getConsultation(appointment.appointmentID);
        const c = res.data;
        setExists(true);
        setForm({
          symptoms: c.symptoms || "",
          diagnosis: c.diagnosis || "",
          notes: c.notes || "",
          bloodPressure: c.bloodPressure || "",
          pulse: c.pulse ?? "",
          temperature: c.temperature ?? "",
          weightKg: c.weightKg ?? "",
          followUpDate: c.followUpDate || "",
          medicines: c.medicines || [],
        });
      } catch (err) {
        if (err?.response?.status !== 404) setError(getErrorMessage(err, "Could not load the consultation."));
      }
      if (appointment.ptInfoId) {
        try {
          const res = await clinicalApi.getPatientHistory(appointment.ptInfoId);
          setHistory(res.data.filter((h) => h.appointmentId !== appointment.appointmentID));
        } catch {
          setHistory([]);
        }
      }
    };
    load();
  }, [appointment]);

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const changeMedicine = (index, field, value) =>
    setForm({
      ...form,
      medicines: form.medicines.map((m, i) => (i === index ? { ...m, [field]: value } : m)),
    });

  const addMedicine = () => setForm({ ...form, medicines: [...form.medicines, { ...emptyMedicine }] });
  const removeMedicine = (index) => setForm({ ...form, medicines: form.medicines.filter((_, i) => i !== index) });

  const save = async (e) => {
    e.preventDefault();
    setError("");
    const toNumber = (v) => (v === "" || v === null ? null : Number(v));
    const payload = {
      ...form,
      pulse: toNumber(form.pulse),
      temperature: toNumber(form.temperature),
      weightKg: toNumber(form.weightKg),
      followUpDate: form.followUpDate || null,
      medicines: form.medicines.filter((m) => m.medicineName.trim()),
    };
    try {
      setSaving(true);
      await clinicalApi.saveConsultation(appointment.appointmentID, payload);
      setExists(true);
      onSaved?.(appointment.appointmentID);
    } catch (err) {
      setError(getErrorMessage(err, "Could not save the consultation."));
    } finally {
      setSaving(false);
    }
  };

  const about = [
    `${appointment.patientName}${appointment.age ? `, ${appointment.age} yrs` : ""}`,
    appointment.date ? formatDate(appointment.date, true) : null,
    `Appointment #${appointment.appointmentID}`,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <Modal title="Consultation" description={about} onClose={onClose} size="lg" closeOnBackdrop={false}>
      {appointment.message && (
        <p className="mb-4 rounded-lg bg-surface-2 px-3 py-2 text-sm">
          <span className="font-medium">Patient note:</span> {appointment.message}
        </p>
      )}

      {history.length > 0 && (
        <div className="mb-5 rounded-lg border border-line">
          <button
            type="button"
            onClick={() => setShowHistory(!showHistory)}
            aria-expanded={showHistory}
            className="flex w-full items-center gap-2 rounded-lg px-4 py-2.5 text-left text-sm font-medium text-primary-ink hover:bg-surface-2"
          >
            <ChevronRight size={16} aria-hidden="true" className={`transition-transform ${showHistory ? "rotate-90" : ""}`} />
            Previous visits ({history.length})
          </button>
          {showHistory && (
            <ul className="max-h-56 space-y-2 overflow-y-auto px-4 pb-3 text-sm">
              {history.map((h) => (
                <li key={h.id} className="border-t border-line pt-2">
                  <span className="font-medium">{h.appointmentDate}</span> — Dr. {h.doctorName}: {h.diagnosis}
                  {h.medicines?.length > 0 && (
                    <span className="text-ink-muted"> ({h.medicines.map((m) => m.medicineName).join(", ")})</span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      <form onSubmit={save} className="space-y-5">
        <fieldset>
          <legend className="mb-2 text-sm font-semibold">Vitals (optional)</legend>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Field label="BP (mmHg)" htmlFor="c-bp">
              <Input id="c-bp" name="bloodPressure" value={form.bloodPressure} onChange={change} placeholder="120/80" />
            </Field>
            <Field label="Pulse (bpm)" htmlFor="c-pulse">
              <Input id="c-pulse" name="pulse" type="number" min="20" max="250" value={form.pulse} onChange={change} />
            </Field>
            <Field label="Temp (°C)" htmlFor="c-temp">
              <Input id="c-temp" name="temperature" type="number" step="0.1" min="30" max="45" value={form.temperature} onChange={change} />
            </Field>
            <Field label="Weight (kg)" htmlFor="c-weight">
              <Input id="c-weight" name="weightKg" type="number" step="0.1" min="0.5" max="500" value={form.weightKg} onChange={change} />
            </Field>
          </div>
        </fieldset>

        <Field label="Symptoms" htmlFor="c-symptoms">
          <Textarea id="c-symptoms" name="symptoms" rows="2" value={form.symptoms} onChange={change} />
        </Field>
        <Field label="Diagnosis *" htmlFor="c-diagnosis">
          <Textarea id="c-diagnosis" name="diagnosis" rows="2" required value={form.diagnosis} onChange={change} />
        </Field>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-semibold">Prescription</span>
            <Button size="sm" variant="secondary" onClick={addMedicine}>
              + Add medicine
            </Button>
          </div>
          {form.medicines.length === 0 && <p className="text-sm text-ink-muted">No medicines added.</p>}
          {form.medicines.length > 0 && (
            <div aria-hidden="true" className="mb-1 hidden gap-2 text-xs font-medium text-ink-muted md:grid md:grid-cols-6">
              <span className="md:col-span-2">Medicine *</span>
              <span>Dosage</span>
              <span>Frequency</span>
              <span>Duration</span>
              <span>Instructions</span>
            </div>
          )}
          <div className="space-y-2">
            {form.medicines.map((m, i) => (
              <div key={i} className="grid grid-cols-2 items-center gap-2 md:grid-cols-6">
                <Input value={m.medicineName} onChange={(e) => changeMedicine(i, "medicineName", e.target.value)}
                  placeholder="e.g. Paracetamol" aria-label="Medicine name" className="md:col-span-2" />
                <Input value={m.dosage || ""} onChange={(e) => changeMedicine(i, "dosage", e.target.value)}
                  placeholder="500 mg" aria-label="Dosage" />
                <Input value={m.frequency || ""} onChange={(e) => changeMedicine(i, "frequency", e.target.value)}
                  placeholder="1-0-1" aria-label="Frequency" />
                <Input value={m.duration || ""} onChange={(e) => changeMedicine(i, "duration", e.target.value)}
                  placeholder="5 days" aria-label="Duration" />
                <div className="flex gap-1">
                  <Input value={m.instructions || ""} onChange={(e) => changeMedicine(i, "instructions", e.target.value)}
                    placeholder="After food" aria-label="Instructions" />
                  <Button variant="ghost" size="sm" onClick={() => removeMedicine(i)} aria-label="Remove medicine" className="h-10 text-danger-ink">
                    <X size={16} aria-hidden="true" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>

        <Field label="Advice / notes" htmlFor="c-notes">
          <Textarea id="c-notes" name="notes" rows="2" value={form.notes} onChange={change} />
        </Field>
        <Field label="Follow-up date" htmlFor="c-follow-up" className="md:w-1/2">
          <Input id="c-follow-up" name="followUpDate" type="date" min={appointment.date} value={form.followUpDate} onChange={change} />
        </Field>

        <Notice tone="danger">{error}</Notice>

        <div className="flex flex-wrap justify-end gap-2 border-t border-line pt-4">
          {exists && (
            <Button variant="secondary" onClick={() => downloadPrescription(appointment.appointmentID, toast.error)}>
              Download Prescription
            </Button>
          )}
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          <Button type="submit" variant="success" disabled={saving}>
            {saving ? "Saving..." : exists ? "Update Consultation" : "Save & Complete"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
