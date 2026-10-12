import React from "react";
import { Link } from "react-router-dom";
import { Clock, HeartPulse, MapPin, Phone } from "lucide-react";
import { HOSPITAL, directionsHref, phoneHref } from "./hospitalInfo";

const LINKS = [
  ["/treatments", "Treatments"],
  ["/doctors", "Doctors"],
  ["/about", "About Us"],
  ["/contact", "Contact Us"],
];

// The public site's footer (docs/UI_REFRESH_PLAN.md, U.6): the hospital, its pages and how to reach it
export default function Footer() {
  return (
    <footer className="border-t border-line bg-surface">
      <div className="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-4 lg:px-8">
        <div className="md:col-span-2">
          <Link to="/" className="inline-flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-white">
              <HeartPulse size={20} aria-hidden="true" />
            </span>
            <span className="font-semibold text-ink">{HOSPITAL.name}</span>
          </Link>
          <p className="mt-4 max-w-md text-sm leading-relaxed text-ink-muted">
            Specialist care in {HOSPITAL.area}: book online, get an SMS confirmation, and follow your turn on the
            waiting-room screen when you arrive.
          </p>
        </div>

        <nav aria-label="Footer">
          <h2 className="text-sm font-semibold text-ink">Hospital</h2>
          <ul className="mt-4 space-y-2.5 text-sm">
            {LINKS.map(([to, label]) => (
              <li key={to}>
                <Link to={to} className="text-ink-muted hover:text-ink">
                  {label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div>
          <h2 className="text-sm font-semibold text-ink">Visit us</h2>
          <ul className="mt-4 space-y-3 text-sm text-ink-muted">
            <li className="flex gap-2.5">
              <MapPin size={16} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-ink" />
              <span>
                {HOSPITAL.address}
                <a href={directionsHref} target="_blank" rel="noreferrer" className="mt-1 block font-medium text-primary-ink hover:underline">
                  Get directions<span className="sr-only"> (opens in a new tab)</span>
                </a>
              </span>
            </li>
            <li className="flex gap-2.5">
              <Phone size={16} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-ink" />
              <a href={phoneHref} className="hover:text-ink">
                {HOSPITAL.phone}
              </a>
            </li>
            <li className="flex gap-2.5">
              <Clock size={16} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-ink" />
              {HOSPITAL.hours}
            </li>
          </ul>
        </div>
      </div>
      <div className="border-t border-line">
        <div className="mx-auto flex max-w-7xl flex-wrap justify-between gap-2 px-4 py-5 text-xs text-ink-muted sm:px-6 lg:px-8">
          <span>
            © {new Date().getFullYear()} {HOSPITAL.name}
          </span>
          <span>Runs on HospitalERP</span>
        </div>
      </div>
    </footer>
  );
}
