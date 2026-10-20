import React, { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import { toLocalISODate } from "../../shared/utils/dateUtils";
import useAreaBase from "../../shared/useAreaBase";
import { getFrontDeskAppointmentPage } from "../appointments/api";
import { dayLabel, formatDate, timeRange, visitStatus } from "../appointments/appointmentFormat";
import * as patientsApi from "./api";
import AbhaEditor from "./AbhaEditor";
import { Badge, Button, Card, CardHeader, Notice, PageHeader, Pagination, Table, TableMessage, Td, Th, useToast } from "../../shared/ui";

// One patient at the front desk (docs/PATIENT_SEARCH_PLAN.md): details, upcoming appointments, the family members, every
// appointment newest first (10 per page), and Book -> Walk-in booking with this patient chosen. The patient's and each
// family member's ABHA number are changed here (docs/ABHA_NUMBER_PLAN.md). /patients/:patientId for
// receptionists, /admin/patients/:patientId for admins.

const HISTORY_PAGE_SIZE = 10;
const GENDER_LABELS = { FEMALE: "Female", MALE: "Male", OTHER: "Other" };
const pretty = (value) => (value ? value.charAt(0) + value.slice(1).toLowerCase() : "");

// Only the last 4 digits of the Aadhaar number are shown
const maskAadhaar = (number) => (number ? `XXXX XXXX ${String(number).slice(-4)}` : null);

function StatusBadge({ appointment }) {
  const { label, tone } = visitStatus(appointment);
  return <Badge tone={tone}>{label}</Badge>;
}

export default function FrontDeskPatient() {
  const { patientId } = useParams();
  const base = useAreaBase();
  const location = useLocation();
  const toast = useToast();
  const [patient, setPatient] = useState(null);
  // null while loading, false when they couldn't be loaded
  const [relatives, setRelatives] = useState(null);
  const [error, setError] = useState("");
  const [upcoming, setUpcoming] = useState(null);
  const [history, setHistory] = useState(null);
  const [historyPage, setHistoryPage] = useState(0);

  useEffect(() => {
    setPatient(null);
    setError("");
    setUpcoming(null);
    setRelatives(null);
    setHistoryPage(0);
    patientsApi
      .getFrontDeskPatient(patientId)
      .then((response) => setPatient(response.data))
      .catch((err) => setError(getErrorMessage(err, "Could not load the patient.")));
    getFrontDeskAppointmentPage({ patientId, status: "upcoming", from: toLocalISODate(), size: 20 })
      .then((response) => setUpcoming(response.data.entries))
      .catch(() => setUpcoming([]));
    patientsApi
      .getPatientRelatives(patientId)
      .then((response) => setRelatives(response.data))
      .catch(() => setRelatives(false));
  }, [patientId]);

  useEffect(() => {
    getFrontDeskAppointmentPage({ patientId, order: "desc", page: historyPage, size: HISTORY_PAGE_SIZE })
      .then((response) => setHistory(response.data))
      .catch(() => setHistory(null));
  }, [patientId, historyPage]);

  const savePatientAbha = async (text) => {
    const { data } = await patientsApi.setPatientAbha(patientId, text);
    setPatient((current) => ({ ...current, abhaNumber: data.abhaNumber }));
    toast.success(data.abhaNumber ? "ABHA number saved." : "ABHA number removed.");
  };

  const saveRelativeAbha = (relative) => async (text) => {
    const { data } = await patientsApi.setRelativeAbha(patientId, relative.id, text);
    setRelatives((list) => list.map((r) => (r.id === relative.id ? { ...r, abhaNumber: data.abhaNumber } : r)));
    toast.success(data.abhaNumber ? `${relative.name}'s ABHA number saved.` : `${relative.name}'s ABHA number removed.`);
  };

  // Back to the list the patient was opened from (Patients or Appointments)
  const backTo = location.state?.from || `${base}/patients`;
  const backLabel = `← ${location.state?.fromLabel ?? "Patients"}`;

  const back = (
    <Link to={backTo} className="text-primary-ink hover:underline">
      {backLabel}
    </Link>
  );

  if (error) {
    return (
      <>
        <div className="mb-4 text-sm">{back}</div>
        <Notice tone="danger">{error}</Notice>
      </>
    );
  }
  if (!patient) {
    return <p className="text-ink-muted">Loading...</p>;
  }

  const details = [
    ["Gender", GENDER_LABELS[patient.gender]],
    ["Age", patient.dob ? `${calculateAgeFromDOB(patient.dob)} years (born ${formatDate(patient.dob, true)})` : null],
    ["Phone", patient.contactNo],
    ["Email", patient.email],
    ["Address", patient.patientAddress],
    ["Aadhaar", maskAadhaar(patient.patientAadharNo)],
    ["ABHA", <AbhaEditor id="patient-abha" number={patient.abhaNumber} who={patient.patientName} onSave={savePatientAbha} />],
  ];
  const historyEntries = history?.entries ?? [];
  const historyPages = history?.totalPages ?? 0;

  return (
    <div className="max-w-5xl">
      <PageHeader
        back={back}
        title={patient.patientName}
        description={`#${patient.patientId} · ${patient.hasLogin ? "App account" : "Front-desk record"}`}
        actions={
          <Button as={Link} to={`${base}/walk-in?patientId=${patient.patientId}`} state={{ patient }} variant="success">
            Book an appointment
          </Button>
        }
      />

      <div className="grid gap-5 lg:grid-cols-2">
        <Card aria-labelledby="patient-details">
          <CardHeader id="patient-details" title="Details" />
          <dl className="grid gap-y-2.5 text-sm">
            {details.map(([label, value]) => (
              <div key={label} className="flex items-baseline gap-3">
                <dt className="w-20 shrink-0 text-ink-muted">{label}</dt>
                <dd>{value || "—"}</dd>
              </div>
            ))}
          </dl>
        </Card>

        <Card aria-labelledby="patient-upcoming">
          <CardHeader id="patient-upcoming" title="Upcoming appointments" />
          {upcoming === null && <p className="text-sm text-ink-muted">Loading...</p>}
          {upcoming?.length === 0 && <p className="text-sm text-ink-muted">No upcoming appointments.</p>}
          {upcoming?.length > 0 && (
            <ul className="divide-y divide-line text-sm">
              {upcoming.map((appointment) => (
                <li key={appointment.appointmentID} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5 first:pt-0 last:pb-0">
                  <span className="font-medium">
                    {dayLabel(appointment.date)} {timeRange(appointment)}
                  </span>
                  <span className="text-ink-muted">Dr. {appointment.doctorName}</span>
                  {appointment.patientName !== patient.patientName && (
                    <span className="text-ink-muted">for {appointment.patientName}</span>
                  )}
                  <span className="ml-auto">
                    <StatusBadge appointment={appointment} />
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card aria-labelledby="patient-family" className="mt-5">
        <CardHeader id="patient-family" title="Family members" description="People this patient books for in the app, each with their own ABHA number." />
        {relatives === null && <p className="text-sm text-ink-muted">Loading...</p>}
        {relatives === false && <p className="text-sm text-ink-muted">The family members couldn't be loaded.</p>}
        {relatives?.length === 0 && <p className="text-sm text-ink-muted">No family members.</p>}
        {relatives?.length > 0 && (
          <ul className="divide-y divide-line text-sm">
            {relatives.map((relative) => (
              <li key={relative.id} className="flex flex-wrap items-start justify-between gap-x-6 gap-y-2 py-3 first:pt-0 last:pb-0">
                <div>
                  <p className="font-medium">{relative.name}</p>
                  <p className="text-ink-muted">
                    {[pretty(relative.relationship), relative.dob ? `${calculateAgeFromDOB(relative.dob)} years` : null, GENDER_LABELS[relative.gender]]
                      .filter(Boolean)
                      .join(" · ")}
                  </p>
                </div>
                <div className="flex items-baseline gap-3">
                  <span className="text-ink-muted">ABHA</span>
                  <AbhaEditor id={`relative-abha-${relative.id}`} number={relative.abhaNumber} who={relative.name} onSave={saveRelativeAbha(relative)} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card padding="none" aria-labelledby="patient-history" className="mt-5">
        <div className="px-5 pt-5">
          <CardHeader id="patient-history" title="All appointments, newest first" />
        </div>
        <Table plain>
          <thead>
            <tr>
              <Th>Date</Th>
              <Th>Time</Th>
              <Th>Doctor</Th>
              <Th>For</Th>
              <Th>Status</Th>
            </tr>
          </thead>
          <tbody>
            {historyEntries.map((appointment) => (
              <tr key={appointment.appointmentID}>
                <Td className="whitespace-nowrap">{formatDate(appointment.date, true)}</Td>
                <Td className="whitespace-nowrap tabular-nums">{timeRange(appointment)}</Td>
                <Td className="text-ink-muted">Dr. {appointment.doctorName}</Td>
                <Td>{appointment.patientName}</Td>
                <Td>
                  <StatusBadge appointment={appointment} />
                </Td>
              </tr>
            ))}
            {history && historyEntries.length === 0 && <TableMessage colSpan={5}>No appointments yet.</TableMessage>}
          </tbody>
        </Table>
        {historyPages > 1 && (
          <div className="border-t border-line px-5 pb-4">
            <Pagination
              page={historyPage}
              totalPages={historyPages}
              totalEntries={history.totalEntries}
              noun="appointments"
              onPage={setHistoryPage}
              previousLabel="Newer"
              nextLabel="Older"
            />
          </div>
        )}
      </Card>
    </div>
  );
}
