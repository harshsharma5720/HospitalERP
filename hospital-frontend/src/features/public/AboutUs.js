import React from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Clock, FileText, HeartPulse, Hospital, ListOrdered, MessageSquare, Receipt } from "lucide-react";
import { CallToAction, PageIntro, PublicPage, SectionHeading } from "../../shared/layout/PublicSections";
import { HOSPITAL } from "../../shared/hospitalInfo";
import { Button, Reveal } from "../../shared/ui";
import DoctorCard from "../doctors/DoctorCard";
import useDoctorDirectory from "../doctors/useDoctorDirectory";

// The hospital's story, mission and vision, what patients can count on, and its doctors from the public directory
// (docs/UI_REFRESH_PLAN.md, U.9)

const PROMISES = [
  { icon: Clock, title: "Care around the clock", text: `${HOSPITAL.name} is open 24×7, every day of the year.` },
  { icon: MessageSquare, title: "Bookings you can trust", text: "Book online and get an SMS confirmation and a reminder." },
  { icon: ListOrdered, title: "No guessing in the waiting room", text: "Your token and the screen tell you when it's your turn." },
  { icon: FileText, title: "Your records, at hand", text: "Prescriptions are ready to download after each visit." },
  { icon: Receipt, title: "Clear bills", text: "Every charge itemised, with a receipt for each payment." },
];

const DOCTORS_SHOWN = 4;

export default function AboutUs() {
  const { doctors } = useDoctorDirectory();

  return (
    <PublicPage>
      <PageIntro eyebrow="About us" title="Caring for you, every step of the way">
        With compassion and expertise.
      </PageIntro>

      {/* ================= STORY ================= */}
      <section aria-labelledby="about-story" className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
        <div className="grid items-center gap-12 lg:grid-cols-2">
          <Reveal>
            <img src="/shreya.jpg" alt={`The entrance of ${HOSPITAL.name}`} className="aspect-[4/3] w-full rounded-3xl object-cover shadow-overlay" />
          </Reveal>
          <Reveal delay={0.1}>
            <p className="text-sm font-semibold uppercase tracking-wide text-primary-ink">Our story</p>
            <h2 id="about-story" className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
              Welcome to {HOSPITAL.name}
            </h2>
            <p className="mt-5 text-lg leading-relaxed text-ink-muted">
              At {HOSPITAL.name}, we believe healthcare should be personal, accessible, and compassionate. For more than a
              decade, we have provided expert medical care using advanced technology and highly experienced doctors.
            </p>
            <p className="mt-4 text-lg leading-relaxed text-ink-muted">
              We deliver high-quality and affordable healthcare with respect, transparency, and empathy.
            </p>
          </Reveal>
        </div>
      </section>

      {/* ================= MISSION + VISION ================= */}
      <section aria-label="Mission and vision" className="border-y border-line bg-surface">
        <div className="mx-auto grid max-w-7xl gap-5 px-4 py-16 sm:px-6 md:grid-cols-2 lg:px-8 lg:py-20">
          {[
            { icon: HeartPulse, title: "Our mission", text: "To provide exceptional healthcare services with compassion, innovation, and integrity." },
            { icon: Hospital, title: "Our vision", text: "To be a global leader in healthcare, recognized for trust, care, and medical excellence." },
          ].map(({ icon: Icon, title, text }, i) => (
            <Reveal key={title} delay={i * 0.08} className="rounded-2xl border border-line bg-canvas p-8">
              <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-ink/10 text-primary-ink">
                <Icon size={22} aria-hidden="true" />
              </span>
              <h2 className="mt-5 text-xl font-semibold">{title}</h2>
              <p className="mt-2 text-lg leading-relaxed text-ink-muted">{text}</p>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ================= WHAT PATIENTS CAN COUNT ON ================= */}
      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
        <SectionHeading eyebrow="What you can count on" title="Care that's easy to arrange" />
        <ul className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          {PROMISES.map(({ icon: Icon, title, text }, i) => (
            <Reveal as="li" key={title} delay={i * 0.06} className="rounded-2xl border border-line bg-surface p-6 shadow-card">
              <Icon size={22} aria-hidden="true" className="text-primary-ink" />
              <h3 className="mt-4 font-semibold">{title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-ink-muted">{text}</p>
            </Reveal>
          ))}
        </ul>
      </section>

      {/* ================= OUR DOCTORS ================= */}
      {doctors?.length > 0 && (
        <section aria-labelledby="about-team" className="mx-auto max-w-7xl px-4 pb-16 sm:px-6 lg:px-8 lg:pb-24">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <SectionHeading eyebrow="Our team" title="Meet our doctors" id="about-team" />
            <Button as={Link} to="/doctors" variant="secondary">
              All doctors
              <ArrowRight size={16} aria-hidden="true" />
            </Button>
          </div>
          <ul className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {doctors.slice(0, DOCTORS_SHOWN).map((doctor, i) => (
              <Reveal as="li" key={doctor.id} delay={i * 0.06}>
                <DoctorCard doctor={doctor} />
              </Reveal>
            ))}
          </ul>
        </section>
      )}

      <CallToAction title="Need medical assistance?" text="Our team is available 24/7 to support your healthcare needs." />
    </PublicPage>
  );
}
