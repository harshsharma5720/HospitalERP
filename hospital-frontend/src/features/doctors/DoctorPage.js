import React, { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Search, Stethoscope, WifiOff } from "lucide-react";
import { PageIntro, PublicPage } from "../../shared/layout/PublicSections";
import { HOSPITAL } from "../../shared/hospitalInfo";
import { Button, Card, EmptyState, Input, Reveal } from "../../shared/ui";
import DoctorCard from "./DoctorCard";
import useBooking from "./useBooking";
import useDoctorDirectory from "./useDoctorDirectory";
import { BOOKABLE_DEPARTMENTS } from "./departments";

// The public doctor directory (docs/UI_REFRESH_PLAN.md, U.8): every active doctor, a speciality filter kept in the
// address (?speciality=CARDIOLOGY, so the treatments page can link to it) and a name search. Book asks a visitor
// to log in first.
const CHIP =
  "whitespace-nowrap rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40";

export default function DoctorPage() {
  const { doctors, loading, error, retry } = useDoctorDirectory();
  const [params, setParams] = useSearchParams();
  const speciality = params.get("speciality") ?? "";
  const [name, setName] = useState("");
  const { book, loginPrompt } = useBooking();

  const all = doctors ?? [];
  const countFor = (key) => all.filter((doctor) => doctor.specialist === key).length;
  const needle = name.trim().toLowerCase();
  const shown = all.filter(
    (doctor) => (!speciality || doctor.specialist === speciality) && (!needle || (doctor.name || "").toLowerCase().includes(needle))
  );
  const choose = (key) => setParams(key ? { speciality: key } : {}, { replace: true });

  const filters = [{ key: "", label: "All", count: all.length }, ...BOOKABLE_DEPARTMENTS.map((d) => ({ key: d.key, label: d.label, count: countFor(d.key) }))];

  return (
    <PublicPage>
      <PageIntro eyebrow="Our doctors" title="Find the right doctor">
        Choose a speciality or search by name, see a doctor's profile and book online.
      </PageIntro>

      <section aria-label="Doctors" className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
        <Card padding="sm" className="mb-8">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
            <div className="relative lg:w-72">
              <Search size={16} aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted" />
              <Input
                type="search"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Search by name"
                aria-label="Search doctors by name"
                className="pl-9"
              />
            </div>
            <div role="group" aria-label="Speciality" className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 lg:flex-wrap lg:overflow-visible lg:pb-0">
              {filters.map(({ key, label, count }) => (
                <button
                  key={label}
                  type="button"
                  aria-pressed={speciality === key}
                  onClick={() => choose(key)}
                  className={`${CHIP} ${
                    speciality === key ? "border-primary bg-primary text-white" : "border-line bg-surface text-ink-muted hover:text-ink"
                  }`}
                >
                  {label}
                  {doctors && <span className={speciality === key ? "text-white" : "text-ink-muted"}> · {count}</span>}
                </button>
              ))}
            </div>
          </div>
        </Card>

        {loading && (
          <div aria-busy="true" className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            <span className="sr-only">Loading doctors...</span>
            {[0, 1, 2, 3].map((i) => (
              <div key={i} aria-hidden="true" className="h-60 animate-pulse rounded-2xl border border-line bg-surface" />
            ))}
          </div>
        )}

        {error && (
          <Card padding="none">
            <EmptyState
              icon={WifiOff}
              title="We couldn't load the doctors"
              action={
                <Button variant="secondary" onClick={retry}>
                  Try again
                </Button>
              }
            >
              Please try again in a moment, or call us on {HOSPITAL.phone}.
            </EmptyState>
          </Card>
        )}

        {doctors && shown.length === 0 && (
          <Card padding="none">
            <EmptyState
              icon={Stethoscope}
              title="No doctors match"
              action={
                <Button
                  variant="secondary"
                  onClick={() => {
                    setName("");
                    choose("");
                  }}
                >
                  Show all doctors
                </Button>
              }
            >
              Try another speciality or name, or call us and we'll help you find the right doctor.
            </EmptyState>
          </Card>
        )}

        {shown.length > 0 && (
          <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {shown.map((doctor, i) => (
              <Reveal as="li" key={doctor.id} delay={(i % 4) * 0.06}>
                <DoctorCard doctor={doctor} onBook={book} />
              </Reveal>
            ))}
          </ul>
        )}
      </section>

      {loginPrompt}
    </PublicPage>
  );
}
