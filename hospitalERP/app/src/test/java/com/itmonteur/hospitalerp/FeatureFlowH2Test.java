package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end over HTTP on H2: admin creates a doctor, the doctor sets a schedule,
 * a patient books from it, the doctor writes a consultation, and the patient downloads
 * the prescription PDF. Also checks the public forgot-password endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class FeatureFlowH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;

    @Test
    void scheduleBookingConsultationAndPrescription() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");

        // Admin creates a doctor
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drrao","email":"drrao@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111111","role":"DOCTOR"}""");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long doctorUserId = json.readTree(created.getBody()).get("id").asLong();
        String doctor = login("drrao", "doctor-123");

        // Doctor works 10:00-11:00 in 30-minute slots tomorrow morning
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ResponseEntity<String> schedule = call(HttpMethod.PUT, "/api/doctor/" + doctorUserId + "/schedule", doctor,
                "[{\"dayOfWeek\":\"" + tomorrow.getDayOfWeek() + "\",\"shift\":\"MORNING\",\"working\":true,"
                        + "\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"slotMinutes\":30}]");
        assertThat(schedule.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(schedule.getBody())).hasSize(14);

        // Patient registers and sees exactly the two configured slots
        String patient = register("asha");
        long doctorId = findDoctorId("drrao");
        JsonNode slots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + tomorrow + "&shift=MORNING", patient, null).getBody());
        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).get("startTime").asText()).startsWith("10:00");

        ResponseEntity<String> booked = call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + slots.get(0).get("id").asLong() + ",\"message\":\"Headache\"}");
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.OK);
        long appointmentId = json.readTree(booked.getBody()).get("appointmentID").asLong();

        // Doctor records the consultation -> appointment becomes COMPLETED
        ResponseEntity<String> consultation = call(HttpMethod.PUT, "/api/consultations/appointment/" + appointmentId, doctor, """
                {"symptoms":"Headache for 2 days","diagnosis":"Tension headache","bloodPressure":"118/76",
                 "pulse":72,"medicines":[{"medicineName":"Paracetamol","dosage":"500 mg","frequency":"1-0-1","duration":"3 days"}]}""");
        assertThat(consultation.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode appointment = json.readTree(call(HttpMethod.GET, "/appointment/appointmentId/" + appointmentId, patient, null).getBody());
        assertThat(appointment.get("status").asText()).isEqualTo("COMPLETED");

        // Patient sees the history and downloads the prescription
        JsonNode history = json.readTree(call(HttpMethod.GET, "/api/consultations/my", patient, null).getBody());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("diagnosis").asText()).isEqualTo("Tension headache");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(patient);
        ResponseEntity<byte[]> pdf = rest.exchange("/api/consultations/appointment/" + appointmentId + "/prescription.pdf",
                HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(new String(pdf.getBody(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        // Someone else's medical record is off limits
        String stranger = register("mallory2");
        assertThat(call(HttpMethod.GET, "/api/consultations/appointment/" + appointmentId, stranger, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // ...and a patient cannot change a doctor's schedule
        assertThat(call(HttpMethod.GET, "/api/doctor/" + doctorUserId + "/schedule", stranger, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void forgotPasswordDoesNotRevealAccountsAndRejectsBadCodes() {
        ResponseEntity<String> unknown = call(HttpMethod.POST, "/api/auth/forgot-password", null,
                "{\"identifier\":\"no-such-user\"}");
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.OK);

        register("resetme");
        ResponseEntity<String> known = call(HttpMethod.POST, "/api/auth/forgot-password", null,
                "{\"identifier\":\"resetme\"}");
        assertThat(known.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(known.getBody()).isEqualTo(unknown.getBody());

        ResponseEntity<String> bad = call(HttpMethod.POST, "/api/auth/reset-password", null,
                "{\"identifier\":\"resetme\",\"code\":\"123456\",\"newPassword\":\"whatever-123\"}");
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // The admin endpoints read the new profile right after creating the user, so they only
    // succeed if the UserRegisteredEvent listeners created it inside the same transaction.
    @Test
    void creatingUsersCreatesTheirProfiles() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");
        String body = "{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"secret-123\","
                + "\"phoneNumber\":\"+919812345678\"}";

        ResponseEntity<String> doctor = call(HttpMethod.POST, "/api/admin/doctor", admin, body.formatted("drnew", "drnew"));
        assertThat(doctor.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(doctor.getBody()).get("specialist").asText()).isEqualTo("NOT_ASSIGNED");

        ResponseEntity<String> receptionist = call(HttpMethod.POST, "/api/admin/receptionist", admin,
                body.formatted("frontdesk", "frontdesk"));
        assertThat(receptionist.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(receptionist.getBody()).get("userName").asText()).isEqualTo("frontdesk");

        ResponseEntity<String> patient = call(HttpMethod.POST, "/api/admin/patient", admin, body.formatted("walkin", "walkin"));
        assertThat(patient.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(patient.getBody()).get("patientName").asText()).isEqualTo("walkin");

        // A self-registered patient can use patient endpoints straight away (profile exists)
        String self = register("selfreg");
        assertThat(call(HttpMethod.GET, "/appointment/getPatientAppointments", self, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // Account deletion must remove the profile, everything that references it, and the login.
    // Both deletions happen while the accounts still have upcoming bookings.
    @Test
    void deletingAccountsRemovesProfilesAndLogins() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");

        // A doctor working tomorrow 10:00-11:30 in 30-minute slots (3 slots)
        long doctorUserId = json.readTree(call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drgone","email":"drgone@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111112","role":"DOCTOR"}""").getBody()).get("id").asLong();
        String doctor = login("drgone", "doctor-123");
        long doctorId = findDoctorId("drgone");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThat(call(HttpMethod.PUT, "/api/doctor/" + doctorUserId + "/schedule", doctor,
                "[{\"dayOfWeek\":\"" + tomorrow.getDayOfWeek() + "\",\"shift\":\"MORNING\",\"working\":true,"
                        + "\"startTime\":\"10:00\",\"endTime\":\"11:30\",\"slotMinutes\":30}]").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        String slotsUrl = "/api/slots/available/" + doctorId + "?date=" + tomorrow + "&shift=MORNING";

        // "leaver": a completed visit with a consultation + an upcoming booking
        String leaver = register("leaver");
        JsonNode slots = json.readTree(call(HttpMethod.GET, slotsUrl, leaver, null).getBody());
        assertThat(slots).hasSize(3);
        long visit = book(leaver, slots.get(0).get("id").asLong());
        long leaversUpcomingSlot = slots.get(1).get("id").asLong();
        book(leaver, leaversUpcomingSlot);
        assertThat(call(HttpMethod.PUT, "/api/consultations/appointment/" + visit, doctor,
                "{\"diagnosis\":\"Migraine\",\"medicines\":[{\"medicineName\":\"Paracetamol\"}]}").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // "stayer": an upcoming booking with the same doctor
        String stayer = register("stayer");
        book(stayer, slots.get(2).get("id").asLong());

        // 1) leaver deletes their own account: login gone, their upcoming slot is bookable again
        assertThat(call(HttpMethod.DELETE, "/api/patient/deleteAccount/" + userIdOf(leaver), leaver, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginStatus("leaver", "secret-123")).isEqualTo(HttpStatus.UNAUTHORIZED);
        JsonNode freeAgain = json.readTree(call(HttpMethod.GET, slotsUrl, stayer, null).getBody());
        assertThat(freeAgain.findValuesAsText("id")).contains(String.valueOf(leaversUpcomingSlot));

        // 2) admin deletes the doctor (stayer still booked): profile, bookings and login all go
        assertThat(call(HttpMethod.DELETE, "/api/doctor/delete/" + doctorId, admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(loginStatus("drgone", "doctor-123")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, "/api/doctor/getDoctor/" + doctorId, null, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json.readTree(call(HttpMethod.GET, "/appointment/getPatientAppointments", stayer, null).getBody()))
                .isEmpty();

        // 3) admin deletes a receptionist (by receptionist id)
        ResponseEntity<String> receptionist = call(HttpMethod.POST, "/api/admin/receptionist", admin, """
                {"username":"deskgone","email":"deskgone@example.com","password":"desk-1234","phoneNumber":"+919812345679"}""");
        long receptionistId = json.readTree(receptionist.getBody()).get("id").asLong();
        assertThat(call(HttpMethod.DELETE, "/api/receptionist/delete/" + receptionistId, admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(loginStatus("deskgone", "desk-1234")).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // Deleting a relative keeps their appointment history; only the link to the relative goes
    @Test
    void deletingARelativeKeepsTheirAppointments() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");
        call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drrel","email":"drrel@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111113","role":"DOCTOR"}""");
        long doctorId = findDoctorId("drrel");
        String patient = register("parent");

        ResponseEntity<String> added = call(HttpMethod.POST, "/api/patient/relative/add", patient, """
                {"name":"Little Ravi","gender":"MALE","dob":"2018-05-01","relationship":"SON"}""");
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
        long relativeId = json.readTree(added.getBody()).get("id").asLong();

        JsonNode slots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + LocalDate.now().plusDays(1) + "&shift=MORNING", patient, null).getBody());
        ResponseEntity<String> booked = call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + slots.get(0).get("id").asLong() + ",\"relativeId\":" + relativeId + "}");
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(booked.getBody()).get("relativeId").asLong()).isEqualTo(relativeId);

        assertThat(call(HttpMethod.DELETE, "/api/patient/relative/delete/" + relativeId, patient, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode appointments = json.readTree(call(HttpMethod.GET, "/appointment/getPatientAppointments", patient, null).getBody());
        assertThat(appointments).hasSize(1);
        assertThat(appointments.get(0).get("patientName").asText()).isEqualTo("Little Ravi");
        assertThat(appointments.get(0).get("relativeId").isNull()).isTrue();
        assertThat(call(HttpMethod.GET, "/api/patient/relative/" + relativeId, patient, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Approving a doctor's leave cancels their bookings in that period and blocks the free slots
    @Test
    void approvingDoctorLeaveCancelsBookingsAndBlocksSlots() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");
        call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drleave","email":"drleave@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111114","role":"DOCTOR"}""");
        String doctor = login("drleave", "doctor-123");
        long doctorId = findDoctorId("drleave");
        String patient = register("leavepatient");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = tomorrow.plusDays(1);

        JsonNode tomorrowSlots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + tomorrow + "&shift=MORNING", patient, null).getBody());
        long bookedTomorrow = book(patient, tomorrowSlots.get(0).get("id").asLong());
        long freeSlotTomorrow = tomorrowSlots.get(1).get("id").asLong();
        JsonNode dayAfterSlots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + dayAfter + "&shift=MORNING", patient, null).getBody());
        long bookedDayAfter = book(patient, dayAfterSlots.get(0).get("id").asLong());

        // Leave for tomorrow only
        ResponseEntity<String> applied = call(HttpMethod.POST, "/api/leaves/apply", doctor,
                "{\"startDate\":\"" + tomorrow + "\",\"endDate\":\"" + tomorrow + "\",\"reason\":\"Conference\"}");
        assertThat(applied.getStatusCode()).isEqualTo(HttpStatus.OK);
        long leaveId = json.readTree(applied.getBody()).get("id").asLong();
        assertThat(call(HttpMethod.PUT, "/api/admin/approve/" + leaveId, admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Tomorrow's booking is cancelled, the day after is untouched
        assertThat(appointmentStatus(patient, bookedTomorrow)).isEqualTo("CANCELLED_BY_DOCTOR");
        assertThat(appointmentStatus(patient, bookedDayAfter)).isEqualTo("SCHEDULED");
        // Tomorrow's free slot is blocked (booking it directly is refused), and none are offered
        assertThat(call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + freeSlotTomorrow + "}").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + tomorrow + "&shift=MORNING", patient, null).getBody())).isEmpty();
        assertThat(json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + dayAfter + "&shift=MORNING", patient, null).getBody())).isNotEmpty();
    }

    // Every endpoint that step 1.6 moves to another controller/module, exercised end to end
    @Test
    void endpointsMovedInStep16KeepWorking() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");
        long doctorUserId = json.readTree(call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drmove","email":"drmove@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111115","role":"DOCTOR"}""").getBody()).get("id").asLong();
        call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drother","email":"drother@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111116","role":"DOCTOR"}""");
        call(HttpMethod.POST, "/api/admin/receptionist", admin, """
                {"username":"deskmove","email":"deskmove@example.com","password":"desk-1234","phoneNumber":"+919812345670"}""");
        String doctor = login("drmove", "doctor-123");
        String otherDoctor = login("drother", "doctor-123");
        String desk = login("deskmove", "desk-1234");
        long doctorId = findDoctorId("drmove");

        // Public doctor directory under /api/patient (no login needed)
        assertThat(call(HttpMethod.GET, "/api/patient/getAllDoctors", null, null).getBody()).contains("drmove");
        assertThat(call(HttpMethod.GET, "/api/patient/getAllBySpecialization?specialization=NOT_ASSIGNED", null, null)
                .getBody()).contains("drmove");
        assertThat(json.readTree(call(HttpMethod.GET, "/api/patient/getAllBySpecialization?specialization=nonsense",
                null, null).getBody())).isEmpty();

        // Schedule endpoint (doctor's own)
        assertThat(json.readTree(call(HttpMethod.GET, "/api/doctor/" + doctorUserId + "/schedule", doctor, null)
                .getBody())).hasSize(14);

        // A patient books one slot; the receptionist books another on the patient's behalf
        String patient = register("movepatient");
        long patientId = json.readTree(call(HttpMethod.GET, "/api/patient/getAccount/" + userIdOf(patient), patient, null)
                .getBody()).get("patientId").asLong();
        JsonNode slots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + LocalDate.now().plusDays(1) + "&shift=EVENING", patient, null).getBody());
        long byPatient = book(patient, slots.get(0).get("id").asLong());
        ResponseEntity<String> deskBooking = call(HttpMethod.POST, "/api/receptionist/NewAppointment", desk,
                "{\"slotId\":" + slots.get(1).get("id").asLong() + ",\"ptInfoId\":" + patientId + "}");
        assertThat(deskBooking.getStatusCode()).isEqualTo(HttpStatus.OK);
        long byDesk = json.readTree(deskBooking.getBody()).get("appointmentID").asLong();

        // Receptionist lists
        assertThat(ids(call(HttpMethod.GET, "/api/receptionist/getAppointments", desk, null))).contains(byPatient, byDesk);
        assertThat(ids(call(HttpMethod.GET, "/api/receptionist/getAppointmentByDoctor/drmove", desk, null)))
                .containsExactlyInAnyOrder(byPatient, byDesk);
        // Pending lists for the doctor (doctor, receptionist and admin views)
        for (String[] view : new String[][]{{"/api/doctor", doctor}, {"/api/receptionist", desk}, {"/api/admin", admin}}) {
            assertThat(ids(call(HttpMethod.GET, view[0] + "/doctorPendingAppointments/" + doctorUserId, view[1], null)))
                    .containsExactlyInAnyOrder(byPatient, byDesk);
        }

        // Only the appointment's own doctor can complete it
        assertThat(call(HttpMethod.PUT, "/api/doctor/complete/" + byPatient, otherDoctor, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> completed = call(HttpMethod.PUT, "/api/doctor/complete/" + byPatient, doctor, null);
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody()).isEqualTo("Appointment marked as completed successfully.");
        for (String[] view : new String[][]{{"/api/doctor", doctor}, {"/api/receptionist", desk}, {"/api/admin", admin}}) {
            assertThat(ids(call(HttpMethod.GET, view[0] + "/doctorCompletedAppointments/" + doctorUserId, view[1], null)))
                    .containsExactly(byPatient);
        }
        JsonNode counts = json.readTree(call(HttpMethod.GET, "/api/admin/doctorAppointmentCount/" + doctorId, admin, null).getBody());
        assertThat(counts.get("pending").asLong()).isEqualTo(1);
        assertThat(counts.get("completed").asLong()).isEqualTo(1);

        // Receptionist cancels the other booking (kept as history)
        ResponseEntity<String> cancelled = call(HttpMethod.DELETE, "/api/receptionist/deleteAppointment/" + byDesk, desk, null);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody()).isEqualTo("Appointment cancelled successfully!");
        assertThat(appointmentStatus(patient, byDesk)).isEqualTo("CANCELLED_BY_PATIENT");
    }

    // Changing a schedule keeps booked slots, removes unused future slots and offers only the new hours
    @Test
    void changingAScheduleKeepsBookingsAndRemovesUnusedSlots() throws Exception {
        String admin = login("flowadmin", "flow-admin-123");
        long doctorUserId = json.readTree(call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drhours","email":"drhours@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111117","role":"DOCTOR"}""").getBody()).get("id").asLong();
        String doctor = login("drhours", "doctor-123");
        long doctorId = findDoctorId("drhours");
        String patient = register("hourspatient");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String slotsUrl = "/api/slots/available/" + doctorId + "?date=" + tomorrow + "&shift=MORNING";

        // Default hours: 09:00-12:00 in 10-minute slots; book 09:00, remember the free 09:10 slot
        JsonNode before = json.readTree(call(HttpMethod.GET, slotsUrl, patient, null).getBody());
        assertThat(before.get(0).get("startTime").asText()).startsWith("09:00");
        long booked = book(patient, before.get(0).get("id").asLong());
        long unusedOldSlot = before.get(1).get("id").asLong();

        // Doctor now works 10:00-11:00 in 30-minute slots tomorrow morning
        assertThat(call(HttpMethod.PUT, "/api/doctor/" + doctorUserId + "/schedule", doctor,
                "[{\"dayOfWeek\":\"" + tomorrow.getDayOfWeek() + "\",\"shift\":\"MORNING\",\"working\":true,"
                        + "\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"slotMinutes\":30}]").getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // The unused old slot was deleted (404), not just blocked
        assertThat(call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + unusedOldSlot + "}").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // The booking and its 09:00 slot survive
        JsonNode appointment = json.readTree(call(HttpMethod.GET, "/appointment/appointmentId/" + booked, patient, null).getBody());
        assertThat(appointment.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(appointment.get("startTime").asText()).startsWith("09:00");
        // Only the new hours are offered
        List<String> offered = new java.util.ArrayList<>();
        json.readTree(call(HttpMethod.GET, slotsUrl, patient, null).getBody())
                .forEach(s -> offered.add(s.get("startTime").asText().substring(0, 5)));
        assertThat(offered).containsExactly("10:00", "10:30");
    }

    private List<Long> ids(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Long> ids = new java.util.ArrayList<>();
        json.readTree(response.getBody()).forEach(a -> ids.add(a.get("appointmentID").asLong()));
        return ids;
    }

    private String appointmentStatus(String token, long appointmentId) throws Exception {
        return json.readTree(call(HttpMethod.GET, "/appointment/appointmentId/" + appointmentId, token, null)
                .getBody()).get("status").asText();
    }

    private long book(String patientToken, long slotId) throws Exception {
        ResponseEntity<String> res = call(HttpMethod.POST, "/appointment/NewAppointment", patientToken,
                "{\"slotId\":" + slotId + "}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody()).get("appointmentID").asLong();
    }

    // ------------------------------------------------------------------ helpers

    private HttpStatusCode loginStatus(String username, String password) {
        return call(HttpMethod.POST, "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}").getStatusCode();
    }

    // Reads the userId claim from the (unverified) JWT payload
    private long userIdOf(String token) throws Exception {
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        return json.readTree(payload).get("userId").asLong();
    }

    private String register(String username) {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/register", null,
                "{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\","
                        + "\"password\":\"secret-123\",\"phoneNumber\":\"+919876543210\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return token(res);
    }

    private String login(String username, String password) {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return token(res);
    }

    private long findDoctorId(String userName) throws Exception {
        for (JsonNode d : json.readTree(call(HttpMethod.GET, "/api/doctor/getAll", null, null).getBody())) {
            if (userName.equals(d.get("userName").asText())) {
                return d.get("id").asLong();
            }
        }
        throw new AssertionError("doctor not found");
    }

    private String token(ResponseEntity<String> res) {
        try {
            return json.readTree(res.getBody()).get("token").asText();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private ResponseEntity<String> call(HttpMethod method, String url, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }
}
