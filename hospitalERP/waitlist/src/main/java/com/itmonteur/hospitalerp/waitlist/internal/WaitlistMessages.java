package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.inbox.Inbox;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import com.itmonteur.hospitalerp.notifications.NotificationOutbox;
import com.itmonteur.hospitalerp.scheduling.SlotFreedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * What the waitlist tells people (docs/WAITLIST_PLAN.md): the bell for the account holder (nothing for a walk-in
 * record without a login) and an SMS to the patient record's phone. Written in the caller's transaction.
 */
@Component
public class WaitlistMessages {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final Inbox inbox;
    private final NotificationOutbox outbox;
    private final String hospitalName;
    private final String hospitalPhone;

    public WaitlistMessages(Inbox inbox, NotificationOutbox outbox,
                            @Value("${app.hospital.name:Hospital ERP}") String hospitalName,
                            @Value("${app.hospital.phone:}") String hospitalPhone) {
        this.inbox = inbox;
        this.outbox = outbox;
        this.hospitalName = hospitalName;
        this.hospitalPhone = hospitalPhone;
    }

    /** A time the person waits for is free; the bell opens the booking page on that doctor, day and shift. */
    void timeFree(WaitlistEntry entry, SlotFreedEvent time) {
        String doctor = "Dr. " + entry.getDoctor().getName();
        inbox.toUser(userId(entry), InboxKind.WAITLIST_TIME_FREE, "A time is free: " + doctor,
                DAY.format(time.date()) + ", " + TIME.format(time.startTime()) + "–" + TIME.format(time.endTime())
                        + ", for " + entry.personName() + ". First come, first served: book it now.",
                "/appointments?doctorId=" + entry.getDoctor().getId() + "&date=" + time.date() + "&shift=" + time.shift());
        sms(entry, hospitalName + ": a time with " + doctor + " is free on " + DAY.format(time.date()) + " at "
                        + TIME.format(time.startTime()) + " (waitlist for " + entry.personName() + "). First come, first served: "
                        + howToBook(entry) + ".",
                "waitlist: a time free", LocalDateTime.of(time.date(), time.startTime()));
    }

    /** The wait ended because the doctor won't see patients that day; {@code why}: "is away on Tue 20 Oct". */
    void ended(WaitlistEntry entry, String why) {
        String doctor = "Dr. " + entry.getDoctor().getName();
        String text = doctor + " " + why + ", so the wait for " + DAY.format(entry.getDate()) + " (" + entry.personName()
                + ") has ended. Please choose another day or doctor.";
        inbox.toUser(userId(entry), InboxKind.WAITLIST_ENDED, "Waitlist ended: " + doctor, text, "/appointments");
        sms(entry, hospitalName + ": " + text, "waitlist: ended", entry.getDate().atTime(LocalTime.MAX));
    }

    static String day(LocalDate date) {
        return DAY.format(date);
    }

    // In the app, or at the desk for a record without a login
    private String howToBook(WaitlistEntry entry) {
        String call = hospitalPhone.isBlank() ? null : "call " + hospitalPhone;
        if (entry.getPatient().getUser() == null) {
            return call != null ? call : "contact the front desk";
        }
        return "book it in the app" + (call != null ? " or " + call : "");
    }

    private void sms(WaitlistEntry entry, String text, String description, LocalDateTime notAfter) {
        String phone = entry.getPatient().getContactNo();
        if (phone != null && !phone.isBlank()) {
            outbox.queueSms(phone, text, description, notAfter);
        }
    }

    private static Long userId(WaitlistEntry entry) {
        return entry.getPatient().getUser() == null ? null : entry.getPatient().getUser().getId();
    }
}
