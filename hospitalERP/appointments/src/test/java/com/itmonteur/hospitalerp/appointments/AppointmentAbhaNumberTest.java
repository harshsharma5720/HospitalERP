package com.itmonteur.hospitalerp.appointments;

import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtRelative;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Whose ABHA number a visit's prescription and receipt show (docs/ABHA_NUMBER_PLAN.md, AB.2). */
class AppointmentAbhaNumberTest {

    private static Appointment visit(String name) {
        PtInfo patient = new PtInfo();
        patient.setPatientName("Asha Rao");
        patient.setAbhaNumber("91000000000001");
        Appointment appointment = new Appointment();
        appointment.setPtInfo(patient);
        appointment.setPatientName(name);
        return appointment;
    }

    @Test
    void aVisitUnderThePatientsOwnNameShowsTheirs() {
        assertThat(visit("Asha Rao").abhaNumber()).isEqualTo("91000000000001");
        assertThat(visit(" asha rao ").abhaNumber()).as("case and spaces aside").isEqualTo("91000000000001");
    }

    @Test
    void aFamilyMembersVisitShowsTheirOwnEvenWhenTheyHaveNone() {
        Appointment appointment = visit("Mira Rao");
        PtRelative mira = new PtRelative();
        mira.setName("Mira Rao");
        mira.setAbhaNumber("91000000000002");
        appointment.setRelative(mira);
        assertThat(appointment.abhaNumber()).isEqualTo("91000000000002");

        mira.setAbhaNumber(null);
        appointment.setPatientName("Asha Rao");
        assertThat(appointment.abhaNumber()).as("never the patient's").isNull();
    }

    @Test
    void someoneElseUnderThePatientsAccountShowsNone() {
        assertThat(visit("Ravi Rao").abhaNumber()).as("a name typed at booking, or a family member since removed").isNull();
        Appointment walkIn = new Appointment();
        walkIn.setPatientName("Asha Rao");
        assertThat(walkIn.abhaNumber()).isNull();
        Appointment unnamed = visit("Asha Rao");
        unnamed.getPtInfo().setPatientName(null);
        assertThat(unnamed.abhaNumber()).isNull();
    }
}
