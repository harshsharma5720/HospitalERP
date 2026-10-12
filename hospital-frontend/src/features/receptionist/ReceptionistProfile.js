import React, { useEffect, useState } from "react";
import useAuthStore from "../auth/useAuthStore";
import { getErrorMessage } from "../../shared/utils/apiError";
import { getReceptionist, updateReceptionist } from "../../shared/profileApi";
import ProfileDetails, { profileRequest } from "../../shared/ProfileDetails";
import { Notice, PageHeader, useToast } from "../../shared/ui";

// The receptionist's own profile, inside the staff portal (docs/PATIENT_AREA_REFRESH_PLAN.md, P.4)
const FIELDS = [
  { key: "name", label: "Name" },
  { key: "userName", label: "Username", readOnly: true },
  { key: "email", label: "Email", type: "email" },
  { key: "phone", label: "Phone number", type: "tel" },
  {
    key: "gender",
    label: "Gender",
    options: [
      ["FEMALE", "Female"],
      ["MALE", "Male"],
      ["OTHER", "Other"],
    ],
  },
  { key: "age", label: "Age", type: "number" },
];

export default function ReceptionistProfile() {
  const userId = useAuthStore((s) => s.userId);
  const toast = useToast();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getReceptionist(userId)
      .then((res) => setProfile(res.data))
      .catch((err) => setError(getErrorMessage(err, "Could not load your profile.")));
  }, [userId]);

  const save = async (details, photo) => {
    try {
      const { data } = await updateReceptionist(userId, profileRequest("receptionistDTO", details, photo));
      setProfile(data);
      toast.success("Your profile is saved.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to update your profile."));
      throw err;
    }
  };

  return (
    <div className="max-w-3xl">
      <PageHeader title="My profile" />
      <Notice tone="danger">{error}</Notice>
      {!profile && !error && <p className="text-ink-muted">Loading your profile...</p>}
      {profile && <ProfileDetails title="Your details" name={profile.name || profile.userName} data={profile} fields={FIELDS} onSave={save} />}
    </div>
  );
}
