import React, { useCallback, useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";
import { getAllDoctors } from "../doctors/api";
import * as appointmentsApi from "./api";

// Walk-in patients at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md): look the phone number up, book for one
// of the patients found or register a new one, pick a doctor and one of their next free slots (the earliest is
// preselected), book. Used by receptionists (/walk-in) and admins (/admin/walk-in).

const SLOTS_SHOWN = 6;
const MORE_SLOTS = 20;

const GENDER_LABELS = { FEMALE: "Female", MALE: "Male", OTHER: "Other" };
const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

const INPUT =
  "w-full p-2 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2]";
const CARD = "bg-white dark:bg-[#1e293b] shadow-md rounded-lg p-5 mb-5";
const QUIET_BORDER = "border-gray-300 dark:border-gray-600";

// "2026-10-09" -> "Fri 9 Oct" (+ " 2026" with the year)
export const formatDate = (isoDate, withYear = false) => {
  const [year, month, day] = isoDate.split("-").map(Number);
  const weekday = DAYS[new Date(year, month - 1, day).getDay()];
  return `${weekday} ${day} ${MONTHS[month - 1]}${withYear ? ` ${year}` : ""}`;
};

// "Today", "Tomorrow" or "Fri 9 Oct"
export const dayLabel = (isoDate, today = new Date()) => {
  if (isoDate === toLocalISODate(today)) return "Today";
  if (isoDate === toLocalISODate(addDays(today, 1))) return "Tomorrow";
  return formatDate(isoDate);
};

const time = (t) => String(t).slice(0, 5); // "10:00:00" -> "10:00"

const doctorLabel = (doctor) => {
  const name = `Dr. ${doctor.name || doctor.userName}`;
  if (!doctor.specialist || doctor.specialist === "NOT_ASSIGNED") return name;
  const specialist = doctor.specialist.toLowerCase().replace(/_/g, " ");
  return `${name} — ${specialist.charAt(0).toUpperCase()}${specialist.slice(1)}`;
};

const EMPTY_NEW_PATIENT = { name: "", gender: "", dob: "", email: "" };

export default function WalkInBooking() {
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

  const loadSlots = useCallback(async (forDoctor, limit) => {
    setSlots(null);
    setSlotId(null);
    if (!forDoctor) return;
    try {
      const response = await appointmentsApi.getNextFreeSlots(forDoctor, limit);
      setSlots(response.data);
      setSlotId(response.data[0]?.slotId ?? null); // the earliest
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
      if (err?.response?.status === 409) loadSlots(doctorId, slotLimit); // the slot is gone: show what is free now
    } finally {
      setBooking(false);
    }
  };

  const nextPatient = () => {
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
      <div className="p-6 max-w-3xl">
        <h1 className="text-3xl font-bold mb-4">Walk-in booking</h1>
        <div role="status" className={`${CARD} border-l-4 border-green-600`}>
          <h2 className="text-xl font-semibold mb-2">Booked</h2>
          <p className="mb-1">
            <strong>{appointment.patientName}</strong> with{" "}
            <strong>{confirmation.doctor ? doctorLabel(confirmation.doctor) : `Dr. ${appointment.doctorName}`}</strong>
          </p>
          <p className="mb-1">
            {formatDate(appointment.date, true)}, {time(appointment.startTime)}–{time(appointment.endTime)}
          </p>
          <p className="text-sm text-gray-600 dark:text-gray-300">
            {confirmation.newPatient ? "New patient record created. " : ""}
            An SMS confirmation is sent to {confirmation.phone}.
          </p>
        </div>
        <button type="button" onClick={nextPatient} className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded">
          Next patient
        </button>
      </div>
    );
  }

  const isNew = patient === "new";
  const knownDob = isNew ? newPatient.dob : patient?.dob;

  return (
    <div className="p-6 max-w-3xl">
      <h1 className="text-3xl font-bold mb-2">Walk-in booking</h1>
      <p className="text-gray-600 dark:text-gray-300 mb-5">
        Look the phone number up first: the patient may already be registered, or share the number with family.
      </p>

      {/* 1. Phone number */}
      <form onSubmit={search} className={CARD}>
        <label htmlFor="walkin-phone" className="block font-semibold mb-2">
          Phone number
        </label>
        <div className="flex gap-3">
          <input
            id="walkin-phone"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+91 98765 43210"
            className={INPUT}
            required
          />
          <button
            type="submit"
            disabled={searching}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded disabled:opacity-50"
          >
            {searching ? "Searching..." : "Find"}
          </button>
        </div>
      </form>

      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}

      {/* 2. Patient */}
      {matches && (
        <div className={CARD}>
          <h2 className="font-semibold mb-3">Patient</h2>
          {matches.length === 0 ? (
            <p className="text-sm text-gray-600 dark:text-gray-300 mb-3">
              No patient with this number yet — register a new patient.
            </p>
          ) : (
            <ul className="mb-3 space-y-2">
              {matches.map((match) => (
                <li
                  key={match.patientId}
                  className={`flex items-center justify-between gap-3 border rounded p-3 ${
                    patient === match ? "border-blue-600 bg-blue-50 dark:bg-[#16224a]" : QUIET_BORDER
                  }`}
                >
                  <div>
                    <p className="font-semibold">{match.patientName}</p>
                    <p className="text-sm text-gray-600 dark:text-gray-300">
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
                  <button
                    type="button"
                    aria-pressed={patient === match}
                    onClick={() => choosePatient(match)}
                    className="border border-blue-600 text-blue-700 dark:text-[#50d4f2] px-3 py-1 rounded text-sm whitespace-nowrap"
                  >
                    Book for {match.patientName}
                  </button>
                </li>
              ))}
            </ul>
          )}
          {matches.length > 0 && (
            <button
              type="button"
              aria-pressed={isNew}
              onClick={() => choosePatient("new")}
              className={`border px-3 py-1 rounded text-sm ${isNew ? "border-blue-600 bg-blue-50 dark:bg-[#16224a]" : QUIET_BORDER}`}
            >
              Someone else — new patient
            </button>
          )}

          {isNew && (
            <div className="grid sm:grid-cols-2 gap-3 mt-3">
              <p className="sm:col-span-2 text-sm">
                Phone: <strong>{searchedPhone}</strong>
              </p>
              <div>
                <label htmlFor="walkin-name" className="block text-sm">Name</label>
                <input id="walkin-name" value={newPatient.name} onChange={changeNewPatient("name")} maxLength={100}
                       className={INPUT} />
              </div>
              <div>
                <label htmlFor="walkin-gender" className="block text-sm">Gender</label>
                <select id="walkin-gender" value={newPatient.gender} onChange={changeNewPatient("gender")} className={INPUT}>
                  <option value="">Choose</option>
                  {Object.entries(GENDER_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="walkin-dob" className="block text-sm">Date of birth (optional)</label>
                <input id="walkin-dob" type="date" value={newPatient.dob} onChange={changeNewPatient("dob")}
                       max={toLocalISODate()} className={INPUT} />
              </div>
              <div>
                <label htmlFor="walkin-email" className="block text-sm">Email (optional)</label>
                <input id="walkin-email" type="email" value={newPatient.email} onChange={changeNewPatient("email")}
                       className={INPUT} />
              </div>
            </div>
          )}
        </div>
      )}

      {/* 3. Doctor, time and visit */}
      {patient && (
        <form onSubmit={book} className={CARD}>
          <h2 className="font-semibold mb-3">Visit</h2>
          <label htmlFor="walkin-doctor" className="block text-sm">Doctor</label>
          <select id="walkin-doctor" value={doctorId} onChange={chooseDoctor} className={`${INPUT} mb-3`}>
            <option value="">Choose a doctor</option>
            {doctors.map((doctor) => (
              <option key={doctor.id} value={doctor.id}>{doctorLabel(doctor)}</option>
            ))}
          </select>

          {doctorId && slots === null && <p className="text-sm mb-3">Loading free times...</p>}
          {slots && slots.length === 0 && (
            <p className="text-sm mb-3">No free times in the next 30 days — please choose another doctor.</p>
          )}
          {slots && slots.length > 0 && (
            <fieldset className="mb-3">
              <legend className="text-sm mb-1">Time (the earliest is chosen)</legend>
              <div className="flex flex-wrap gap-2">
                {slots.map((slot) => (
                  <label
                    key={slot.slotId}
                    className={`cursor-pointer border rounded px-3 py-1 text-sm ${
                      slotId === slot.slotId ? "bg-blue-600 text-white border-blue-600" : QUIET_BORDER
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
                    {dayLabel(slot.date)} {time(slot.startTime)}
                  </label>
                ))}
                {slotLimit === SLOTS_SHOWN && slots.length === SLOTS_SHOWN && (
                  <button type="button" onClick={showMoreSlots} className="text-sm underline px-2">
                    More times
                  </button>
                )}
              </div>
            </fieldset>
          )}

          <div className="grid sm:grid-cols-2 gap-3 mb-3">
            <div>
              <label htmlFor="walkin-age" className="block text-sm">Age</label>
              <input id="walkin-age" type="number" min={0} max={130} value={age} onChange={(e) => setAge(e.target.value)}
                     className={INPUT} />
              {knownDob && age === "" && (
                <p className="text-xs text-gray-500 mt-1">Empty: taken from the date of birth.</p>
              )}
            </div>
            <div>
              <label htmlFor="walkin-message" className="block text-sm">Reason for the visit (optional)</label>
              <input id="walkin-message" value={message} onChange={(e) => setMessage(e.target.value)} className={INPUT} />
            </div>
          </div>

          <button
            type="submit"
            disabled={booking}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded disabled:opacity-50"
          >
            {booking ? "Booking..." : isNew ? "Register and book" : "Book"}
          </button>
        </form>
      )}
    </div>
  );
}
