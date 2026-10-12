import React from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Check, Phone, PhoneCall } from "lucide-react";
import { CallToAction, PageIntro, PublicPage } from "../../shared/layout/PublicSections";
import { HOSPITAL, phoneHref } from "../../shared/hospitalInfo";
import { Button, Reveal } from "../../shared/ui";
import useDoctorDirectory from "../doctors/useDoctorDirectory";
import { DEPARTMENTS } from "../doctors/departments";

// The hospital's departments (docs/UI_REFRESH_PLAN.md, U.8): what each looks after and its doctors, from the
// public directory. A department without doctors in the app is arranged by phone.

const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

function DepartmentCard({ department, doctorCount }) {
  const { key, label, icon: Icon, summary, treats, bookable = true } = department;
  return (
    <article aria-labelledby={`department-${key}`} className="flex h-full flex-col rounded-2xl border border-line bg-surface p-6 shadow-card">
      <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-ink/10 text-primary-ink">
        <Icon size={22} aria-hidden="true" />
      </span>
      <h2 id={`department-${key}`} className="mt-5 text-lg font-semibold">
        {label}
      </h2>
      <p className="mt-1.5 text-sm text-ink-muted">{summary}</p>
      <ul className="mt-4 flex-1 space-y-2 text-sm">
        {treats.map((item) => (
          <li key={item} className="flex gap-2">
            <Check size={16} aria-hidden="true" className="mt-0.5 shrink-0 text-success-ink" />
            {item}
          </li>
        ))}
      </ul>
      <div className="mt-6 border-t border-line pt-4">
        {bookable ? (
          <Link to={`/doctors?speciality=${key}`} className="group flex items-center gap-1.5 text-sm font-medium text-primary-ink">
            {doctorCount > 0 ? `See ${plural(doctorCount, "doctor")}` : "See doctors"}
            <ArrowRight size={16} aria-hidden="true" className="transition-transform group-hover:translate-x-0.5" />
          </Link>
        ) : (
          <a href={phoneHref} className="flex items-center gap-1.5 text-sm font-medium text-primary-ink">
            <PhoneCall size={16} aria-hidden="true" />
            Call to arrange a visit
          </a>
        )}
      </div>
    </article>
  );
}

export default function Treatments() {
  const { doctors } = useDoctorDirectory();
  const countFor = (key) => (doctors ?? []).filter((doctor) => doctor.specialist === key).length;

  return (
    <PublicPage>
      <PageIntro
        eyebrow="Treatments"
        title="Specialist care under one roof"
        actions={
          <>
            <Button as={Link} to="/doctors" size="lg">
              Find a doctor
            </Button>
            <Button as="a" href={phoneHref} size="lg" variant="secondary">
              <Phone size={18} aria-hidden="true" />
              Call {HOSPITAL.phone}
            </Button>
          </>
        }
      >
        Our departments and what they look after. Book a specialist online, or call us if you're not sure where to start.
      </PageIntro>

      <section aria-label="Departments" className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-20">
        <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {DEPARTMENTS.map((department, i) => (
            <Reveal as="li" key={department.key} delay={(i % 4) * 0.06}>
              <DepartmentCard department={department} doctorCount={countFor(department.key)} />
            </Reveal>
          ))}
          <Reveal as="li" delay={0.18}>
            <div className="flex h-full flex-col justify-between rounded-2xl border border-primary/30 bg-primary-ink/5 p-6 shadow-card">
              <div>
                <h2 className="text-lg font-semibold">Not sure where to start?</h2>
                <p className="mt-2 text-sm text-ink-muted">
                  Call us and we'll help you find the right department and doctor, day or night.
                </p>
              </div>
              <Button as="a" href={phoneHref} className="mt-6">
                <Phone size={16} aria-hidden="true" />
                {HOSPITAL.phone}
              </Button>
            </div>
          </Reveal>
        </ul>
      </section>

      <CallToAction />
    </PublicPage>
  );
}
