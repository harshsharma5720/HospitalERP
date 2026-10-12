import React, { useEffect, useState } from "react";
import { getErrorMessage } from "../../shared/utils/apiError";
import { formatRupees } from "../../shared/utils/money";
import { specialityLabel } from "../appointments/appointmentFormat";
import * as adminApi from "./api";
import { Badge, Button, Input, Notice, PageHeader, Table, Td, Th } from "../../shared/ui";

// Consultation fees (docs/BILLING_PLAN.md): one per doctor; a visit's bill starts with it. Doctors without a fee
// use the default fee (BILLING_DEFAULT_FEE). Deactivated doctors are listed too, for completeness.

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
    <div className="max-w-4xl">
      <PageHeader
        title="Consultation fees"
        description={
          <>
            A visit's bill starts with the doctor's fee; the front desk can add items and a discount.
            {fees && ` Doctors without a fee use the default fee of ${formatRupees(fees.defaultFee)}.`}
          </>
        }
      />

      <Notice tone="success" className="mb-4">{notice}</Notice>
      <Notice tone="danger" className="mb-4">{error}</Notice>

      {!fees && !error && <p className="text-ink-muted">Loading...</p>}
      {fees && (
        <Table>
          <thead>
            <tr>
              <Th>Doctor</Th>
              <Th>Speciality</Th>
              <Th>Fee (₹)</Th>
              <Th aria-label="Save" />
            </tr>
          </thead>
          <tbody>
            {fees.doctors.map((doctor) => (
              <tr key={doctor.doctorId} className={doctor.active ? "" : "text-ink-muted"}>
                <Td>
                  <span className="font-medium">Dr. {doctor.doctorName}</span>
                  {!doctor.active && (
                    <span className="ml-2">
                      <Badge>Deactivated</Badge>
                    </span>
                  )}
                </Td>
                <Td className="text-ink-muted">{specialityLabel(doctor.specialist) || "—"}</Td>
                <Td>
                  <div className="w-32">
                    <Input
                      type="number"
                      min="0"
                      max="100000"
                      step="0.01"
                      value={draftOf(doctor)}
                      onChange={(e) => setDrafts({ ...drafts, [doctor.doctorId]: e.target.value })}
                      aria-label={`Fee of Dr. ${doctor.doctorName}`}
                      placeholder={String(fees.defaultFee)}
                      className="text-right tabular-nums"
                    />
                  </div>
                  {doctor.consultationFee === null && (
                    <span className="mt-1 block text-xs text-ink-muted">Not set: the default fee applies</span>
                  )}
                </Td>
                <Td align="right">
                  <Button
                    size="sm"
                    onClick={() => save(doctor)}
                    disabled={!changed(doctor) || savingId === doctor.doctorId}
                    aria-label={`Save the fee of Dr. ${doctor.doctorName}`}
                  >
                    Save
                  </Button>
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}
