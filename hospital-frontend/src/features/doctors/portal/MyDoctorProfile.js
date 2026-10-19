import React, { useEffect, useState } from "react";
import useAuthStore from "../../auth/useAuthStore";
import { getErrorMessage } from "../../../shared/utils/apiError";
import { API_BASE_URL } from "../../../app/config";
import { specialityLabel } from "../../appointments/appointmentFormat";
import * as doctorsApi from "../api";
import { Button, Card, Field, Input, PageHeader, Select, useToast } from "../../../shared/ui";
import LogOutEverywhereCard from "../../auth/LogOutEverywhere";

const SPECIALIST_OPTIONS = [
  "NOT_ASSIGNED",
  "CARDIOLOGY",
  "DENTISTRY",
  "ORTHOPEDICS",
  "NEUROLOGY",
  "PEDIATRICS",
  "DERMATOLOGY",
];

const initials = (name) =>
  (name || "?")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join("");

// The logged-in doctor's own profile, editable (doctor portal)
export default function MyDoctorProfile() {
  const toast = useToast();
  const userId = useAuthStore((s) => s.userId);
  const [doctor, setDoctor] = useState(null);
  const [formData, setFormData] = useState({});
  const [isEditing, setIsEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    doctorsApi
      .getDoctor(userId)
      .then((res) => {
        setDoctor(res.data);
        setFormData(res.data);
      })
      .catch((err) => toast.error(getErrorMessage(err, "Could not load your profile.")));
  }, [toast, userId]);

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleImageChange = (e) => setFormData({ ...formData, profileImage: e.target.files[0] });

  const cancel = () => {
    setFormData(doctor);
    setIsEditing(false);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const formDataToSend = new FormData();
      const doctorPayload = {
        id: doctor.id,
        userId,
        name: formData.name,
        specialist: formData.specialist,
        email: formData.email,
        phoneNumber: formData.phoneNumber,
        userName: formData.userName,
        profileImage: formData.profileImage instanceof File ? doctor.profileImage : formData.profileImage,
      };
      formDataToSend.append("doctorDTO", new Blob([JSON.stringify(doctorPayload)], { type: "application/json" }));
      if (formData.profileImage instanceof File) {
        formDataToSend.append("profileImage", formData.profileImage);
      }

      await doctorsApi.updateDoctor(userId, formDataToSend);

      setDoctor(doctorPayload);
      setIsEditing(false);
      toast.success("Profile updated.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to update profile."));
    } finally {
      setSaving(false);
    }
  };

  if (!doctor) return <p className="text-ink-muted">Loading profile...</p>;

  const photo =
    formData.profileImage instanceof File
      ? URL.createObjectURL(formData.profileImage)
      : formData.profileImage
      ? `${API_BASE_URL}${formData.profileImage}`
      : null;

  return (
    <div className="max-w-3xl space-y-6">
      <form onSubmit={handleSave}>
        <PageHeader
          title="My profile"
          actions={
            isEditing ? (
              <>
                <Button variant="secondary" onClick={cancel}>
                  Cancel
                </Button>
                <Button type="submit" variant="success" disabled={saving}>
                  {saving ? "Saving..." : "Save changes"}
                </Button>
              </>
            ) : (
              <Button onClick={() => setIsEditing(true)}>Edit profile</Button>
            )
          }
        />

        <Card>
          <div className="mb-6 flex flex-wrap items-center gap-5 border-b border-line pb-6">
            {photo ? (
              <img src={photo} alt="" className="h-20 w-20 rounded-full object-cover ring-4 ring-primary-ink/10" />
            ) : (
              <span aria-hidden="true" className="flex h-20 w-20 items-center justify-center rounded-full bg-primary-ink/10 text-2xl font-semibold text-primary-ink">
                {initials(doctor.name)}
              </span>
            )}
            <div className="min-w-0 flex-1">
              <h2 className="text-xl font-semibold">Dr. {doctor.name}</h2>
              <p className="text-ink-muted">{specialityLabel(doctor.specialist) || "No speciality set"}</p>
            </div>
            {isEditing && (
              <Field label="New photo" htmlFor="profile-photo">
                <input
                  id="profile-photo"
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  className="text-sm text-ink-muted file:mr-3 file:rounded-lg file:border file:border-line file:bg-surface file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-ink hover:file:bg-surface-2"
                />
              </Field>
            )}
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <ProfileField label="Name" name="name" value={formData.name} />
            <ProfileField label="Username" name="userName" value={formData.userName} isEditing={isEditing} onChange={handleChange} />
            <ProfileField label="Email" name="email" value={formData.email} isEditing={isEditing} onChange={handleChange} />
            <ProfileField label="Phone Number" name="phoneNumber" value={formData.phoneNumber} isEditing={isEditing} onChange={handleChange} />
            {isEditing ? (
              <Field label="Specialization" htmlFor="profile-specialist">
                <Select id="profile-specialist" name="specialist" value={formData.specialist || "NOT_ASSIGNED"} onChange={handleChange}>
                  {SPECIALIST_OPTIONS.map((spec) => (
                    <option key={spec} value={spec}>
                      {specialityLabel(spec) || "Not assigned"}
                    </option>
                  ))}
                </Select>
              </Field>
            ) : (
              <ProfileField label="Specialization" value={specialityLabel(formData.specialist) || "Not assigned"} />
            )}
          </div>
        </Card>
      </form>
      <LogOutEverywhereCard />
    </div>
  );
}

// A detail of the profile: its value, or a box to change it while editing
function ProfileField({ label, name, value, isEditing = false, onChange }) {
  if (isEditing) {
    return (
      <Field label={label} htmlFor={`profile-${name}`}>
        <Input id={`profile-${name}`} name={name} value={value || ""} onChange={onChange} />
      </Field>
    );
  }
  return (
    <div>
      <p className="text-sm text-ink-muted">{label}</p>
      <p className="mt-1 font-medium">{value || "—"}</p>
    </div>
  );
}
