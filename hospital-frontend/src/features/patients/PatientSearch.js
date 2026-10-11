import React, { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import { calculateAgeFromDOB } from "../../shared/utils/calculateAgeFromDOB";
import useAreaBase from "../../shared/useAreaBase";
import * as patientsApi from "./api";

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
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-2">Patients</h1>
      <p className="text-gray-600 dark:text-gray-300 mb-4">
        Search by part of a name, a phone number (10 digits or more) or a patient ID such as #41.
      </p>

      <form onSubmit={search} role="search" className="flex flex-wrap gap-3 mb-4 max-w-2xl">
        <label htmlFor="patient-search" className="sr-only">
          Name, phone or patient ID
        </label>
        <input
          id="patient-search"
          type="search"
          value={box}
          onChange={(e) => setBox(e.target.value)}
          placeholder="Name, phone or patient ID"
          className="flex-1 min-w-0 p-2 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2]"
        />
        <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded">
          Search
        </button>
        {query && (
          <button type="button" onClick={() => setParams({})} className="border dark:border-gray-600 px-4 py-2 rounded">
            Show everyone
          </button>
        )}
      </form>

      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full border-collapse bg-white dark:bg-[#1e293b] shadow-md rounded-lg text-sm">
          <thead>
            <tr className="bg-gray-100 dark:bg-gray-800 border-b dark:border-gray-600">
              <th className="p-3 text-left">ID</th>
              <th className="p-3 text-left">Name</th>
              <th className="p-3 text-left">Gender</th>
              <th className="p-3 text-right">Age</th>
              <th className="p-3 text-left">Phone</th>
              <th className="p-3 text-left">Email</th>
              <th className="p-3 text-left">Account</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((patient) => (
              <tr key={patient.patientId} className="border-b dark:border-gray-700">
                <td className="p-3 text-gray-500 dark:text-gray-400">#{patient.patientId}</td>
                <td className="p-3">
                  <Link
                    to={`${base}/patients/${patient.patientId}`}
                    state={{ from }}
                    className="font-semibold text-blue-700 dark:text-[#50d4f2] hover:underline"
                  >
                    {patient.patientName || "(no name)"}
                  </Link>
                </td>
                <td className="p-3">{GENDER_LABELS[patient.gender] ?? "—"}</td>
                <td className="p-3 text-right">{patient.dob ? calculateAgeFromDOB(patient.dob) : "—"}</td>
                <td className="p-3 whitespace-nowrap">{patient.contactNo || "—"}</td>
                <td className="p-3">{patient.email || "—"}</td>
                <td className="p-3">{patient.hasLogin ? "App account" : "Front-desk record"}</td>
              </tr>
            ))}
            {!loading && !error && entries.length === 0 && (
              <tr>
                <td colSpan={7} className="p-6 text-center text-gray-500">
                  {query ? `No patient found for "${query}".` : "No patients yet."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-3 mt-4">
        <button
          type="button"
          onClick={() => goToPage(page - 1)}
          disabled={loading || page === 0}
          className="border dark:border-gray-600 px-3 py-1 rounded disabled:opacity-50"
        >
          Previous
        </button>
        <span className="text-sm text-gray-700 dark:text-gray-300">
          {loading
            ? "Loading..."
            : `Page ${totalPages === 0 ? 0 : page + 1} of ${totalPages} · ${result?.totalEntries ?? 0} patients`}
        </span>
        <button
          type="button"
          onClick={() => goToPage(page + 1)}
          disabled={loading || page + 1 >= totalPages}
          className="border dark:border-gray-600 px-3 py-1 rounded disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  );
}
