package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP helpers for the end-to-end tests that run in FeatureFlowH2Test's context (admin "flowadmin"). Not a Spring
 * bean: each test class creates one from its TestRestTemplate and ObjectMapper.
 */
final class TestApi {

    record Doctor(long userId, long id) {}

    private final TestRestTemplate rest;
    private final ObjectMapper json;

    TestApi(TestRestTemplate rest, ObjectMapper json) {
        this.rest = rest;
        this.json = json;
    }

    ResponseEntity<String> call(HttpMethod method, String url, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    /** GET that must answer 200; the body as JSON. */
    JsonNode get(String token, String url) throws Exception {
        ResponseEntity<String> res = call(HttpMethod.GET, url, token, null);
        assertThat(res.getStatusCode()).as(url + " " + res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    String login(String username, String password) {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return token(res);
    }

    String admin() {
        return login("flowadmin", "flow-admin-123");
    }

    /** A new staff account (RECEPTIONIST, ADMIN, ...) with the password "staff-123", logged in. */
    String staff(String username, String role) {
        assertThat(call(HttpMethod.POST, "/api/admin/users", admin(),
                "{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\",\"password\":\"staff-123\","
                        + "\"phoneNumber\":\"+911111112002\",\"role\":\"" + role + "\"}").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return login(username, "staff-123");
    }

    String registerPatient(String username) {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/register", null,
                "{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\","
                        + "\"password\":\"secret-123\",\"phoneNumber\":\"+919876543210\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return token(res);
    }

    /**
     * A new doctor (password "doctor-123") working tomorrow 10:00-10:30 and 16:00-17:00 and the day after
     * 09:00-09:30, in 30-minute slots, and nothing else.
     */
    Doctor doctorWithSchedule(String username) throws Exception {
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/admin/users", admin(),
                "{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\",\"password\":\"doctor-123\","
                        + "\"phoneNumber\":\"+911111112001\",\"role\":\"DOCTOR\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long userId = json.readTree(created.getBody()).get("id").asLong();
        DayOfWeek day1 = LocalDate.now().plusDays(1).getDayOfWeek();
        DayOfWeek day2 = day1.plus(1);
        List<String> entries = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            entries.add(scheduleEntry(day, "MORNING", day == day1 ? "10:00-10:30" : day == day2 ? "09:00-09:30" : null));
            entries.add(scheduleEntry(day, "EVENING", day == day1 ? "16:00-17:00" : null));
        }
        assertThat(call(HttpMethod.PUT, "/api/doctor/" + userId + "/schedule", login(username, "doctor-123"),
                "[" + String.join(",", entries) + "]").getStatusCode()).isEqualTo(HttpStatus.OK);
        for (JsonNode d : get(null, "/api/doctor/getAll")) {
            if (username.equals(d.get("userName").asText())) {
                return new Doctor(userId, d.get("id").asLong());
            }
        }
        throw new AssertionError("doctor not found");
    }

    /**
     * Three walk-in visits with a doctor from {@link #doctorWithSchedule} - booked for tomorrow 10:00, 16:00 and 16:30,
     * then moved to today in the database, so tests of today's queue don't depend on the time of day. Patients are
     * named "{surname} 1..3", phones "{phonePrefix}01..03" (10 digits in all). Returns the appointment ids in time order.
     */
    List<Long> visitsToday(String desk, long doctorId, String surname, String phonePrefix, JdbcTemplate jdbc)
            throws Exception {
        JsonNode slots = get(desk, "/api/receptionist/doctors/" + doctorId + "/next-free-slots?limit=3");
        List<Long> ids = new ArrayList<>();
        for (JsonNode slot : slots) {
            ResponseEntity<String> res = call(HttpMethod.POST, "/api/receptionist/walk-in", desk,
                    "{\"newPatient\":{\"name\":\"" + surname + " " + (ids.size() + 1) + "\",\"phone\":\""
                            + phonePrefix + "%02d".formatted(ids.size() + 1) + "\",\"gender\":\"OTHER\"},"
                            + "\"age\":40,\"slotId\":" + slot.get("slotId").asLong() + "}");
            assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
            JsonNode appointment = json.readTree(res.getBody()).get("appointment");
            assertThat(appointment.get("tokenNumber").isNull()).as("booked for tomorrow: not checked in").isTrue();
            ids.add(appointment.get("appointmentID").asLong());
            jdbc.update("UPDATE slots SET date = ? WHERE id = ?", LocalDate.now(), slot.get("slotId").asLong());
            jdbc.update("UPDATE appointments SET date = ? WHERE appointmentid = ?", LocalDate.now(), ids.get(ids.size() - 1));
        }
        return ids;
    }

    /** The free slots of a doctor from {@link #doctorWithSchedule} tomorrow; {@code shift}: MORNING or EVENING. */
    JsonNode freeSlotsTomorrow(String token, long doctorId, String shift) throws Exception {
        return get(token, "/api/slots/available/" + doctorId + "?date=" + LocalDate.now().plusDays(1) + "&shift=" + shift);
    }

    /**
     * A patient's own booking (or for a family member: {@code relativeId}) of a slot from {@link #freeSlotsTomorrow},
     * then moved to today in the database so the front desk can check it in. Returns the appointment id.
     */
    long bookedForToday(String patient, long slotId, Long relativeId, JdbcTemplate jdbc) throws Exception {
        ResponseEntity<String> booked = call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + slotId + (relativeId == null ? "" : ",\"relativeId\":" + relativeId) + "}");
        assertThat(booked.getStatusCode()).as(booked.getBody()).isEqualTo(HttpStatus.OK);
        long appointmentId = json.readTree(booked.getBody()).get("appointmentID").asLong();
        jdbc.update("UPDATE slots SET date = ? WHERE id = ?", LocalDate.now(), slotId);
        jdbc.update("UPDATE appointments SET date = ? WHERE appointmentid = ?", LocalDate.now(), appointmentId);
        return appointmentId;
    }

    private static String scheduleEntry(DayOfWeek day, String shift, String hours) {
        String start = "{\"dayOfWeek\":\"" + day + "\",\"shift\":\"" + shift + "\",";
        if (hours == null) {
            return start + "\"working\":false}";
        }
        String[] times = hours.split("-");
        return start + "\"working\":true,\"startTime\":\"" + times[0] + "\",\"endTime\":\"" + times[1] + "\",\"slotMinutes\":30}";
    }

    private String token(ResponseEntity<String> res) {
        try {
            return json.readTree(res.getBody()).get("token").asText();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
