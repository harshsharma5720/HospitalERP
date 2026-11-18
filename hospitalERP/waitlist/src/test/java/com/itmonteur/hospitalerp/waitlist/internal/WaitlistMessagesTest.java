package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.inbox.Inbox;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import com.itmonteur.hospitalerp.notifications.NotificationOutbox;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.scheduling.Shift;
import com.itmonteur.hospitalerp.scheduling.SlotFreedEvent;
import com.itmonteur.hospitalerp.staff.Doctor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The waitlist's messages with the hospital's phone set (docs/WAITLIST_PLAN.md, WL.2); the rest end to end. */
class WaitlistMessagesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20); // a Friday
    private static final SlotFreedEvent TEN = new SlotFreedEvent(5L, 3L, DAY, Shift.MORNING, LocalTime.of(10, 0), LocalTime.of(10, 30));

    private Inbox inbox;
    private NotificationOutbox outbox;
    private WaitlistMessages messages;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        inbox = mock(Inbox.class);
        outbox = mock(NotificationOutbox.class);
        messages = new WaitlistMessages(inbox, outbox, "Shreya Hospital", "+91 120 555 1234");
        doctor = new Doctor();
        doctor.setId(3L);
        doctor.setName("Anita Rao");
    }

    private WaitlistEntry waiting(String name, String phone, Long userId) {
        PtInfo patient = new PtInfo();
        patient.setPatientName(name);
        patient.setContactNo(phone);
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            patient.setUser(user);
        }
        return new WaitlistEntry(patient, null, doctor, DAY, null, "desk", LocalDateTime.of(2026, 11, 18, 9, 0));
    }

    @Test
    void anAppAccountIsToldToBookInTheAppOrCall() {
        messages.timeFree(waiting("Asha Rao", "+919800000001", 41L), TEN);

        verify(inbox).toUser(41L, InboxKind.WAITLIST_TIME_FREE, "A time is free: Dr. Anita Rao",
                "Fri 20 Nov, 10:00–10:30, for Asha Rao. First come, first served: book it now.",
                "/appointments?doctorId=3&date=2026-11-20&shift=MORNING");
        verify(outbox).queueSms("+919800000001", "Shreya Hospital: a time with Dr. Anita Rao is free on Fri 20 Nov at 10:00"
                        + " (waitlist for Asha Rao). First come, first served: book it in the app or call +91 120 555 1234.",
                "waitlist: a time free", LocalDateTime.of(DAY, LocalTime.of(10, 0)));
    }

    @Test
    void aRecordWithoutALoginIsToldToCallAndHasNoBell() {
        messages.timeFree(waiting("Walk In", "9000081002", null), TEN);

        verify(inbox).toUser(isNull(), eq(InboxKind.WAITLIST_TIME_FREE), anyString(), anyString(), anyString());
        verify(outbox).queueSms(eq("9000081002"), eq("Shreya Hospital: a time with Dr. Anita Rao is free on Fri 20 Nov at 10:00"
                + " (waitlist for Walk In). First come, first served: call +91 120 555 1234."), anyString(), any());
    }

    @Test
    void noPhoneNoSms() {
        messages.timeFree(waiting("No Phone", " ", 42L), TEN);
        messages.ended(waiting("No Phone", null, 42L), "is away on Fri 20 Nov");

        verify(outbox, never()).queueSms(any(), any(), any(), any());
        verify(inbox).toUser(eq(42L), eq(InboxKind.WAITLIST_ENDED), eq("Waitlist ended: Dr. Anita Rao"),
                eq("Dr. Anita Rao is away on Fri 20 Nov, so the wait for Fri 20 Nov (No Phone) has ended."
                        + " Please choose another day or doctor."), eq("/appointments"));
    }

    @Test
    void anEndedWaitsSmsIsNotSentAfterItsDay() {
        messages.ended(waiting("Asha Rao", "+919800000001", 41L), "isn't seeing patients any more");

        verify(outbox).queueSms("+919800000001", "Shreya Hospital: Dr. Anita Rao isn't seeing patients any more, so the wait"
                        + " for Fri 20 Nov (Asha Rao) has ended. Please choose another day or doctor.", "waitlist: ended",
                DAY.atTime(LocalTime.MAX));
    }
}
