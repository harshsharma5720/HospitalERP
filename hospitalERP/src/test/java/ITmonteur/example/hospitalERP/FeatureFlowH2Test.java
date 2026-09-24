package ITmonteur.example.hospitalERP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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
        "spring.jpa.hibernate.ddl-auto=create-drop",
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

    // ------------------------------------------------------------------ helpers

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
