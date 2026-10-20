import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { ABHA_HINT, abhaInputProps, formatAbha } from "../../shared/utils/abha";
import { PublicPage } from "../../shared/layout/PublicSections";
import { Button, Card, Field, Input, Select, useToast } from "../../shared/ui";
import * as patientsApi from "./api";

// Adding a family member ("/add-relative") or changing one ("/edit-relative", state.relative). Afterwards back to
// where the patient came from (state.from, e.g. the booking page), otherwise to My account.

const RELATIONSHIPS = ["FATHER", "MOTHER", "WIFE", "HUSBAND", "SON", "DAUGHTER", "BROTHER", "SISTER", "GRANDFATHER", "GRANDMOTHER", "OTHER"];
const pretty = (value) => value.charAt(0) + value.slice(1).toLowerCase();

export default function AddRelativePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const editing = location.state?.relative || null;
  const back = location.state?.from || "/edit-profile";
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: editing?.name || "",
    gender: editing?.gender || "",
    dob: editing?.dob || "",
    relationship: editing?.relationship || "",
    patientAadharNo: editing?.patientAadharNo ? String(editing.patientAadharNo) : "",
    abhaNumber: formatAbha(editing?.abhaNumber),
  });

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    // The backend links the relative to the logged-in patient, so no patientId is sent. An empty ABHA number: none.
    const payload = {
      ...form,
      name: form.name.trim(),
      patientAadharNo: form.patientAadharNo ? Number(form.patientAadharNo) : null,
      abhaNumber: form.abhaNumber.trim(),
    };
    setSaving(true);
    try {
      if (editing) {
        await patientsApi.updateRelative(editing.id, payload);
        toast.success(`${payload.name}'s details are saved.`);
      } else {
        await patientsApi.addRelative(payload);
        toast.success(`${payload.name} added. You can now book for them.`);
      }
      navigate(back);
    } catch (err) {
      toast.error(getErrorMessage(err, editing ? "Failed to update the family member." : "Failed to add the family member."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <PublicPage>
      <div className="mx-auto max-w-2xl px-4 py-12 sm:px-6">
        <Link to={back} className="text-sm text-primary-ink hover:underline">
          ← Back
        </Link>
        <h1 className="mt-3 text-3xl font-semibold tracking-tight">{editing ? `Edit ${editing.name}` : "Add a family member"}</h1>
        <p className="mt-2 text-ink-muted">
          {editing ? "Change their details." : "Then you can book appointments for them from your account."}
        </p>

        <Card className="mt-8">
          <form onSubmit={handleSubmit} className="space-y-5">
            <Field label="Full name" htmlFor="relative-name">
              <Input id="relative-name" name="name" value={form.name} onChange={change} required />
            </Field>
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Relationship" htmlFor="relative-relationship">
                <Select id="relative-relationship" name="relationship" value={form.relationship} onChange={change} required>
                  <option value="">Choose</option>
                  {RELATIONSHIPS.map((value) => (
                    <option key={value} value={value}>
                      {pretty(value)}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="Gender" htmlFor="relative-gender">
                <Select id="relative-gender" name="gender" value={form.gender} onChange={change} required>
                  <option value="">Choose</option>
                  <option value="FEMALE">Female</option>
                  <option value="MALE">Male</option>
                  <option value="OTHER">Other</option>
                </Select>
              </Field>
            </div>
            <Field label="Date of birth" htmlFor="relative-dob">
              <Input id="relative-dob" name="dob" type="date" value={form.dob} max={toLocalISODate()} onChange={change} required />
            </Field>
            <Field label="Aadhaar number (optional)" htmlFor="relative-aadhaar" hint="12 digits.">
              <Input
                id="relative-aadhaar"
                name="patientAadharNo"
                inputMode="numeric"
                pattern="[0-9]{12}"
                title="Aadhaar number must be 12 digits"
                value={form.patientAadharNo}
                onChange={change}
              />
            </Field>
            <Field label="ABHA number (optional)" htmlFor="relative-abha" hint={`Their own health ID: ${ABHA_HINT}`}>
              <Input id="relative-abha" name="abhaNumber" value={form.abhaNumber} onChange={change} {...abhaInputProps} />
            </Field>
            <div className="flex justify-end gap-2 border-t border-line pt-4">
              <Button as={Link} to={back} variant="secondary">
                Cancel
              </Button>
              <Button type="submit" disabled={saving}>
                {saving ? "Saving..." : editing ? "Save changes" : "Add family member"}
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </PublicPage>
  );
}
