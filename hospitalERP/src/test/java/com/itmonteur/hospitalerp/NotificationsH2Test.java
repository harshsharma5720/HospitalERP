package com.itmonteur.hospitalerp;

import ITmonteur.example.hospitalERP.events.AppointmentNotificationEvent;
import ITmonteur.example.hospitalERP.services.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Which notifications go out, and when. NotificationService is replaced by a mock, so nothing
 * is really sent; the test checks the calls. (Runs the whole app on H2, like FeatureFlowH2Test.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:notify;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=notifyadmin", "ADMIN_PASSWORD=notify-admin-123"
})
class NotificationsH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private NotificationService notificationService;

    @Test
    void bookingAndCancellingNotifyOnceAndAFailedBookingNotifiesNothing() throws Exception {
        String admin = login("notifyadmin", "notify-admin-123");
        call(HttpMethod.POST, "/api/admin/users", admin, """
                {"username":"drnotify","email":"drnotify@example.com","password":"doctor-123",
                 "phoneNumber":"+911111111118","role":"DOCTOR"}""");
        String patient = register("notifypatient");
        long doctorId = findDoctorId("drnotify");
        JsonNode slots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + doctorId
                + "?date=" + LocalDate.now().plusDays(1) + "&shift=MORNING", patient, null).getBody());
        long slotId = slots.get(0).get("id").asLong();

        ResponseEntity<String> booked = call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}");
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService, timeout(3000).times(1)).appointmentBooked(argThat(info ->
                info.patientName().equals("notifypatient") && info.doctorName().equals("drnotify")));

        // Same slot again: refused, and no second "booked" message
        assertThat(call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}")
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(notificationService, after(500).times(1)).appointmentBooked(argThat(info ->
                info.patientName().equals("notifypatient")));

        long appointmentId = json.readTree(booked.getBody()).get("appointmentID").asLong();
        assertThat(call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + appointmentId, patient, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService, timeout(3000).times(1)).appointmentCancelled(argThat(info ->
                info.patientName().equals("notifypatient")));
    }

    // Step 1.9: messages wait for the commit, so a rolled-back change never notifies anyone
    @Test
    void notificationsAreSentOnlyAfterACommit() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new AppointmentNotificationEvent(
                    AppointmentNotificationEvent.Kind.BOOKED, info("Rolled Back")));
            status.setRollbackOnly();
        });
        verify(notificationService, after(500).never()).appointmentBooked(argThat(i ->
                i.patientName().equals("Rolled Back")));

        tx.executeWithoutResult(status -> publisher.publishEvent(new AppointmentNotificationEvent(
                AppointmentNotificationEvent.Kind.BOOKED, info("Committed"))));
        verify(notificationService, timeout(3000)).appointmentBooked(argThat(i ->
                i.patientName().equals("Committed")));
    }

    private static NotificationService.AppointmentInfo info(String patientName) {
        return new NotificationService.AppointmentInfo(patientName, null, null, "Dr Test", null, null,
                "2026-01-01", "09:00 - 09:10");
    }

    // ------------------------------------------------------------------ helpers

    private String register(String username) throws Exception {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/register", null,
                "{\"username\":\"" + username + "\",\"email\":\"" + username + "@example.com\","
                        + "\"password\":\"secret-123\",\"phoneNumber\":\"+919876543210\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody()).get("token").asText();
    }

    private String login(String username, String password) throws Exception {
        ResponseEntity<String> res = call(HttpMethod.POST, "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody()).get("token").asText();
    }

    private long findDoctorId(String userName) throws Exception {
        for (JsonNode d : json.readTree(call(HttpMethod.GET, "/api/doctor/getAll", null, null).getBody())) {
            if (userName.equals(d.get("userName").asText())) {
                return d.get("id").asLong();
            }
        }
        throw new AssertionError("doctor not found");
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
