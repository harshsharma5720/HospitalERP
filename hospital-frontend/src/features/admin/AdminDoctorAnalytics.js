import React, { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { formatSpecialization } from "../../shared/dashboard/dashboardFormat";
import DoctorAnalytics from "../doctors/analytics/DoctorAnalytics";
import { PageHeader } from "../../shared/ui";

// One doctor's analytics for an admin (docs/DOCTOR_ANALYTICS_PLAN.md), from the Doctors page:
// /admin/doctor/:doctorId/analytics
export default function AdminDoctorAnalytics() {
  const { doctorId } = useParams();
  const [doctor, setDoctor] = useState(null);

  return (
    <div className="w-full">
      <PageHeader
        back={
          <Link to="/admin/manage-doctor" className="text-primary-ink hover:underline">
            ← Doctors
          </Link>
        }
        title={doctor?.name ? `Dr. ${doctor.name}` : "Doctor analytics"}
        description={doctor ? `${formatSpecialization(doctor.specialist)} · Analytics` : null}
      />
      <DoctorAnalytics doctorId={doctorId} onDoctor={setDoctor} />
    </div>
  );
}
