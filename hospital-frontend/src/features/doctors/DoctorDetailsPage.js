import React, { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Check, Mail, Phone, UserX } from "lucide-react";
import { PublicPage } from "../../shared/layout/PublicSections";
import { HOSPITAL, phoneHref } from "../../shared/hospitalInfo";
import { Badge, Button, Card, EmptyState, Notice, Reveal } from "../../shared/ui";
import { specialityLabel } from "../appointments/appointmentFormat";
import DoctorCard, { DoctorAvatar } from "./DoctorCard";
import useBooking from "./useBooking";
import useDoctorDirectory from "./useDoctorDirectory";
import { departmentOf } from "./departments";
import { getPublicDoctor } from "./api";

const OTHERS_SHOWN = 4;

// Public profile of one doctor (opened from the doctor list): what the directory knows about them, their
// department, Book, and other doctors in the same speciality
export default function DoctorDetailsPage() {
  const { doctorId } = useParams();
  const [doctor, setDoctor] = useState(null);
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(true);
  const { doctors } = useDoctorDirectory();
  const { book, loginPrompt } = useBooking();

  const load = useCallback(() => {
    let current = true;
    setLoading(true);
    setError(false);
    getPublicDoctor(doctorId)
      .then((response) => current && setDoctor(response.data))
      .catch(() => current && setError(true))
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [doctorId]);

  useEffect(() => load(), [load]);

  const back = (
    <Link to={doctor?.specialist ? `/doctors?speciality=${doctor.specialist}` : "/doctors"} className="text-sm text-primary-ink hover:underline">
      ← All doctors
    </Link>
  );

  if (loading || error || !doctor) {
    return (
      <PublicPage>
        <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="mb-6">{back}</div>
          {loading ? (
            <div aria-busy="true" className="h-72 animate-pulse rounded-2xl border border-line bg-surface">
              <span className="sr-only">Loading the doctor's profile...</span>
            </div>
          ) : (
            <Card padding="none">
              <EmptyState
                icon={UserX}
                title="We couldn't load this doctor"
                action={
                  <Button variant="secondary" onClick={load}>
                    Try again
                  </Button>
                }
              >
                Please try again in a moment, or call us on {HOSPITAL.phone}.
              </EmptyState>
            </Card>
          )}
        </div>
      </PublicPage>
    );
  }

  const department = departmentOf(doctor.specialist);
  const active = doctor.active !== false;
  const others = (doctors ?? []).filter((d) => d.specialist === doctor.specialist && d.id !== doctor.id).slice(0, OTHERS_SHOWN);

  return (
    <PublicPage>
      <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="mb-6">{back}</div>

        <Reveal>
          <Card className="sm:p-8">
            <div className="flex flex-col items-center gap-6 text-center sm:flex-row sm:items-start sm:text-left">
              <DoctorAvatar doctor={doctor} size="lg" />
              <div className="min-w-0 flex-1">
                <h1 className="text-3xl font-semibold tracking-tight">Dr. {doctor.name}</h1>
                <div className="mt-2">
                  <Badge tone="primary">{specialityLabel(doctor.specialist) || "General"}</Badge>
                </div>
                <ul className="mt-5 space-y-2 text-sm text-ink-muted">
                  {doctor.email && (
                    <li className="flex items-center justify-center gap-2 sm:justify-start">
                      <Mail size={16} aria-hidden="true" className="text-primary-ink" />
                      <a href={`mailto:${doctor.email}`} className="hover:text-ink">
                        {doctor.email}
                      </a>
                    </li>
                  )}
                  {doctor.phoneNumber && (
                    <li className="flex items-center justify-center gap-2 sm:justify-start">
                      <Phone size={16} aria-hidden="true" className="text-primary-ink" />
                      <a href={`tel:${String(doctor.phoneNumber).replace(/\s/g, "")}`} className="hover:text-ink">
                        {doctor.phoneNumber}
                      </a>
                    </li>
                  )}
                </ul>
              </div>
              <div className="flex w-full flex-col gap-2 sm:w-auto">
                {active ? (
                  <Button size="lg" onClick={() => book(doctor)}>
                    Book an appointment
                  </Button>
                ) : null}
                <Button as="a" href={phoneHref} size="lg" variant="secondary">
                  <Phone size={18} aria-hidden="true" />
                  Call the hospital
                </Button>
              </div>
            </div>
            {!active && (
              <Notice tone="warning" className="mt-6">
                Dr. {doctor.name} no longer takes appointments here.
              </Notice>
            )}
          </Card>
        </Reveal>

        {department && (
          <Reveal className="mt-8">
            <section aria-labelledby="doctor-department" className="rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8">
              <h2 id="doctor-department" className="text-xl font-semibold">
                {department.label} at {HOSPITAL.name}
              </h2>
              <p className="mt-2 text-ink-muted">{department.summary}</p>
              <ul className="mt-4 grid gap-2 text-sm sm:grid-cols-3">
                {department.treats.map((item) => (
                  <li key={item} className="flex gap-2">
                    <Check size={16} aria-hidden="true" className="mt-0.5 shrink-0 text-success-ink" />
                    {item}
                  </li>
                ))}
              </ul>
              <Link to="/treatments" className="mt-5 inline-block text-sm font-medium text-primary-ink hover:underline">
                All departments
              </Link>
            </section>
          </Reveal>
        )}

        {others.length > 0 && (
          <section aria-labelledby="doctor-others" className="mt-12">
            <h2 id="doctor-others" className="text-xl font-semibold">
              Other doctors in {specialityLabel(doctor.specialist)}
            </h2>
            <ul className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {others.map((other, i) => (
                <Reveal as="li" key={other.id} delay={i * 0.06}>
                  <DoctorCard doctor={other} onBook={book} />
                </Reveal>
              ))}
            </ul>
          </section>
        )}
      </div>

      {loginPrompt}
    </PublicPage>
  );
}
