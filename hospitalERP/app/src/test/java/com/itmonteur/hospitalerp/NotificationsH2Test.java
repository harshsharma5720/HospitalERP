package com.itmonteur.hospitalerp;

import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.appointments.internal.AppointmentReminderService;
import com.itmonteur.hospitalerp.notifications.EmailService;
import com.itmonteur.hospitalerp.notifications.NotificationService;
import com.itmonteur.hospitalerp.notifications.OutboxChannel;
import com.itmonteur.hospitalerp.notifications.OutboxStatus;
import com.itmonteur.hospitalerp.notifications.SmsService;
import com.itmonteur.hospitalerp.notifications.internal.OutboxMessage;
import com.itmonteur.hospitalerp.notifications.internal.OutboxRepository;
import com.itmonteur.hospitalerp.notifications.internal.OutboxSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Appointment notifications through the outbox (docs/RELIABLE_NOTIFICATIONS_PLAN.md, step C.2). Email and SMS
 * delivery are mocks; the background sender is off, so the test sees what was queued and then runs the sender.
 * (Runs the whole app on H2, like FeatureFlowH2Test.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:notify;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false", "app.notifications.sender.enabled=false",
        "ADMIN_USERNAME=notifyadmin", "ADMIN_PASSWORD=notify-admin-123"
})
class NotificationsH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private OutboxRepository outbox;
    @Autowired private OutboxSender sender;
    @Autowired private AppointmentReminderService reminders;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    @BeforeEach
    void channelsWork() {
        when(emailService.isEnabled()).thenReturn(true);
        when(smsService.isEnabled()).thenReturn(true);
    }

    @Test
    void bookingQueuesFourMessagesThatAreSentAfterTheCommit() throws Exception {
        String admin = login("notifyadmin", "notify-admin-123");
        createDoctor(admin, "drnotify", "+911111111118");
        String patient = register("notifypatient");
        long slotId = firstFreeSlot(patient, "drnotify");

        ResponseEntity<String> booked = call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}");
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<OutboxMessage> queued = messagesAbout("notifypatient");
        assertThat(queued).extracting(OutboxMessage::getDescription, OutboxMessage::getChannel, OutboxMessage::getRecipient)
                .containsExactly(
                        tuple("booking email to patient", OutboxChannel.EMAIL, "notifypatient@example.com"),
                        tuple("booking SMS to patient", OutboxChannel.SMS, "+919876543210"),
                        tuple("booking email to doctor", OutboxChannel.EMAIL, "drnotify@example.com"),
                        tuple("booking SMS to doctor", OutboxChannel.SMS, "+911111111118"));
        assertThat(queued).allSatisfy(m -> {
            assertThat(m.getStatus()).isEqualTo(OutboxStatus.PENDING);
            // retries end at the appointment (or after 24 hours, if that is sooner)
            assertThat(m.getGiveUpAt()).isAfter(m.getCreatedAt()).isBeforeOrEqualTo(m.getCreatedAt().plusHours(24));
        });
        verifyNoInteractions(emailService, smsService); // queued, not sent yet

        // The same slot again: refused, and nothing more is queued
        assertThat(call(HttpMethod.POST, "/appointment/NewAppointment", patient, "{\"slotId\":" + slotId + "}")
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(messagesAbout("notifypatient")).hasSize(4);

        sender.sendDueMessages();
        assertThat(messagesAbout("notifypatient")).extracting(OutboxMessage::getStatus).containsOnly(OutboxStatus.SENT);
        verify(emailService).sendHtml(eq("notifypatient@example.com"), eq("Appointment Confirmation - Hospital ERP"),
                contains("notifypatient"));
        verify(emailService).sendHtml(eq("drnotify@example.com"), eq("New Appointment Scheduled - Hospital ERP"),
                contains("notifypatient"));
        verify(smsService).send(eq("+919876543210"), contains("successfully scheduled"));
        verify(smsService).send(eq("+911111111118"), contains("New appointment scheduled"));

        // Cancelling queues the patient's email and SMS and the doctor's SMS
        long appointmentId = json.readTree(booked.getBody()).get("appointmentID").asLong();
        assertThat(call(HttpMethod.DELETE, "/appointment/CancelAppointment/" + appointmentId, patient, null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(messagesAbout("notifypatient")).extracting(OutboxMessage::getDescription).containsSubsequence(
                "cancellation email to patient", "cancellation SMS to patient", "cancellation SMS to doctor");
    }

    @Test
    void aMailServerThatIsDownDelaysTheMailButDoesNotLoseIt() throws Exception {
        doThrow(new MessagingException("SMTP server unreachable")).doNothing()
                .when(emailService).sendHtml(anyString(), anyString(), anyString());
        inTransaction(() -> publisher.publishEvent(new AppointmentNotificationEvent(
                AppointmentNotificationEvent.Kind.CANCELLED_BY_DOCTOR_LEAVE, info("Mail Down", "maildown@example.com"))));

        sender.sendDueMessages();
        OutboxMessage mail = mailTo("maildown@example.com");
        assertThat(mail.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(mail.getAttempts()).isEqualTo(1);
        assertThat(mail.getLastError()).isEqualTo("MessagingException: SMTP server unreachable");

        // A minute later (moved forward in the database instead of waiting) the retry goes through
        jdbc.update("UPDATE notification_outbox SET next_attempt_at = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(1), mail.getId());
        sender.sendDueMessages();
        assertThat(mailTo("maildown@example.com").getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(mailTo("maildown@example.com").getAttempts()).isEqualTo(2);
        verify(emailService, times(2)).sendHtml(eq("maildown@example.com"),
                eq("Appointment Cancelled Due to Doctor Leave - Hospital ERP"), contains("Mail Down"));
    }

    @Test
    void aRolledBackChangeQueuesNothing() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            publisher.publishEvent(new AppointmentNotificationEvent(
                    AppointmentNotificationEvent.Kind.BOOKED, info("Rolled Back", "rolledback@example.com")));
            status.setRollbackOnly();
        });
        inTransaction(() -> publisher.publishEvent(new AppointmentNotificationEvent(
                AppointmentNotificationEvent.Kind.BOOKED, info("Committed", "committed@example.com"))));

        assertThat(outbox.findAll()).extracting(OutboxMessage::getRecipient)
                .doesNotContain("rolledback@example.com").contains("committed@example.com");
    }

    @Test
    void theReminderIsMarkedSentOnlyTogetherWithItsQueuedMessages() throws Exception {
        String admin = login("notifyadmin", "notify-admin-123");
        createDoctor(admin, "drremind", "+911111111119");
        String patient = register("remindpatient");
        long appointmentId = json.readTree(call(HttpMethod.POST, "/appointment/NewAppointment", patient,
                "{\"slotId\":" + firstFreeSlot(patient, "drremind") + "}").getBody()).get("appointmentID").asLong();

        reminders.sendDayBeforeReminders();

        assertThat(messagesAbout("remindpatient")).extracting(OutboxMessage::getDescription)
                .contains("reminder email to patient", "reminder SMS to patient");
        assertThat(jdbc.queryForObject("SELECT reminder_sent FROM appointments WHERE appointmentid = ?",
                Boolean.class, appointmentId)).isTrue();
        reminders.sendDayBeforeReminders();
        assertThat(messagesAbout("remindpatient")).filteredOn(m -> m.getDescription().startsWith("reminder"))
                .as("reminded once").hasSize(2);
    }

    // ------------------------------------------------------------------ helpers

    private static NotificationService.AppointmentInfo info(String patientName, String patientEmail) {
        return new NotificationService.AppointmentInfo(patientName, patientEmail, null, "Dr Test", null, null,
                LocalDate.now().plusDays(1).toString(), "09:00 - 09:10");
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    /** Messages to or about a patient (their own, and the doctor's that name them), oldest first. */
    private List<OutboxMessage> messagesAbout(String patient) {
        return outbox.findAll().stream()
                .filter(m -> m.getRecipient().startsWith(patient) || m.getBody().contains(patient))
                .sorted(Comparator.comparing(OutboxMessage::getId)).toList();
    }

    private OutboxMessage mailTo(String address) {
        return outbox.findAll().stream().filter(m -> m.getRecipient().equals(address)).findFirst().orElseThrow();
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }

    private void createDoctor(String adminToken, String username, String phone) {
        assertThat(call(HttpMethod.POST, "/api/admin/users", adminToken, "{\"username\":\"" + username + "\",\"email\":\""
                + username + "@example.com\",\"password\":\"doctor-123\",\"phoneNumber\":\"" + phone + "\",\"role\":\"DOCTOR\"}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private long firstFreeSlot(String patientToken, String doctorUsername) throws Exception {
        JsonNode slots = json.readTree(call(HttpMethod.GET, "/api/slots/available/" + findDoctorId(doctorUsername)
                + "?date=" + LocalDate.now().plusDays(1) + "&shift=MORNING", patientToken, null).getBody());
        return slots.get(0).get("id").asLong();
    }

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
