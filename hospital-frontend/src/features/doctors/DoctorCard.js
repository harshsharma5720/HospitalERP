import React from "react";
import { Link } from "react-router-dom";
import { API_BASE_URL } from "../../app/config";
import { Button } from "../../shared/ui";
import { specialityLabel } from "../appointments/appointmentFormat";

const initials = (name) =>
  (name || "?")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join("");

// A doctor's photo, or their initials
export function DoctorAvatar({ doctor, size = "md" }) {
  const box = size === "lg" ? "h-28 w-28 text-3xl" : "h-20 w-20 text-xl";
  if (doctor.profileImage) {
    return <img src={`${API_BASE_URL}${doctor.profileImage}`} alt="" className={`${box} shrink-0 rounded-full object-cover ring-4 ring-primary-ink/10`} />;
  }
  return (
    <span aria-hidden="true" className={`${box} flex shrink-0 items-center justify-center rounded-full bg-primary-ink/10 font-semibold text-primary-ink`}>
      {initials(doctor.name)}
    </span>
  );
}

// One doctor in a grid of the public site: photo, name, speciality, their profile and - with onBook - Book
export default function DoctorCard({ doctor, onBook }) {
  return (
    <div className="flex h-full flex-col items-center rounded-2xl border border-line bg-surface p-6 text-center shadow-card">
      <DoctorAvatar doctor={doctor} />
      <h3 className="mt-4 font-semibold">Dr. {doctor.name}</h3>
      <p className="text-sm text-ink-muted">{specialityLabel(doctor.specialist) || "General"}</p>
      <div className="mt-5 flex w-full flex-wrap justify-center gap-2">
        <Button as={Link} to={`/doctor-profile/${doctor.id}`} variant="secondary" size="sm" aria-label={`View profile of Dr. ${doctor.name}`}>
          View profile
        </Button>
        {onBook && (
          <Button size="sm" onClick={() => onBook(doctor)} aria-label={`Book with Dr. ${doctor.name}`}>
            Book
          </Button>
        )}
      </div>
    </div>
  );
}
