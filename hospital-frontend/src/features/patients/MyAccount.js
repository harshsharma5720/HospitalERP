import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarCheck, CalendarPlus, UserPlus, Users } from "lucide-react";
import useAuthStore from "../auth/useAuthStore";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { PageIntro, PublicPage } from "../../shared/layout/PublicSections";
import ProfileDetails, { profileRequest } from "../../shared/ProfileDetails";
import { Button, Card, CardHeader, EmptyState, Notice, useConfirm, useToast } from "../../shared/ui";
import { formatDate, timeRange } from "../appointments/appointmentFormat";
import { getMyAppointments } from "../appointments/api";
import * as patientsApi from "./api";

// The patient's account (docs/PATIENT_AREA_REFRESH_PLAN.md, P.4): their details, their family members (who they
// can book for) and their next appointment

const GENDERS = [
  ["FEMALE", "Female"],
  ["MALE", "Male"],
  ["OTHER", "Other"],
];
const pretty = (value) => (value ? value.charAt(0) + value.slice(1).toLowerCase() : "");

// Only the last 4 digits of the Aadhaar number are shown
const maskAadhaar = (number) => (number ? `XXXX XXXX ${String(number).slice(-4)}` : "");

const FIELDS = [
  { key: "patientName", label: "Name" },
  { key: "userName", label: "Username", readOnly: true },
  { key: "patientId", label: "Patient ID", readOnly: true, show: (id) => (id ? `#${id}` : "") },
  { key: "email", label: "Email", type: "email" },
  { key: "contactNo", label: "Phone number", type: "tel" },
  { key: "gender", label: "Gender", options: GENDERS },
  { key: "dob", label: "Date of birth", type: "date", show: (dob) => (dob ? formatDate(dob, true) : "") },
  { key: "patientAadharNo", label: "Aadhaar number", show: maskAadhaar },
  { key: "patientAddress", label: "Address" },
];

const isUpcoming = (a, today) => (a.status === "SCHEDULED" || a.status === "CONFIRMED") && a.date >= today;

export default function MyAccount() {
  const userId = useAuthStore((s) => s.userId);
  const toast = useToast();
  const confirm = useConfirm();
  const [account, setAccount] = useState(null);
  const [error, setError] = useState("");
  const [relatives, setRelatives] = useState(null);
  const [upcoming, setUpcoming] = useState(null);

  const load = useCallback(async () => {
    setError("");
    try {
      const { data } = await patientsApi.getPatientAccount(userId);
      setAccount(data);
      patientsApi
        .getRelatives(data.patientId)
        .then((res) => setRelatives(res.data))
        .catch(() => setRelatives([]));
    } catch (err) {
      setError(getErrorMessage(err, "Could not load your account."));
    }
    const today = toLocalISODate();
    getMyAppointments()
      .then((res) => setUpcoming(res.data.filter((a) => isUpcoming(a, today)).sort((a, b) => `${a.date} ${a.startTime}`.localeCompare(`${b.date} ${b.startTime}`))))
      .catch(() => setUpcoming([]));
  }, [userId]);

  useEffect(() => {
    load();
  }, [load]);

  const saveDetails = async (details, photo) => {
    const dto = { ...details, patientAadharNo: details.patientAadharNo === "" ? null : details.patientAadharNo };
    try {
      const { data } = await patientsApi.updatePatientAccount(userId, profileRequest("ptInfoDTO", dto, photo));
      setAccount(data);
      toast.success("Your details are saved.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to update your details."));
      throw err;
    }
  };

  const removeRelative = async (relative) => {
    const ok = await confirm({
      title: `Remove ${relative.name}?`,
      message: "You can no longer book for them. Their past appointments stay in your history.",
      confirmLabel: "Remove",
      tone: "danger",
    });
    if (!ok) return;
    try {
      await patientsApi.deleteRelative(relative.id);
      setRelatives((list) => list.filter((r) => r.id !== relative.id));
      toast.success(`${relative.name} removed.`);
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to remove the family member."));
    }
  };

  if (error) {
    return (
      <PublicPage>
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <Notice tone="danger">{error}</Notice>
          <Button variant="secondary" className="mt-4" onClick={load}>
            Try again
          </Button>
        </div>
      </PublicPage>
    );
  }

  if (!account) {
    return (
      <PublicPage>
        <p className="mx-auto max-w-7xl px-4 py-16 text-ink-muted sm:px-6 lg:px-8">Loading your account...</p>
      </PublicPage>
    );
  }

  const next = upcoming?.[0];

  return (
    <PublicPage>
      <PageIntro eyebrow="My account" title={`Hello, ${(account.patientName || account.userName || "").split(" ")[0]}`}>
        Your details, the family members you book for, and your appointments.
      </PageIntro>

      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-6 px-4 py-12 sm:px-6 lg:grid-cols-3 lg:px-8">
        <div className="min-w-0 space-y-6 lg:col-span-2">
          <ProfileDetails name={account.patientName || account.userName} data={account} fields={FIELDS} onSave={saveDetails} />

          <Card aria-labelledby="family-members">
            <CardHeader
              id="family-members"
              title="Family members"
              description="People you can book appointments for."
              actions={
                <Button as={Link} to="/add-relative" size="sm" variant="secondary">
                  <UserPlus size={16} aria-hidden="true" />
                  Add
                </Button>
              }
            />
            {relatives === null && <p className="text-sm text-ink-muted">Loading...</p>}
            {relatives?.length === 0 && (
              <EmptyState icon={Users} title="No family members yet">
                Add your parents, partner or children to book appointments for them.
              </EmptyState>
            )}
            {relatives?.length > 0 && (
              <ul className="divide-y divide-line">
                {relatives.map((relative) => (
                  <li key={relative.id} className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
                    <div>
                      <p className="font-medium">{relative.name}</p>
                      <p className="text-sm text-ink-muted">
                        {[pretty(relative.relationship), relative.dob ? `${calculateAgeFromDOB(relative.dob)} y` : null, pretty(relative.gender)]
                          .filter(Boolean)
                          .join(" · ")}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button as={Link} to="/edit-relative" state={{ relative }} size="sm" variant="secondary" aria-label={`Edit ${relative.name}`}>
                        Edit
                      </Button>
                      <Button size="sm" variant="danger-ghost" onClick={() => removeRelative(relative)} aria-label={`Remove ${relative.name}`}>
                        Remove
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>

        <aside className="min-w-0 lg:self-start">
          <Card aria-labelledby="account-appointments">
            <CardHeader id="account-appointments" title="Your appointments" />
            {upcoming === null && <p className="text-sm text-ink-muted">Loading...</p>}
            {upcoming && next && (
              <div className="rounded-xl bg-primary-ink/10 p-4">
                <p className="flex items-center gap-2 text-sm font-medium text-primary-ink">
                  <CalendarCheck size={16} aria-hidden="true" />
                  Next visit
                </p>
                <p className="mt-2 font-semibold">
                  {formatDate(next.date, true)}, {timeRange(next)}
                </p>
                <p className="text-sm text-ink-muted">
                  Dr. {next.doctorName} · for {next.patientName}
                </p>
              </div>
            )}
            {upcoming && !next && <p className="text-sm text-ink-muted">No upcoming appointments.</p>}
            {upcoming && upcoming.length > 1 && <p className="mt-3 text-sm text-ink-muted">and {upcoming.length - 1} more upcoming</p>}
            <div className="mt-5 flex flex-col gap-2">
              <Button as={Link} to="/appointments">
                <CalendarPlus size={16} aria-hidden="true" />
                Book an appointment
              </Button>
              <Button as={Link} to="/appointment-details" variant="secondary">
                My appointments
              </Button>
            </div>
          </Card>
        </aside>
      </div>
    </PublicPage>
  );
}
