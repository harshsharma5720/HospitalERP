import React, { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { CalendarCheck, CheckCircle2, Moon, Sun, UserPlus } from "lucide-react";
import useAuthStore from "../auth/useAuthStore";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { getErrorMessage } from "../../shared/utils/apiError";
import { PageIntro, PublicPage } from "../../shared/layout/PublicSections";
import { Button, Card, Field, Input, Notice, Select, Textarea, useToast } from "../../shared/ui";
import useDoctorDirectory from "../doctors/useDoctorDirectory";
import { getPatientAccount, getRelatives } from "../patients/api";
import { dayLabel, doctorLabel, formatDate, formatTime } from "./appointmentFormat";
import * as appointmentsApi from "./api";

// Booking an appointment, step by step on one page (docs/PATIENT_AREA_REFRESH_PLAN.md, P.2): who it is for (the
// patient or a family member), the doctor, the day (today and the next 6) and a free time, the reason - with a
// summary beside the steps. The same page reschedules (state.rescheduleAppointment: the person stays the same).

const DAYS_AHEAD = 7;
const SHIFTS = [
  ["MORNING", "Morning", Sun],
  ["EVENING", "Evening", Moon],
];
const GENDERS = [
  ["FEMALE", "Female"],
  ["MALE", "Male"],
  ["OTHER", "Other"],
];
const RELATIONSHIP = (value) => (value ? value.charAt(0) + value.slice(1).toLowerCase() : "Family member");
const CHOICE = "rounded-xl border px-4 py-3 text-left transition-colors focus-within:ring-2 focus-within:ring-primary/40";

function Step({ number, title, children }) {
  return (
    <Card as="section" aria-labelledby={`step-${number}`}>
      <div className="mb-4 flex items-center gap-3">
        <span aria-hidden="true" className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-white">
          {number}
        </span>
        <h2 id={`step-${number}`} className="text-lg font-semibold">
          {title}
        </h2>
      </div>
      {children}
    </Card>
  );
}

const nextDays = () => Array.from({ length: DAYS_AHEAD }, (_, i) => toLocalISODate(addDays(new Date(), i)));

export default function AppointmentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const userId = useAuthStore((s) => s.userId);
  const reschedule = location.state?.rescheduleAppointment || null;
  const wantedDoctor = { id: location.state?.doctorId ?? reschedule?.doctorId, name: location.state?.doctorName ?? reschedule?.doctorName };

  const { doctors } = useDoctorDirectory();
  const [people, setPeople] = useState(null); // the patient and their family members
  const [peopleError, setPeopleError] = useState("");
  const [personKey, setPersonKey] = useState("");
  const [extra, setExtra] = useState({ gender: "", age: "" }); // when the profile doesn't say
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(toLocalISODate());
  const [shift, setShift] = useState(reschedule?.shift || "MORNING");
  const [slots, setSlots] = useState(null);
  const [slotError, setSlotError] = useState("");
  const [slotId, setSlotId] = useState(null);
  const [message, setMessage] = useState(reschedule?.message || "");
  const [saving, setSaving] = useState(false);
  const [booked, setBooked] = useState(null);
  const [refresh, setRefresh] = useState(0);

  // Who the appointment can be for
  useEffect(() => {
    if (reschedule || !userId) return;
    let current = true;
    (async () => {
      try {
        const { data: me } = await getPatientAccount(userId);
        const { data: relatives } = await getRelatives(me.patientId);
        if (!current) return;
        const list = [
          { key: "me", name: me.patientName, relation: "You", gender: me.gender, age: calculateAgeFromDOB(me.dob), relativeId: null, patientId: me.patientId },
          ...relatives.map((r) => ({
            key: `relative-${r.id}`, name: r.name, relation: RELATIONSHIP(r.relationship), gender: r.gender, age: calculateAgeFromDOB(r.dob),
            relativeId: r.id, patientId: me.patientId,
          })),
        ];
        setPeople(list);
        setPersonKey((key) => key || "me");
      } catch (err) {
        if (current) setPeopleError(getErrorMessage(err, "Could not load your profile."));
      }
    })();
    return () => {
      current = false;
    };
  }, [userId, reschedule]);

  // The doctor asked for (from a doctor's page, Book again or the visit being rescheduled)
  useEffect(() => {
    if (!doctors || doctorId) return;
    const match =
      doctors.find((d) => wantedDoctor.id && String(d.id) === String(wantedDoctor.id)) ||
      doctors.find((d) => wantedDoctor.name && d.name?.toLowerCase() === wantedDoctor.name.toLowerCase());
    if (match) setDoctorId(String(match.id));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doctors]);

  // The free times for the doctor, day and part of the day
  useEffect(() => {
    setSlotId(null);
    setSlotError("");
    if (!doctorId) {
      setSlots(null);
      return undefined;
    }
    let current = true;
    setSlots(null);
    appointmentsApi
      .getAvailableSlots(doctorId, date, shift)
      .then((res) => current && setSlots(res.data))
      .catch((err) => {
        if (!current) return;
        setSlots([]);
        setSlotError(getErrorMessage(err, "Unable to load the free times. Please try again."));
      });
    return () => {
      current = false;
    };
  }, [doctorId, date, shift, refresh]);

  const person = reschedule ? null : (people ?? []).find((p) => p.key === personKey);
  const needsGender = person && !person.relativeId && !person.gender;
  const needsAge = person && !person.relativeId && person.age === "";
  const doctor = (doctors ?? []).find((d) => String(d.id) === String(doctorId));
  const slot = (slots ?? []).find((s) => s.id === slotId);
  const ready = Boolean(slot) && (reschedule || (person && (!needsGender || extra.gender) && (!needsAge || extra.age !== "")));

  const confirm = async (e) => {
    e.preventDefault();
    if (!ready) return;
    setSaving(true);
    try {
      if (reschedule) {
        // Moves the appointment to the new time (the server releases the old one)
        await appointmentsApi.rescheduleAppointment(reschedule.appointmentID, { slotId, message: message.trim() });
        toast.success(`Moved to ${formatDate(date)}, ${formatTime(slot.startTime)}.`);
        navigate("/appointment-details");
        return;
      }
      await appointmentsApi.bookAppointment({
        patientName: person.name,
        gender: person.gender || extra.gender,
        age: Number(needsAge ? extra.age : person.age) || 0,
        message: message.trim(),
        relativeId: person.relativeId,
        slotId,
      });
      setBooked({ person: person.name, doctor, date, slot });
    } catch (err) {
      toast.error(getErrorMessage(err, "Failed to book the appointment. Please try again."));
      setRefresh((n) => n + 1); // the time may have been taken meanwhile
    } finally {
      setSaving(false);
    }
  };

  const bookAnother = () => {
    setBooked(null);
    setSlotId(null);
    setMessage("");
    setRefresh((n) => n + 1);
  };

  if (booked) {
    return (
      <PublicPage>
        <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6">
          <Card role="status" className="text-center sm:p-10">
            <CheckCircle2 size={48} aria-hidden="true" className="mx-auto text-success-ink" />
            <h1 className="mt-4 text-3xl font-semibold tracking-tight">Appointment booked</h1>
            <p className="mt-3 text-lg">
              {booked.person} with <strong>Dr. {booked.doctor?.name}</strong>
            </p>
            <p className="mt-1 text-lg">
              {formatDate(booked.date, true)}, {formatTime(booked.slot.startTime)}–{formatTime(booked.slot.endTime)}
            </p>
            <p className="mt-4 text-sm text-ink-muted">You'll get an SMS confirmation. Please check in at the front desk when you arrive.</p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Button as={Link} to="/appointment-details">
                View my appointments
              </Button>
              <Button variant="secondary" onClick={bookAnother}>
                Book another
              </Button>
            </div>
          </Card>
        </div>
      </PublicPage>
    );
  }

  return (
    <PublicPage>
      <PageIntro eyebrow={reschedule ? "Reschedule" : "Book an appointment"} title={reschedule ? "Choose a new time" : "Book in a few steps"}>
        {reschedule
          ? `For ${reschedule.patientName}, now ${formatDate(reschedule.date, true)}${reschedule.startTime ? `, ${formatTime(reschedule.startTime)}` : ""}.`
          : "Choose who it's for, the doctor and a free time. You'll get an SMS confirmation."}
      </PageIntro>

      <form onSubmit={confirm} className="mx-auto grid max-w-7xl grid-cols-1 gap-6 px-4 py-12 sm:px-6 lg:grid-cols-3 lg:px-8">
        <div className="min-w-0 space-y-6 lg:col-span-2">
          {!reschedule && (
            <Step number={1} title="Who is the appointment for?">
              <Notice tone="danger">{peopleError}</Notice>
              {!people && !peopleError && <p className="text-sm text-ink-muted">Loading...</p>}
              {people && (
                <fieldset>
                  <legend className="sr-only">Patient</legend>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {people.map((p) => (
                      <label
                        key={p.key}
                        className={`${CHOICE} flex cursor-pointer items-center gap-3 ${
                          personKey === p.key ? "border-primary bg-primary-ink/10" : "border-line hover:bg-surface-2"
                        }`}
                      >
                        <input type="radio" name="person" value={p.key} checked={personKey === p.key} onChange={() => setPersonKey(p.key)} className="h-4 w-4 accent-primary" />
                        <span className="min-w-0">
                          <span className="block font-medium">{p.name}</span>
                          <span className="block text-sm text-ink-muted">
                            {[p.relation, p.age !== "" ? `${p.age} y` : null].filter(Boolean).join(" · ")}
                          </span>
                        </span>
                      </label>
                    ))}
                  </div>
                </fieldset>
              )}
              {(needsGender || needsAge) && (
                <div className="mt-4 grid gap-4 sm:grid-cols-2">
                  {needsGender && (
                    <Field label="Gender" htmlFor="booking-gender">
                      <Select id="booking-gender" value={extra.gender} onChange={(e) => setExtra({ ...extra, gender: e.target.value })}>
                        <option value="">Choose</option>
                        {GENDERS.map(([value, label]) => (
                          <option key={value} value={value}>
                            {label}
                          </option>
                        ))}
                      </Select>
                    </Field>
                  )}
                  {needsAge && (
                    <Field label="Age" htmlFor="booking-age" hint="Add your date of birth in My account to skip this next time.">
                      <Input id="booking-age" type="number" min={0} max={130} value={extra.age} onChange={(e) => setExtra({ ...extra, age: e.target.value })} />
                    </Field>
                  )}
                </div>
              )}
              <Link to="/add-relative" state={{ from: "/appointments" }} className="mt-4 inline-flex items-center gap-1.5 text-sm font-medium text-primary-ink hover:underline">
                <UserPlus size={16} aria-hidden="true" />
                Add a family member
              </Link>
            </Step>
          )}

          <Step number={reschedule ? 1 : 2} title="Choose the doctor">
            <Field label="Doctor" htmlFor="booking-doctor">
              <Select id="booking-doctor" value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
                <option value="">{doctors ? "Choose a doctor" : "Loading doctors..."}</option>
                {(doctors ?? []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {doctorLabel(d)}
                  </option>
                ))}
              </Select>
            </Field>
            <Link to="/doctors" className="mt-3 inline-block text-sm font-medium text-primary-ink hover:underline">
              Not sure? See the doctors and their specialities
            </Link>
          </Step>

          <Step number={reschedule ? 2 : 3} title="Day and time">
            <div role="group" aria-label="Day" className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
              {nextDays().map((day) => (
                <button
                  key={day}
                  type="button"
                  aria-pressed={date === day}
                  onClick={() => setDate(day)}
                  className={`whitespace-nowrap rounded-full border px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                    date === day ? "border-primary bg-primary text-white" : "border-line text-ink-muted hover:text-ink"
                  }`}
                >
                  {dayLabel(day)}
                </button>
              ))}
            </div>
            <div role="group" aria-label="Part of the day" className="mt-4 inline-flex rounded-lg border border-line bg-surface-2 p-1">
              {SHIFTS.map(([value, label, Icon]) => (
                <button
                  key={value}
                  type="button"
                  aria-pressed={shift === value}
                  onClick={() => setShift(value)}
                  className={`inline-flex items-center gap-2 rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                    shift === value ? "bg-surface text-ink shadow-sm" : "text-ink-muted hover:text-ink"
                  }`}
                >
                  <Icon size={16} aria-hidden="true" />
                  {label}
                </button>
              ))}
            </div>

            <div className="mt-5">
              {!doctorId && <p className="text-sm text-ink-muted">Choose a doctor to see the free times.</p>}
              {doctorId && slots === null && <p className="text-sm text-ink-muted">Loading free times...</p>}
              <Notice tone="danger">{slotError}</Notice>
              {slots && slots.length === 0 && !slotError && (
                <Notice tone="info">No free times then. Try another day or the {shift === "MORNING" ? "evening" : "morning"}.</Notice>
              )}
              {slots && slots.length > 0 && (
                <fieldset>
                  <legend className="mb-2 text-sm font-medium">Free times</legend>
                  <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-6">
                    {slots.map((s) => (
                      <label
                        key={s.id}
                        className={`cursor-pointer rounded-lg border px-2 py-2 text-center text-sm font-medium tabular-nums transition-colors focus-within:ring-2 focus-within:ring-primary/40 ${
                          slotId === s.id ? "border-primary bg-primary text-white" : "border-line hover:bg-surface-2"
                        }`}
                      >
                        <input type="radio" name="slot" value={s.id} checked={slotId === s.id} onChange={() => setSlotId(s.id)} className="sr-only" />
                        {formatTime(s.startTime)}
                      </label>
                    ))}
                  </div>
                </fieldset>
              )}
            </div>
          </Step>

          <Step number={reschedule ? 3 : 4} title="Reason for the visit">
            <Field label="What would you like to see the doctor about? (optional)" htmlFor="booking-message">
              <Textarea id="booking-message" rows={3} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="e.g. Fever and cough for three days" />
            </Field>
          </Step>
        </div>

        <aside className="min-w-0 lg:sticky lg:top-24 lg:self-start">
          <Card aria-label="Summary">
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <CalendarCheck size={20} aria-hidden="true" className="text-primary-ink" />
              Summary
            </h2>
            <dl className="mt-4 space-y-3 text-sm">
              <div>
                <dt className="text-ink-muted">For</dt>
                <dd className="font-medium">{reschedule ? reschedule.patientName : person?.name || "—"}</dd>
              </div>
              <div>
                <dt className="text-ink-muted">Doctor</dt>
                <dd className="font-medium">{doctor ? doctorLabel(doctor) : "—"}</dd>
              </div>
              <div>
                <dt className="text-ink-muted">When</dt>
                <dd className="font-medium">
                  {slot ? `${formatDate(date)}, ${formatTime(slot.startTime)}–${formatTime(slot.endTime)}` : "Choose a time"}
                </dd>
              </div>
            </dl>
            <Button type="submit" size="lg" className="mt-6 w-full" disabled={!ready || saving}>
              {saving ? (reschedule ? "Moving..." : "Booking...") : reschedule ? "Move appointment" : "Confirm booking"}
            </Button>
          </Card>
        </aside>
      </form>
    </PublicPage>
  );
}
