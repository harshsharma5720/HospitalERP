package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Patients can't cancel or move a visit less than 2 hours before it starts (docs/CANCELLATION_CUTOFF_PLAN.md); the
 * front desk can. Same configuration as FeatureFlowH2Test, so both share one application context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class CancellationCutoffH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    // A patient's booking of the doctor's first free slot tomorrow in that shift
    private long book(String patient, long doctorId, String shift) throws Exception {
        long slotId = api.freeSlotsTomorrow(patient, doctorId, shift).get(0).get("id").asLong();
        ResponseEntity<String> booked = api.call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}");
        assertThat(booked.getStatusCode()).as(booked.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(booked.getBody()).get("appointmentID").asLong();
    }

    // Moves the visit (and its slot) so that it starts at that time
    private void startsAt(long appointmentId, LocalDateTime start) {
        jdbc.update("UPDATE slots SET date = ?, start_time = ?, end_time = ? WHERE id = "
                        + "(SELECT slot_id FROM appointments WHERE appointmentid = ?)",
                start.toLocalDate(), start.toLocalTime(), start.toLocalTime().plusMinutes(30), appointmentId);
        jdbc.update("UPDATE appointments SET date = ? WHERE appointmentid = ?", start.toLocalDate(), appointmentId);
    }

    private JsonNode mine(String patient, long appointmentId) throws Exception {
        for (JsonNode a : api.get(patient, "/appointment/getPatientAppointments")) {
            if (a.get("appointmentID").asLong() == appointmentId) {
                return a;
            }
        }
        throw new AssertionError("appointment " + appointmentId + " not in the patient's list");
    }

    @Test
    void patientsCantCancelOrMoveAVisitLessThanTwoHoursBeforeItButTheDeskCan() throws Exception {
        String desk = api.staff("deskcut", "RECEPTIONIST");
        long doctorId = api.doctorWithSchedule("drcut").id();
        String patient = api.registerPatient("cutpatient");
        long soon = book(patient, doctorId, "MORNING");
        long later = book(patient, doctorId, "EVENING");
        long spareSlot = api.freeSlotsTomorrow(patient, doctorId, "EVENING").get(0).get("id").asLong();

        assertThat(mine(patient, soon).get("changeableUntil").asText())
                .as("tomorrow 10:00 less 2 hours").isEqualTo(LocalDate.now().plusDays(1) + "T08:00:00");

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        startsAt(soon, now.plusHours(1));
        startsAt(later, now.plusHours(3));
        assertThat(LocalDateTime.parse(mine(patient, soon).get("changeableUntil").asText())).isEqualTo(now.minusHours(1));

        ResponseEntity<String> cancel = api.call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + soon, patient, null);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json.readTree(cancel.getBody()).get("message").asText())
                .isEqualTo("Less than 2 hours to go: this visit can't be cancelled or moved online any more. Please call the hospital.");
        assertThat(api.call(HttpMethod.PUT, "/appointment/update/" + soon, patient, "{\"slotId\":" + spareSlot + "}")
                .getStatusCode()).as("moving it").isEqualTo(HttpStatus.CONFLICT);
        assertThat(api.call(HttpMethod.PUT, "/appointment/update/" + soon, patient, "{\"message\":\"Running late\"}")
                .getStatusCode()).as("the reason may still change").isEqualTo(HttpStatus.OK);
        assertThat(mine(patient, soon).get("status").asText()).isEqualTo("SCHEDULED");

        assertThat(api.call(HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + soon, desk, null).getStatusCode())
                .as("the front desk can").isEqualTo(HttpStatus.OK);
        assertThat(api.call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + later, patient, null).getStatusCode())
                .as("3 hours before").isEqualTo(HttpStatus.OK);
        assertThat(mine(patient, later).get("changeableUntil").isNull()).as("cancelled: nothing to change").isTrue();
    }
}
