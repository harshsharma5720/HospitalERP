import React, { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import { ABHA_ERROR, ABHA_HINT, abhaInputProps, isAbhaOrEmpty } from "../../shared/utils/abha";
import useAreaBase from "../../shared/useAreaBase";
import { getAllDoctors } from "../doctors/api";
import { getFrontDeskPatient } from "../patients/api";
import { dayLabel, doctorLabel, formatDate, formatTime } from "./appointmentFormat";
import * as appointmentsApi from "./api";
import { CheckCircle2 } from "lucide-react";
import { Button, Card, Field, Input, Notice, PageHeader, Select } from "../../shared/ui";

// Walk-in patients at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md): look the phone number up, book for one
// of the patients found or register a new one, pick a doctor and one of their next free slots (the earliest is
// preselected), book. Used by receptionists (/walk-in) and admins (/admin/walk-in). Opened from a patient's page
// (?patientId=41, docs/PATIENT_SEARCH_PLAN.md) that patient is already chosen.

const SLOTS_SHOWN = 6;
const MORE_SLOTS = 20;

const GENDER_LABELS = { FEMALE: "Female", MALE: "Male", OTHER: "Other" };

const CHOICE = "rounded-lg border transition-colors";
const CHOSEN = "border-primary bg-primary-ink/10";
const NOT_CHOSEN = "border-line hover:bg-surface-2";

// A numbered step's title (the number is decoration; the title is the heading or label)
function Step({ number, children }) {
  return (
    <div className="mb-4 flex items-center gap-3">
      <span aria-hidden="true" className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-white">
        {number}
      </span>
      {children}
    </div>
  );
}

const EMPTY_NEW_PATIENT = { name: "", gender: "", dob: "", email: "", abhaNumber: "" };

export default function WalkInBooking() {
  const base = useAreaBase();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const preselectedId = params.get("patientId");
  const preselected = location.state?.patient; // passed by the patient page, saves loading it again
  const [doctors, setDoctors] = useState([]);
  const [phone, setPhone] = useState("");
  const [searchedPhone, setSearchedPhone] = useState("");
  const [matches, setMatches] = useState(null); // null: not searched yet
  const [searching, setSearching] = useState(false);
  const [patient, setPatient] = useState(null); // a match, or "new"
  const [newPatient, setNewPatient] = useState(EMPTY_NEW_PATIENT);
  const [doctorId, setDoctorId] = useState("");
  const [slots, setSlots] = useState(null); // null: no doctor chosen or loading
  const [slotLimit, setSlotLimit] = useState(SLOTS_SHOWN);
  const [slotId, setSlotId] = useState(null);
  const [age, setAge] = useState("");
  const [message, setMessage] = useState("");
  const [booking, setBooking] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState(null);

  useEffect(() => {
    getAllDoctors()
      .then((response) => setDoctors(response.data.filter((doctor) => doctor.active !== false)))
      .catch((err) => setError(getErrorMessage(err, "Could not load the doctors.")));
  }, []);

  // Opened for one patient: as if their number had been looked up and they had been chosen
  useEffect(() => {
    if (!preselectedId) return;
    const load =
      preselected && String(preselected.patientId) === preselectedId
        ? Promise.resolve({ data: preselected })
        : getFrontDeskPatient(preselectedId);
    load
      .then(({ data }) => {
        setPhone(data.contactNo ?? "");
        setSearchedPhone(data.contactNo ?? "");
        setMatches([data]);
        setPatient(data);
        setNewPatient(EMPTY_NEW_PATIENT);
        setAge(data.dob ? String(calculateAgeFromDOB(data.dob)) : "");
      })
      .catch((err) => setError(getErrorMessage(err, "Could not load the patient.")));
  }, [preselectedId, preselected]);

  // keep: the slot to stay chosen if it is still free
  const loadSlots = useCallback(async (forDoctor, limit, keep = null) => {
    setSlots(null);
    setSlotId(null);
    if (!forDoctor) return;
    try {
      const response = await appointmentsApi.getNextFreeSlots(forDoctor, limit);
      setSlots(response.data);
      setSlotId(response.data.some((slot) => slot.slotId === keep) ? keep : response.data[0]?.slotId ?? null); // else the earliest
    } catch (err) {
      setSlots([]);
      setError(getErrorMessage(err, "Could not load the free slots."));
    }
  }, []);

  const search = async (e) => {
    e.preventDefault();
    setError("");
    setConfirmation(null);
    setPatient(null);
    setMatches(null);
    setSearching(true);
    try {
      const response = await appointmentsApi.findPatientsByPhone(phone.trim());
      setSearchedPhone(phone.trim());
      setMatches(response.data);
      if (response.data.length === 0) choosePatient("new");
    } catch (err) {
      setError(getErrorMessage(err, "Could not search for the phone number."));
    } finally {
      setSearching(false);
    }
  };

  const choosePatient = (chosen) => {
    setError("");
    setPatient(chosen);
    setNewPatient(EMPTY_NEW_PATIENT);
    setAge(chosen !== "new" && chosen.dob ? String(calculateAgeFromDOB(chosen.dob)) : "");
  };

  const chooseDoctor = (e) => {
    setError("");
    setDoctorId(e.target.value);
    setSlotLimit(SLOTS_SHOWN);
    loadSlots(e.target.value, SLOTS_SHOWN);
  };

  const showMoreSlots = () => {
    setSlotLimit(MORE_SLOTS);
    loadSlots(doctorId, MORE_SLOTS);
  };

  const changeNewPatient = (field) => (e) => setNewPatient({ ...newPatient, [field]: e.target.value });

  const book = async (e) => {
    e.preventDefault();
    setError("");
    const isNew = patient === "new";
    if (isNew && (!newPatient.name.trim() || !newPatient.gender)) {
      setError("Please enter the patient's name and gender.");
      return;
    }
    if (isNew && !isAbhaOrEmpty(newPatient.abhaNumber)) {
      setError(ABHA_ERROR);
      return;
    }
    if (!slotId) {
      setError("Please choose a doctor and a time.");
      return;
    }
    if (age === "" && !(isNew ? newPatient.dob : patient.dob)) {
      setError("Please enter the patient's age.");
      return;
    }
    const request = {
      slotId,
      age: age === "" ? null : Number(age),
      message: message.trim() || null,
    };
    if (isNew) {
      request.newPatient = {
        name: newPatient.name.trim(),
        phone: searchedPhone,
        gender: newPatient.gender,
        dob: newPatient.dob || null,
        email: newPatient.email.trim() || null,
        abhaNumber: newPatient.abhaNumber.trim() || null,
      };
    } else {
      request.patientId = patient.patientId;
    }
    setBooking(true);
    try {
      const response = await appointmentsApi.bookWalkIn(request);
      const doctor = doctors.find((d) => String(d.id) === String(doctorId));
      setConfirmation({ ...response.data, phone: searchedPhone, doctor });
    } catch (err) {
      setError(getErrorMessage(err, "Could not book the appointment."));
      // The slot gone (or the ABHA number taken): show what is free now, the chosen time kept if it still is
      if (err?.response?.status === 409) loadSlots(doctorId, slotLimit, slotId);
    } finally {
      setBooking(false);
    }
  };

  const nextPatient = () => {
    if (preselectedId) setParams({}, { replace: true });
    setPhone("");
    setSearchedPhone("");
    setMatches(null);
    setPatient(null);
    setNewPatient(EMPTY_NEW_PATIENT);
    setDoctorId("");
    setSlots(null);
    setSlotId(null);
    setAge("");
    setMessage("");
    setError("");
    setConfirmation(null);
  };

  if (confirmation) {
    const { appointment } = confirmation;
    return (
      <div className="max-w-3xl">
        <PageHeader title="Walk-in booking" />
        <Card role="status" className="mb-5 border-l-4 border-l-success">
          <div className="flex items-start gap-3">
            <CheckCircle2 size={22} aria-hidden="true" className="mt-0.5 shrink-0 text-success-ink" />
            <div>
              <h2 className="mb-2 text-lg font-semibold">Booked</h2>
              <p className="mb-1">
                <strong>{appointment.patientName}</strong> with{" "}
                <strong>{confirmation.doctor ? doctorLabel(confirmation.doctor) : `Dr. ${appointment.doctorName}`}</strong>
              </p>
              <p className="mb-1">
                {formatDate(appointment.date, true)}, {formatTime(appointment.startTime)}–{formatTime(appointment.endTime)}
              </p>
              <p className="text-sm text-ink-muted">
                {confirmation.newPatient ? "New patient record created. " : ""}
                An SMS confirmation is sent to {confirmation.phone}.
              </p>
              {appointment.tokenNumber && (
                <p className="mt-3 text-lg font-semibold">Checked in for today: token {appointment.tokenNumber}</p>
              )}
              <Link to={`${base}/patients/${confirmation.patientId}`} className="mt-3 inline-block text-sm font-medium text-primary-ink hover:underline">
                Open the patient's page
              </Link>
            </div>
          </div>
        </Card>
        <Button onClick={nextPatient}>Next patient</Button>
      </div>
    );
  }

  const isNew = patient === "new";
  const knownDob = isNew ? newPatient.dob : patient?.dob;

  return (
    <div className="max-w-3xl">
      <PageHeader
        title="Walk-in booking"
        description="Look the phone number up first: the patient may already be registered, or share the number with family."
      />

      {/* 1. Phone number */}
      <Card className="mb-5">
        <form onSubmit={search}>
          <Step number={1}>
            <label htmlFor="walkin-phone" className="font-semibold">
              Phone number
            </label>
          </Step>
          <div className="flex gap-2">
            <Input
              id="walkin-phone"
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+91 98765 43210"
              required
            />
            <Button type="submit" disabled={searching}>
              {searching ? "Searching..." : "Find"}
            </Button>
          </div>
        </form>
      </Card>

      <Notice tone="danger" className="mb-5">{error}</Notice>

      {/* 2. Patient */}
      {matches && (
        <Card className="mb-5">
          <Step number={2}>
            <h2 className="font-semibold">Patient</h2>
          </Step>
          {matches.length === 0 ? (
            <p className="mb-3 text-sm text-ink-muted">No patient with this number yet — register a new patient.</p>
          ) : (
            <ul className="mb-3 space-y-2">
              {matches.map((match) => (
                <li
                  key={match.patientId}
                  className={`flex items-center justify-between gap-3 p-3 ${CHOICE} ${patient === match ? CHOSEN : NOT_CHOSEN}`}
                >
                  <div>
                    <p className="font-medium">{match.patientName}</p>
                    <p className="text-sm text-ink-muted">
                      {[
                        GENDER_LABELS[match.gender],
                        match.dob ? `${calculateAgeFromDOB(match.dob)} years` : null,
                        match.email,
                        match.hasLogin ? "App account" : "Front-desk record",
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  </div>
                  <Button
                    size="sm"
                    variant={patient === match ? "primary" : "secondary"}
                    aria-pressed={patient === match}
                    onClick={() => choosePatient(match)}
                  >
                    Book for {match.patientName}
                  </Button>
                </li>
              ))}
            </ul>
          )}
          {matches.length > 0 && (
            <Button size="sm" variant={isNew ? "primary" : "secondary"} aria-pressed={isNew} onClick={() => choosePatient("new")}>
              Someone else — new patient
            </Button>
          )}

          {isNew && (
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <p className="text-sm sm:col-span-2">
                Phone: <strong>{searchedPhone}</strong>
              </p>
              <Field label="Name" htmlFor="walkin-name">
                <Input id="walkin-name" value={newPatient.name} onChange={changeNewPatient("name")} maxLength={100} />
              </Field>
              <Field label="Gender" htmlFor="walkin-gender">
                <Select id="walkin-gender" value={newPatient.gender} onChange={changeNewPatient("gender")}>
                  <option value="">Choose</option>
                  {Object.entries(GENDER_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="Date of birth (optional)" htmlFor="walkin-dob">
                <Input id="walkin-dob" type="date" value={newPatient.dob} onChange={changeNewPatient("dob")} max={toLocalISODate()} />
              </Field>
              <Field label="Email (optional)" htmlFor="walkin-email">
                <Input id="walkin-email" type="email" value={newPatient.email} onChange={changeNewPatient("email")} />
              </Field>
              <Field label="ABHA number (optional)" htmlFor="walkin-abha" hint={ABHA_HINT}>
                <Input id="walkin-abha" value={newPatient.abhaNumber} onChange={changeNewPatient("abhaNumber")} {...abhaInputProps} />
              </Field>
            </div>
          )}
        </Card>
      )}

      {/* 3. Doctor, time and visit */}
      {patient && (
        <Card>
          <form onSubmit={book}>
            <Step number={3}>
              <h2 className="font-semibold">Visit</h2>
            </Step>
            <Field label="Doctor" htmlFor="walkin-doctor" className="mb-4">
              <Select id="walkin-doctor" value={doctorId} onChange={chooseDoctor}>
                <option value="">Choose a doctor</option>
                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.id}>
                    {doctorLabel(doctor)}
                  </option>
                ))}
              </Select>
            </Field>

            {doctorId && slots === null && <p className="mb-4 text-sm text-ink-muted">Loading free times...</p>}
            {slots && slots.length === 0 && (
              <Notice tone="warning" className="mb-4">No free times in the next 30 days — please choose another doctor.</Notice>
            )}
            {slots && slots.length > 0 && (
              <fieldset className="mb-4">
                <legend className="mb-1.5 text-sm font-medium">Time (the earliest is chosen)</legend>
                <div className="flex flex-wrap gap-2">
                  {slots.map((slot) => (
                    <label
                      key={slot.slotId}
                      className={`cursor-pointer px-3 py-1.5 text-sm font-medium focus-within:ring-2 focus-within:ring-primary/40 ${CHOICE} ${
                        slotId === slot.slotId ? "border-primary bg-primary text-white" : NOT_CHOSEN
                      }`}
                    >
                      <input
                        type="radio"
                        name="walkin-slot"
                        value={slot.slotId}
                        checked={slotId === slot.slotId}
                        onChange={() => setSlotId(slot.slotId)}
                        className="sr-only"
                      />
                      {dayLabel(slot.date)} {formatTime(slot.startTime)}
                    </label>
                  ))}
                  {slotLimit === SLOTS_SHOWN && slots.length === SLOTS_SHOWN && (
                    <Button variant="ghost" size="sm" onClick={showMoreSlots}>
                      More times
                    </Button>
                  )}
                </div>
              </fieldset>
            )}

            <div className="mb-5 grid gap-4 sm:grid-cols-2">
              <Field label="Age" htmlFor="walkin-age" hint={knownDob && age === "" ? "Empty: taken from the date of birth." : null}>
                <Input id="walkin-age" type="number" min={0} max={130} value={age} onChange={(e) => setAge(e.target.value)} />
              </Field>
              <Field label="Reason for the visit (optional)" htmlFor="walkin-message">
                <Input id="walkin-message" value={message} onChange={(e) => setMessage(e.target.value)} />
              </Field>
            </div>

            <Button type="submit" variant="success" disabled={booking}>
              {booking ? "Booking..." : isNew ? "Register and book" : "Book"}
            </Button>
          </form>
        </Card>
      )}
    </div>
  );
}
