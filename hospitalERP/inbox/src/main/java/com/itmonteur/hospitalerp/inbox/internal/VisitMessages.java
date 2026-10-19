package com.itmonteur.hospitalerp.inbox.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.inbox.Inbox;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import com.itmonteur.hospitalerp.notifications.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Visits in the notification centre (docs/NOTIFICATION_CENTRE_PLAN.md): booked, moved, cancelled, cancelled by the
 * doctor's leave, and the reminder - to the patient (the account holder) and, where it concerns them, the doctor.
 * Written in the transaction that changes the visit.
 */
@Component
public class VisitMessages {

    static final String PATIENT_PAGE = "/appointment-details";
    static final String DOCTOR_PAGE = "/doctor/doctor-appointments";

    private final Inbox inbox;

    public VisitMessages(Inbox inbox) {
        this.inbox = inbox;
    }

    @EventListener
    public void on(AppointmentNotificationEvent event) {
        if (event.who() == null) {
            return; // nobody to tell in the app
        }
        NotificationService.AppointmentInfo visit = event.info();
        Long patient = event.who().patientUserId();
        Long doctor = event.who().doctorUserId();
        String when = Texts.day(visit.date()) + ", " + Texts.time(visit.time());
        String withDoctor = "Dr. " + visit.doctorName() + ", " + when + ", for " + visit.patientName() + ".";
        String forDoctor = visit.patientName() + ", " + when + ".";
        switch (event.kind()) {
            case BOOKED -> {
                inbox.toUser(patient, InboxKind.VISIT_BOOKED, "Appointment booked", withDoctor, PATIENT_PAGE);
                inbox.toUser(doctor, InboxKind.VISIT_BOOKED, "New appointment", forDoctor, DOCTOR_PAGE);
            }
            case MOVED -> {
                inbox.toUser(patient, InboxKind.VISIT_MOVED, "Appointment moved", "Now " + withDoctor, PATIENT_PAGE);
                inbox.toUser(doctor, InboxKind.VISIT_MOVED, "Appointment moved", visit.patientName() + ": now " + when + ".",
                        DOCTOR_PAGE);
            }
            case CANCELLED -> {
                inbox.toUser(patient, InboxKind.VISIT_CANCELLED, "Appointment cancelled", withDoctor, PATIENT_PAGE);
                inbox.toUser(doctor, InboxKind.VISIT_CANCELLED, "Appointment cancelled", forDoctor, DOCTOR_PAGE);
            }
            case CANCELLED_BY_DOCTOR_LEAVE -> inbox.toUser(patient, InboxKind.VISIT_CANCELLED_BY_LEAVE,
                    "Appointment cancelled: the doctor is away", "Dr. " + visit.doctorName() + " is on leave on "
                            + Texts.day(visit.date()) + ". The visit at " + Texts.time(visit.time()) + " for "
                            + visit.patientName() + " is cancelled; please book another time.", "/appointments");
            case REMINDER -> inbox.toUser(patient, InboxKind.VISIT_REMINDER, "Your appointment is tomorrow", withDoctor,
                    PATIENT_PAGE);
        }
    }
}
