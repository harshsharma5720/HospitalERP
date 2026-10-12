import React from "react";
import { Link } from "react-router-dom";
import { ArrowRight, CalendarCheck, CheckCircle2, Clock, FileText, ListOrdered, MapPin, Phone, UserPlus } from "lucide-react";
import PopupForm from "../../shared/PopupForm";
import { CallToAction, PublicPage, SectionHeading } from "../../shared/layout/PublicSections";
import { HOSPITAL, directionsHref, phoneHref } from "../../shared/hospitalInfo";
import { Button, Reveal } from "../../shared/ui";
import DoctorCard from "../doctors/DoctorCard";
import useDoctorDirectory from "../doctors/useDoctorDirectory";
import { BOOKABLE_DEPARTMENTS } from "../doctors/departments";

// The public landing page (docs/UI_REFRESH_PLAN.md, U.6): what the hospital offers, real doctors and specialities
// from the public directory, how a visit works, and how to reach the hospital. Sections fade in as they scroll
// into view (not for people who ask for reduced motion).

const STEPS = [
  { icon: UserPlus, title: "Register", text: "Create an account online, or simply walk in: the front desk registers you." },
  { icon: CalendarCheck, title: "Book a time", text: "Choose a doctor and a free slot; an SMS confirms the booking." },
  { icon: ListOrdered, title: "Check in", text: "At the front desk you get a token, and the waiting-room screen shows your turn." },
  { icon: FileText, title: "See the doctor", text: "After the visit your prescription is ready to download." },
];

const DOCTORS_SHOWN = 4;

const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

export default function HomePage() {
  const { doctors } = useDoctorDirectory(); // null while loading or when the directory can't be reached

  const perSpeciality = (key) => (doctors ?? []).filter((d) => d.specialist === key).length;
  const specialityCount = new Set((doctors ?? []).map((d) => d.specialist).filter((s) => s && s !== "NOT_ASSIGNED")).size;
  const facts = [
    doctors?.length ? [String(doctors.length), doctors.length === 1 ? "doctor" : "doctors"] : null,
    specialityCount ? [String(specialityCount), specialityCount === 1 ? "speciality" : "specialities"] : null,
    ["24×7", "care, every day"],
    ["SMS", "booking confirmations"],
  ].filter(Boolean);

  return (
    <PublicPage>
      {/* ================= HERO ================= */}
      <section className="relative overflow-hidden">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-gradient-to-b from-primary-ink/[0.07] to-transparent" />
        <div className="relative mx-auto grid max-w-7xl items-center gap-12 px-4 pb-20 pt-12 sm:px-6 lg:grid-cols-2 lg:gap-16 lg:px-8 lg:pb-28 lg:pt-20">
          <Reveal>
            <p className="inline-flex items-center gap-2 rounded-full border border-line bg-surface px-3 py-1 text-sm font-medium text-ink-muted shadow-card">
              <Clock size={14} aria-hidden="true" className="text-success-ink" />
              {HOSPITAL.hours} · {HOSPITAL.area}
            </p>
            <h1 className="mt-6 text-4xl font-semibold leading-tight tracking-tight sm:text-5xl lg:text-6xl">
              Specialist care, <span className="text-primary-ink">booked in a minute.</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-ink-muted">
              See the right specialist at {HOSPITAL.name}. Book online, get an SMS confirmation, and follow your turn
              on the waiting-room screen when you arrive.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button as={Link} to="/appointments" size="lg">
                Book an appointment
                <ArrowRight size={18} aria-hidden="true" />
              </Button>
              <Button as={Link} to="/doctors" size="lg" variant="secondary">
                Find a doctor
              </Button>
            </div>
            <p className="mt-6 text-sm text-ink-muted">
              Prefer to call?{" "}
              <a href={phoneHref} className="font-medium text-ink hover:text-primary-ink">
                {HOSPITAL.phone}
              </a>
            </p>
          </Reveal>

          <Reveal delay={0.15} className="relative mx-auto w-full max-w-xl lg:max-w-none">
            <div aria-hidden="true" className="absolute -inset-6 rounded-[2.5rem] bg-primary-ink/10 blur-3xl" />
            <img
              src="/Treatments-pediatrics.jpg"
              alt="A doctor listening to a baby's heartbeat with a stethoscope"
              className="relative aspect-[4/3] w-full rounded-3xl object-cover shadow-overlay"
            />
            <Reveal
              delay={0.45}
              className="absolute -bottom-6 left-4 flex items-center gap-3 rounded-xl border border-line bg-surface p-4 shadow-overlay sm:-left-6"
            >
              <span className="rounded-full bg-success-ink/10 p-2 text-success-ink">
                <CheckCircle2 size={20} aria-hidden="true" />
              </span>
              <span className="text-sm">
                <span className="block font-semibold">Appointment confirmed</span>
                <span className="text-ink-muted">SMS sent to your phone</span>
              </span>
            </Reveal>
            <Reveal
              delay={0.6}
              className="absolute -top-5 right-4 hidden items-center gap-3 rounded-xl border border-line bg-surface p-3 shadow-overlay sm:flex sm:-right-6"
            >
              <span className="rounded-lg bg-primary p-2 text-white">
                <ListOrdered size={18} aria-hidden="true" />
              </span>
              <span className="text-sm">
                <span className="block font-semibold">Live queue</span>
                <span className="text-ink-muted">Your token on the screen</span>
              </span>
            </Reveal>
          </Reveal>
        </div>
      </section>

      {/* ================= FACTS (from the doctors' directory) ================= */}
      <section aria-label="At a glance" className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <Reveal className={`grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-line bg-line shadow-card ${facts.length === 4 ? "lg:grid-cols-4" : ""}`}>
          {facts.map(([value, label]) => (
            <div key={label} className="bg-surface px-6 py-6 text-center">
              <p className="text-3xl font-semibold tracking-tight text-ink">{value}</p>
              <p className="mt-1 text-sm text-ink-muted">{label}</p>
            </div>
          ))}
        </Reveal>
      </section>

      {/* ================= SPECIALITIES ================= */}
      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-28">
        <SectionHeading eyebrow="Specialities" title="Care across the departments you need">
          Experienced doctors in each department, with appointments you can book online.
        </SectionHeading>
        <ul className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {BOOKABLE_DEPARTMENTS.map(({ key, label, icon: Icon, summary }, i) => {
            const count = perSpeciality(key);
            return (
              <Reveal as="li" key={key} delay={(i % 3) * 0.08}>
                <Link
                  to={`/doctors?speciality=${key}`}
                  className="group flex h-full flex-col rounded-2xl border border-line bg-surface p-6 shadow-card transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-overlay focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
                >
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-ink/10 text-primary-ink">
                    <Icon size={22} aria-hidden="true" />
                  </span>
                  <span className="mt-5 text-lg font-semibold text-ink">{label}</span>
                  <span className="mt-1.5 flex-1 text-sm text-ink-muted">{summary}</span>
                  <span className="mt-5 flex items-center gap-1.5 text-sm font-medium text-primary-ink">
                    {count > 0 ? plural(count, "doctor") : "See doctors"}
                    <ArrowRight size={16} aria-hidden="true" className="transition-transform group-hover:translate-x-0.5" />
                  </span>
                </Link>
              </Reveal>
            );
          })}
        </ul>
      </section>

      {/* ================= HOW IT WORKS ================= */}
      <section className="border-y border-line bg-surface">
        <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-24">
          <SectionHeading eyebrow="Your visit" title="Simple from booking to prescription" center />
          <ol className="mt-14 grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
            {STEPS.map(({ icon: Icon, title, text }, i) => (
              <Reveal as="li" key={title} delay={i * 0.1}>
                <div className="flex items-center gap-3">
                  <span className="flex h-11 w-11 items-center justify-center rounded-full bg-primary text-white">
                    <Icon size={20} aria-hidden="true" />
                  </span>
                  <span className="text-sm font-semibold text-ink-muted">Step {i + 1}</span>
                </div>
                <h3 className="mt-4 text-lg font-semibold">{title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-ink-muted">{text}</p>
              </Reveal>
            ))}
          </ol>
        </div>
      </section>

      {/* ================= DOCTORS ================= */}
      {doctors?.length > 0 && (
        <section aria-labelledby="home-doctors" className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 lg:py-28">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <Reveal>
              <p className="text-sm font-semibold uppercase tracking-wide text-primary-ink">Our doctors</p>
              <h2 id="home-doctors" className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
                Meet some of our doctors
              </h2>
            </Reveal>
            <Button as={Link} to="/doctors" variant="secondary">
              All doctors
              <ArrowRight size={16} aria-hidden="true" />
            </Button>
          </div>
          <ul className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {doctors.slice(0, DOCTORS_SHOWN).map((doctor, i) => (
              <Reveal as="li" key={doctor.id} delay={i * 0.08}>
                <DoctorCard doctor={doctor} />
              </Reveal>
            ))}
          </ul>
        </section>
      )}

      {/* ================= VISIT US ================= */}
      <section
        aria-labelledby="home-visit"
        className={`mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 ${doctors?.length > 0 ? "pb-20 lg:pb-28" : "py-20 lg:py-28"}`}
      >
        <div className="grid items-center gap-10 overflow-hidden rounded-3xl border border-line bg-surface p-6 shadow-card sm:p-10 lg:grid-cols-2">
          <Reveal>
            <img src="/shreya.jpg" alt={`The entrance of ${HOSPITAL.name}`} className="aspect-[4/3] w-full max-w-lg rounded-2xl object-cover" />
          </Reveal>
          <Reveal delay={0.1}>
            <p className="text-sm font-semibold uppercase tracking-wide text-primary-ink">Visit us</p>
            <h2 id="home-visit" className="mt-2 text-3xl font-semibold tracking-tight">
              {HOSPITAL.name}, {HOSPITAL.area}
            </h2>
            <ul className="mt-6 space-y-4 text-ink-muted">
              <li className="flex gap-3">
                <MapPin size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-ink" />
                {HOSPITAL.address}
              </li>
              <li className="flex gap-3">
                <Phone size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-ink" />
                <a href={phoneHref} className="font-medium text-ink hover:text-primary-ink">
                  {HOSPITAL.phone}
                </a>
              </li>
              <li className="flex gap-3">
                <Clock size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-ink" />
                {HOSPITAL.hours}
              </li>
            </ul>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button as="a" href={directionsHref} target="_blank" rel="noreferrer">
                Get directions<span className="sr-only"> (opens in a new tab)</span>
              </Button>
              <Button as={Link} to="/contact" variant="secondary">
                Contact us
              </Button>
            </div>
          </Reveal>
        </div>
      </section>

      <CallToAction />

      <PopupForm />
    </PublicPage>
  );
}
