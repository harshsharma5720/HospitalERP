import React, { useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import { specialityLabel } from "../appointments/appointmentFormat";
import * as adminApi from "./api";

// Consultation fees (docs/BILLING_PLAN.md): one per doctor; a visit's bill starts with it. Doctors without a fee
// use the default fee (BILLING_DEFAULT_FEE). Deactivated doctors are listed too, for completeness.

const INPUT =
  "w-32 p-2 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] text-right";

export default function Fees() {
  const [fees, setFees] = useState(null);
  const [drafts, setDrafts] = useState({}); // doctorId -> what is typed
  const [savingId, setSavingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    adminApi
      .getFees()
      .then((response) => setFees(response.data))
      .catch((err) => setError(getErrorMessage(err, "Could not load the fees.")));
  }, []);

  const draftOf = (doctor) => drafts[doctor.doctorId] ?? (doctor.consultationFee ?? "").toString();
  const changed = (doctor) => draftOf(doctor).trim() !== (doctor.consultationFee ?? "").toString();

  const save = async (doctor) => {
    setError("");
    setNotice("");
    const typed = draftOf(doctor).trim();
    if (typed === "") {
      setError(`Please enter the fee of Dr. ${doctor.doctorName}.`);
      return;
    }
    setSavingId(doctor.doctorId);
    try {
      const response = await adminApi.setFee(doctor.doctorId, Number(typed));
      setFees((current) => ({
        ...current,
        doctors: current.doctors.map((d) => (d.doctorId === doctor.doctorId ? response.data : d)),
      }));
      setDrafts((current) => {
        const next = { ...current };
        delete next[doctor.doctorId];
        return next;
      });
      setNotice(`Fee of Dr. ${doctor.doctorName} saved: ${formatRupees(response.data.consultationFee)}.`);
    } catch (err) {
      setError(getErrorMessage(err, "Could not save the fee."));
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="p-6 max-w-4xl">
      <h1 className="text-3xl font-bold mb-2">Consultation fees</h1>
      <p className="text-gray-600 dark:text-gray-300 mb-4">
        A visit's bill starts with the doctor's fee; the front desk can add items and a discount.
        {fees && ` Doctors without a fee use the default fee of ${formatRupees(fees.defaultFee)}.`}
      </p>

      {notice && (
        <p role="status" className="text-green-700 dark:text-green-400 mb-4">
          {notice}
        </p>
      )}
      {error && (
        <p role="alert" className="text-red-600 mb-4">
          {error}
        </p>
      )}

      {!fees && !error && <p>Loading...</p>}
      {fees && (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse bg-white dark:bg-[#1e293b] shadow-md rounded-lg text-sm">
            <thead>
              <tr className="bg-gray-100 dark:bg-gray-800 border-b dark:border-gray-600">
                <th className="p-3 text-left">Doctor</th>
                <th className="p-3 text-left">Speciality</th>
                <th className="p-3 text-left">Fee (₹)</th>
                <th className="p-3 text-left" aria-label="Save" />
              </tr>
            </thead>
            <tbody>
              {fees.doctors.map((doctor) => (
                <tr key={doctor.doctorId} className={`border-b dark:border-gray-700 ${doctor.active ? "" : "text-gray-500"}`}>
                  <td className="p-3">
                    Dr. {doctor.doctorName}
                    {!doctor.active && <span className="ml-2 text-xs">(inactive)</span>}
                  </td>
                  <td className="p-3">{specialityLabel(doctor.specialist) || "—"}</td>
                  <td className="p-3">
                    <input
                      type="number"
                      min="0"
                      max="100000"
                      step="0.01"
                      value={draftOf(doctor)}
                      onChange={(e) => setDrafts({ ...drafts, [doctor.doctorId]: e.target.value })}
                      aria-label={`Fee of Dr. ${doctor.doctorName}`}
                      placeholder={String(fees.defaultFee)}
                      className={INPUT}
                    />
                    {doctor.consultationFee === null && (
                      <span className="block text-xs text-gray-500 mt-1">Not set: the default fee applies</span>
                    )}
                  </td>
                  <td className="p-3">
                    <button
                      type="button"
                      onClick={() => save(doctor)}
                      disabled={!changed(doctor) || savingId === doctor.doctorId}
                      aria-label={`Save the fee of Dr. ${doctor.doctorName}`}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm disabled:opacity-40"
                    >
                      Save
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
