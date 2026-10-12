import React from "react";
import { Link } from "react-router-dom";
import { CalendarCheck, CalendarClock, Clock, FileText, ListOrdered, MapPin, Phone, Receipt } from "lucide-react";
import { PageIntro, PublicPage, SectionHeading } from "../../shared/layout/PublicSections";
import { HOSPITAL, directionsHref, phoneHref } from "../../shared/hospitalInfo";
import { Button, Reveal } from "../../shared/ui";

// How to reach the hospital (docs/UI_REFRESH_PLAN.md, U.9): call, visit, book online - the ways that work today -
// and what to know before a visit. (The old enquiry form sent nothing, so it is gone until there is an inbox.)

const BEFORE_YOUR_VISIT = [
  { icon: FileText, title: "Bring earlier reports", text: "Previous prescriptions and test reports help the doctor." },
  { icon: ListOrdered, title: "Check in at the front desk", text: "You get a token, and the waiting-room screen shows when it's your turn." },
  { icon: CalendarClock, title: "Change of plans?", text: "Reschedule or cancel in My appointments, or call us." },
  { icon: Receipt, title: "Paying for the visit", text: "Pay at the front desk by cash, card or UPI and take your receipt." },
];

function ContactCard({ icon: Icon, title, children, action }) {
  return (
    <section aria-label={title} className="flex h-full flex-col rounded-2xl border border-line bg-surface p-6 shadow-card">
      <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-ink/10 text-primary-ink">
        <Icon size={22} aria-hidden="true" />
      </span>
      <h2 className="mt-5 text-lg font-semibold">{title}</h2>
      <div className="mt-2 flex-1 text-ink-muted">{children}</div>
      <div className="mt-6">{action}</div>
    </section>
  );
}

function ContactUsPage() {
  return (
    <PublicPage>
      <PageIntro eyebrow="Contact us" title="We're here, day and night">
        Call us any time, visit us in {HOSPITAL.area}, or book an appointment online.
      </PageIntro>

      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-20">
        <div className="grid gap-5 md:grid-cols-3">
          <Reveal>
            <ContactCard
              icon={Phone}
              title="Call us"
              action={
                <Button as="a" href={phoneHref} className="w-full">
                  <Phone size={16} aria-hidden="true" />
                  Call {HOSPITAL.phone}
                </Button>
              }
            >
              <p className="text-2xl font-semibold tracking-tight text-ink">{HOSPITAL.phone}</p>
              <p className="mt-2 flex items-center gap-2 text-sm">
                <Clock size={16} aria-hidden="true" className="text-success-ink" />
                {HOSPITAL.hours}
              </p>
            </ContactCard>
          </Reveal>
          <Reveal delay={0.08}>
            <ContactCard
              icon={CalendarCheck}
              title="Book online"
              action={
                <div className="flex flex-wrap gap-2">
                  <Button as={Link} to="/appointments" className="flex-1">
                    Book an appointment
                  </Button>
                  <Button as={Link} to="/doctors" variant="secondary" className="flex-1">
                    Find a doctor
                  </Button>
                </div>
              }
            >
              Choose a doctor and a free time; an SMS confirms the booking.
            </ContactCard>
          </Reveal>
          <Reveal delay={0.16}>
            <ContactCard
              icon={MapPin}
              title="Visit us"
              action={
                <Button as="a" href={directionsHref} target="_blank" rel="noreferrer" variant="secondary" className="w-full">
                  Get directions<span className="sr-only"> (opens in a new tab)</span>
                </Button>
              }
            >
              <p>{HOSPITAL.name}</p>
              <p>{HOSPITAL.address}</p>
            </ContactCard>
          </Reveal>
        </div>

        <div className="mt-16 grid items-center gap-10 lg:mt-24 lg:grid-cols-2">
          <div>
            <SectionHeading eyebrow="Your visit" title="Good to know before you come" />
            <ul className="mt-8 grid gap-6 sm:grid-cols-2">
              {BEFORE_YOUR_VISIT.map(({ icon: Icon, title, text }, i) => (
                <Reveal as="li" key={title} delay={i * 0.06}>
                  <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-white">
                    <Icon size={18} aria-hidden="true" />
                  </span>
                  <h3 className="mt-3 font-semibold">{title}</h3>
                  <p className="mt-1 text-sm leading-relaxed text-ink-muted">{text}</p>
                </Reveal>
              ))}
            </ul>
          </div>
          <Reveal delay={0.1}>
            <figure>
              <img src="/shreya.jpg" alt={`The entrance of ${HOSPITAL.name}`} className="aspect-[4/3] w-full rounded-3xl object-cover shadow-overlay" />
              <figcaption className="mt-3 text-sm text-ink-muted">The entrance of {HOSPITAL.name}, {HOSPITAL.area}</figcaption>
            </figure>
          </Reveal>
        </div>
      </div>
    </PublicPage>
  );
}

export default ContactUsPage;
