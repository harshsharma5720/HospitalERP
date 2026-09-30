package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Sends a reminder (email + SMS) the day before each upcoming appointment.
 * Runs every hour by default, so appointments booked later in the day for tomorrow are
 * still reminded; each appointment is reminded only once (reminderSent flag).
 */
@Service
public class AppointmentReminderService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentReminderService.class);

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final boolean enabled;

    public AppointmentReminderService(AppointmentRepository appointmentRepository,
                                      ApplicationEventPublisher eventPublisher,
                                      Clock clock,
                                      @Value("${app.reminders.enabled:true}") boolean enabled) {
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 * * * *}")
    @Transactional // a self-call would skip the annotation on sendDayBeforeReminders
    public void scheduledRun() {
        if (enabled) {
            sendDayBeforeReminders();
        }
    }

    /** Returns how many reminders were queued. */
    @Transactional
    public int sendDayBeforeReminders() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        List<Appointment> due = appointmentRepository.findDueForReminder(tomorrow);
        for (Appointment appointment : due) {
            eventPublisher.publishEvent(new AppointmentNotificationEvent(
                    AppointmentNotificationEvent.Kind.REMINDER, AppointmentService.notificationInfo(appointment)));
            appointment.setReminderSent(true);
        }
        appointmentRepository.saveAll(due);
        if (!due.isEmpty()) {
            logger.info("Queued {} appointment reminders for {}", due.size(), tomorrow);
        }
        return due.size();
    }
}
