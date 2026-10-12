import React from "react";
import { Link } from "react-router-dom";
import { Phone } from "lucide-react";
import PublicHeader from "./PublicHeader";
import { HOSPITAL, phoneHref } from "../hospitalInfo";
import { Reveal } from "../ui";

// The building blocks of the public site's pages (docs/UI_REFRESH_PLAN.md, U.6 / U.8 / U.9)

// A public page: the header, then the page (the footer comes from the app)
export function PublicPage({ children }) {
  return (
    <div className="min-h-screen bg-canvas text-ink">
      <PublicHeader />
      <main>{children}</main>
    </div>
  );
}

// The top of a page: a small label, the title, a line about it and actions
export function PageIntro({ eyebrow, title, children, actions }) {
  return (
    <section className="relative overflow-hidden border-b border-line">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-gradient-to-b from-primary-ink/[0.07] to-transparent" />
      <div className="relative mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
        <Reveal className="max-w-3xl">
          {eyebrow && <p className="text-sm font-semibold uppercase tracking-wide text-primary-ink">{eyebrow}</p>}
          <h1 className="mt-2 text-4xl font-semibold tracking-tight sm:text-5xl">{title}</h1>
          {children && <p className="mt-4 text-lg leading-relaxed text-ink-muted">{children}</p>}
          {actions && <div className="mt-8 flex flex-wrap gap-3">{actions}</div>}
        </Reveal>
      </div>
    </section>
  );
}

// A section's heading: small label, title and a line under it
export function SectionHeading({ eyebrow, title, id, children, center = false }) {
  return (
    <Reveal className={`max-w-2xl ${center ? "mx-auto text-center" : ""}`}>
      {eyebrow && <p className="text-sm font-semibold uppercase tracking-wide text-primary-ink">{eyebrow}</p>}
      <h2 id={id} className="mt-2 text-3xl font-semibold tracking-tight text-ink sm:text-4xl">
        {title}
      </h2>
      {children && <p className="mt-3 text-lg text-ink-muted">{children}</p>}
    </Reveal>
  );
}

// The closing band: book online or call
export function CallToAction({ title = "Ready to see a doctor?", text = "Book online in a minute, or call us any time of day." }) {
  return (
    <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6 lg:px-8 lg:pb-28">
      <Reveal className="relative overflow-hidden rounded-3xl bg-primary px-6 py-14 text-center text-white shadow-overlay sm:px-12">
        <div aria-hidden="true" className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10" />
        <div aria-hidden="true" className="absolute -bottom-20 -left-10 h-56 w-56 rounded-full bg-white/10" />
        <h2 className="relative text-3xl font-semibold tracking-tight sm:text-4xl">{title}</h2>
        <p className="relative mx-auto mt-3 max-w-xl text-lg text-white">{text}</p>
        <div className="relative mt-8 flex flex-wrap justify-center gap-3">
          <Link
            to="/appointments"
            className="inline-flex h-11 items-center gap-2 rounded-lg bg-white px-5 font-medium text-primary shadow-sm transition-colors hover:bg-white/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70"
          >
            Book an appointment
          </Link>
          <a
            href={phoneHref}
            className="inline-flex h-11 items-center gap-2 rounded-lg border border-white/40 px-5 font-medium text-white transition-colors hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70"
          >
            <Phone size={18} aria-hidden="true" />
            Call {HOSPITAL.phone}
          </a>
        </div>
      </Reveal>
    </section>
  );
}
