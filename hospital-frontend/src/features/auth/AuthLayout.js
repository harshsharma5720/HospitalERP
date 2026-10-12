import React from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, CalendarCheck, FileText, HeartPulse, ListOrdered, Phone } from "lucide-react";
import { HOSPITAL, phoneHref } from "../../shared/hospitalInfo";
import { Reveal, ThemeButton } from "../../shared/ui";

// Login, register and forgot password (docs/PATIENT_AREA_REFRESH_PLAN.md, P.1): the hospital's panel on the left
// (wide screens only) and the form on the right
const POINTS = [
  { icon: CalendarCheck, text: "Book online and get an SMS confirmation" },
  { icon: ListOrdered, text: "Follow your turn on the waiting-room screen" },
  { icon: FileText, text: "Download your prescriptions after each visit" },
];

function Brand({ light = false }) {
  return (
    <Link to="/" className="inline-flex items-center gap-2.5 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40">
      <span className={`flex h-9 w-9 items-center justify-center rounded-lg ${light ? "bg-white text-primary" : "bg-primary text-white"}`}>
        <HeartPulse size={20} aria-hidden="true" />
      </span>
      <span className={`font-semibold ${light ? "text-white" : "text-ink"}`}>{HOSPITAL.name}</span>
    </Link>
  );
}

export default function AuthLayout({ title, description, children, footer }) {
  return (
    <div className="min-h-screen bg-canvas text-ink lg:grid lg:grid-cols-2">
      <aside className="relative hidden overflow-hidden bg-primary p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div aria-hidden="true" className="absolute -right-24 -top-24 h-80 w-80 rounded-full bg-white/10" />
        <div aria-hidden="true" className="absolute -bottom-32 -left-16 h-96 w-96 rounded-full bg-white/10" />
        <div className="relative">
          <Brand light />
        </div>
        <div className="relative max-w-md">
          <p className="text-4xl font-semibold leading-tight tracking-tight">Specialist care, booked in a minute.</p>
          <ul className="mt-8 space-y-4">
            {POINTS.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-center gap-3">
                <span className="rounded-lg bg-white/15 p-2">
                  <Icon size={18} aria-hidden="true" />
                </span>
                {text}
              </li>
            ))}
          </ul>
        </div>
        <a href={phoneHref} className="relative inline-flex items-center gap-2 text-sm font-medium text-white hover:underline">
          <Phone size={16} aria-hidden="true" />
          {HOSPITAL.phone} · {HOSPITAL.hours}
        </a>
      </aside>

      <main className="flex min-h-screen flex-col px-4 py-6 sm:px-8 lg:px-16">
        <div className="flex items-center justify-between">
          <div className="lg:hidden">
            <Brand />
          </div>
          <Link to="/" className="hidden items-center gap-1.5 text-sm font-medium text-ink-muted hover:text-ink lg:inline-flex">
            <ArrowLeft size={16} aria-hidden="true" />
            Back to the site
          </Link>
          <ThemeButton />
        </div>
        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10">
          <Reveal>
            <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
            {description && <p className="mt-2 text-ink-muted">{description}</p>}
            <div className="mt-8">{children}</div>
            {footer && <div className="mt-8 text-center text-sm text-ink-muted">{footer}</div>}
          </Reveal>
        </div>
      </main>
    </div>
  );
}
