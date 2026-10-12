import React, { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import useAreaBase from "../../shared/useAreaBase";
import * as patientsApi from "./api";
import { Search } from "lucide-react";
import { Badge, Button, Input, Notice, PageHeader, Pagination, Table, TableMessage, Td, Th } from "../../shared/ui";

// The front desk's Patients page (docs/PATIENT_SEARCH_PLAN.md): one search box - part of a name, a phone number or a
// patient ID - and the patients found, 20 per page; without a search, everyone. The search and the page are kept in
// the address (?q=...&page=...), so coming back from a patient shows the same list. Receptionists: /patients,
// admins: /admin/patients.

const PAGE_SIZE = 20;
const GENDER_LABELS = { FEMALE: "Female", MALE: "Male", OTHER: "Other" };

export default function PatientSearch() {
  const base = useAreaBase();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const query = params.get("q") ?? "";
  const page = Math.max(0, Number(params.get("page")) || 0);
  const [box, setBox] = useState(query);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => setBox(query), [query]);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError("");
    patientsApi
      .searchPatients(query, page, PAGE_SIZE)
      .then((response) => current && setResult(response.data))
      .catch((err) => {
        if (!current) return;
        setResult(null);
        setError(getErrorMessage(err, "Could not search for patients."));
      })
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [query, page]);

  const search = (e) => {
    e.preventDefault();
    setParams(box.trim() ? { q: box.trim() } : {});
  };

  const goToPage = (number) => {
    const next = query ? { q: query } : {};
    if (number > 0) next.page = String(number);
    setParams(next);
  };

  const entries = result?.entries ?? [];
  const totalPages = result?.totalPages ?? 0;
  const from = location.pathname + location.search; // the patient page links back here

  return (
    <>
      <PageHeader
        title="Patients"
        description="Search by part of a name, a phone number (10 digits or more) or a patient ID such as #41."
      />

      <form onSubmit={search} role="search" className="mb-4 flex max-w-2xl flex-wrap gap-2">
        <label htmlFor="patient-search" className="sr-only">
          Name, phone or patient ID
        </label>
        <div className="relative min-w-0 flex-1">
          <Search size={16} aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted" />
          <Input
            id="patient-search"
            type="search"
            value={box}
            onChange={(e) => setBox(e.target.value)}
            placeholder="Name, phone or patient ID"
            className="pl-9"
          />
        </div>
        <Button type="submit">Search</Button>
        {query && (
          <Button variant="secondary" onClick={() => setParams({})}>
            Show everyone
          </Button>
        )}
      </form>

      <Notice tone="danger" className="mb-4">{error}</Notice>

      <Table>
        <thead>
          <tr>
            <Th>ID</Th>
            <Th>Name</Th>
            <Th>Gender</Th>
            <Th align="right">Age</Th>
            <Th>Phone</Th>
            <Th>Email</Th>
            <Th>Account</Th>
          </tr>
        </thead>
        <tbody>
          {entries.map((patient) => (
            <tr key={patient.patientId}>
              <Td className="text-ink-muted tabular-nums">#{patient.patientId}</Td>
              <Td>
                <Link to={`${base}/patients/${patient.patientId}`} state={{ from }} className="font-medium text-primary-ink hover:underline">
                  {patient.patientName || "(no name)"}
                </Link>
              </Td>
              <Td>{GENDER_LABELS[patient.gender] ?? "—"}</Td>
              <Td align="right" className="tabular-nums">{patient.dob ? calculateAgeFromDOB(patient.dob) : "—"}</Td>
              <Td className="whitespace-nowrap tabular-nums">{patient.contactNo || "—"}</Td>
              <Td className="text-ink-muted">{patient.email || "—"}</Td>
              <Td>
                <Badge tone={patient.hasLogin ? "primary" : "neutral"}>{patient.hasLogin ? "App account" : "Front-desk record"}</Badge>
              </Td>
            </tr>
          ))}
          {!loading && !error && entries.length === 0 && (
            <TableMessage colSpan={7}>{query ? `No patient found for "${query}".` : "No patients yet."}</TableMessage>
          )}
        </tbody>
      </Table>

      <Pagination
        page={page}
        totalPages={totalPages}
        totalEntries={result?.totalEntries}
        noun="patients"
        loading={loading}
        onPage={goToPage}
      />
    </>
  );
}
