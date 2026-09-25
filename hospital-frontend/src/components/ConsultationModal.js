import React, { useEffect, useState } from "react";
import axios from "axios";
import { API_BASE_URL } from "../config";
import { getErrorMessage } from "../utils/apiError";
import { downloadPrescription } from "../utils/downloadPrescription";

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

const inputClass =
  "w-full p-2 border rounded-lg bg-white dark:bg-[#0a1124] dark:border-[#233565] text-black dark:text-[#e2f6ff]";

// Doctor's consultation form for one appointment: vitals, findings, diagnosis and prescription.
// Saving it marks the appointment as completed. Also shows the patient's previous visits.
export default function ConsultationModal({ appointment, onClose, onSaved }) {
  const [form, setForm] = useState(emptyForm);
  const [exists, setExists] = useState(false);
  const [history, setHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const res = await axios.get(`${API_BASE_URL}/api/consultations/appointment/${appointment.appointmentID}`);
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
          const res = await axios.get(`${API_BASE_URL}/api/consultations/patient/${appointment.ptInfoId}`);
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
      await axios.put(`${API_BASE_URL}/api/consultations/appointment/${appointment.appointmentID}`, payload);
      setExists(true);
      onSaved?.(appointment.appointmentID);
    } catch (err) {
      setError(getErrorMessage(err, "Could not save the consultation."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-start justify-center overflow-y-auto p-4" role="dialog" aria-modal="true">
      <div className="bg-white dark:bg-[#111a3b] dark:text-gray-100 rounded-2xl shadow-2xl w-full max-w-3xl p-6 my-8">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h2 className="text-2xl font-bold text-[#1E63DB] dark:text-[#50d4f2]">Consultation</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {appointment.patientName}
              {appointment.age ? `, ${appointment.age} yrs` : ""} • {appointment.date} • Appointment #{appointment.appointmentID}
            </p>
            {appointment.message && (
              <p className="text-sm mt-1"><strong>Patient note:</strong> {appointment.message}</p>
            )}
          </div>
          <button onClick={onClose} className="text-2xl leading-none text-gray-500 hover:text-gray-800" aria-label="Close">×</button>
        </div>

        {history.length > 0 && (
          <div className="mb-4 border rounded-lg dark:border-[#233565]">
            <button
              type="button"
              onClick={() => setShowHistory(!showHistory)}
              className="w-full text-left px-4 py-2 font-semibold text-blue-700 dark:text-[#50d4f2]"
            >
              {showHistory ? "▼" : "▶"} Previous visits ({history.length})
            </button>
            {showHistory && (
              <ul className="px-4 pb-3 space-y-2 text-sm max-h-56 overflow-y-auto">
                {history.map((h) => (
                  <li key={h.id} className="border-t pt-2 dark:border-[#233565]">
                    <strong>{h.appointmentDate}</strong> — Dr. {h.doctorName}: {h.diagnosis}
                    {h.medicines?.length > 0 && (
                      <span className="text-gray-500"> ({h.medicines.map((m) => m.medicineName).join(", ")})</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <form onSubmit={save} className="space-y-4">
          <fieldset className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <legend className="font-semibold mb-1 col-span-full">Vitals (optional)</legend>
            <label className="text-sm">BP (mmHg)
              <input name="bloodPressure" value={form.bloodPressure} onChange={change} placeholder="120/80" className={inputClass} />
            </label>
            <label className="text-sm">Pulse (bpm)
              <input name="pulse" type="number" min="20" max="250" value={form.pulse} onChange={change} className={inputClass} />
            </label>
            <label className="text-sm">Temp (°C)
              <input name="temperature" type="number" step="0.1" min="30" max="45" value={form.temperature} onChange={change} className={inputClass} />
            </label>
            <label className="text-sm">Weight (kg)
              <input name="weightKg" type="number" step="0.1" min="0.5" max="500" value={form.weightKg} onChange={change} className={inputClass} />
            </label>
          </fieldset>

          <label className="block text-sm font-semibold">Symptoms
            <textarea name="symptoms" rows="2" value={form.symptoms} onChange={change} className={inputClass} />
          </label>
          <label className="block text-sm font-semibold">Diagnosis *
            <textarea name="diagnosis" rows="2" required value={form.diagnosis} onChange={change} className={inputClass} />
          </label>

          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="font-semibold">Prescription</span>
              <button type="button" onClick={addMedicine} className="text-sm px-3 py-1 rounded-lg bg-blue-600 text-white hover:bg-blue-700">
                + Add medicine
              </button>
            </div>
            {form.medicines.length === 0 && <p className="text-sm text-gray-500">No medicines added.</p>}
            <div className="space-y-2">
              {form.medicines.map((m, i) => (
                <div key={i} className="grid grid-cols-2 md:grid-cols-6 gap-2 items-center">
                  <input value={m.medicineName} onChange={(e) => changeMedicine(i, "medicineName", e.target.value)}
                    placeholder="Medicine *" aria-label="Medicine name" className={`${inputClass} md:col-span-2`} />
                  <input value={m.dosage || ""} onChange={(e) => changeMedicine(i, "dosage", e.target.value)}
                    placeholder="Dosage (500 mg)" aria-label="Dosage" className={inputClass} />
                  <input value={m.frequency || ""} onChange={(e) => changeMedicine(i, "frequency", e.target.value)}
                    placeholder="Frequency (1-0-1)" aria-label="Frequency" className={inputClass} />
                  <input value={m.duration || ""} onChange={(e) => changeMedicine(i, "duration", e.target.value)}
                    placeholder="Duration (5 days)" aria-label="Duration" className={inputClass} />
                  <div className="flex gap-1">
                    <input value={m.instructions || ""} onChange={(e) => changeMedicine(i, "instructions", e.target.value)}
                      placeholder="After food" aria-label="Instructions" className={inputClass} />
                    <button type="button" onClick={() => removeMedicine(i)} aria-label="Remove medicine"
                      className="px-2 text-red-600 hover:text-red-800 text-xl">×</button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <label className="block text-sm font-semibold">Advice / notes
            <textarea name="notes" rows="2" value={form.notes} onChange={change} className={inputClass} />
          </label>
          <label className="block text-sm font-semibold md:w-1/2">Follow-up date
            <input name="followUpDate" type="date" min={appointment.date} value={form.followUpDate} onChange={change} className={inputClass} />
          </label>

          {error && <p className="text-red-600 font-semibold">{error}</p>}

          <div className="flex flex-wrap justify-end gap-3 pt-2">
            {exists && (
              <button type="button" onClick={() => downloadPrescription(appointment.appointmentID)}
                className="px-4 py-2 rounded-lg border border-blue-600 text-blue-700 dark:text-[#50d4f2] font-semibold">
                Download Prescription
              </button>
            )}
            <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg bg-gray-300 dark:bg-[#2c3558] font-semibold">
              Close
            </button>
            <button type="submit" disabled={saving} className="px-5 py-2 rounded-lg bg-green-600 text-white font-semibold hover:bg-green-700 disabled:opacity-60">
              {saving ? "Saving..." : exists ? "Update Consultation" : "Save & Complete"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
