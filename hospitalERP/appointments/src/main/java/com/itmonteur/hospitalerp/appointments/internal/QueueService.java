package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * Today's queue (docs/TODAYS_QUEUE_PLAN.md). Check-in: the patient has arrived and gets a token - the appointment's
 * place in the doctor's day (its slot among all of the doctor's slots that day, in time order), kept from then on.
 * Should that number already be someone else's that day (the day's slots changed after a schedule change), the next
 * free number is used, so no two patients of a doctor share a token.
 */
@Service
public class QueueService {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private final AppointmentRepository appointmentRepository;
    private final SlotService slotService;
    private final Clock clock;

    public QueueService(AppointmentRepository appointmentRepository, SlotService slotService, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.slotService = slotService;
        this.clock = clock;
    }

    /** Checks in today's booked appointment. Checking in twice keeps the first token and time. */
    @Transactional
    public AppointmentDTO checkIn(long appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        if (!LocalDate.now(clock).equals(appointment.getDate())) {
            throw new BadRequestException("Only today's appointments can be checked in");
        }
        if (!appointment.isActive()) {
            throw new BadRequestException("This appointment is cancelled or already completed");
        }
        if (appointment.getCheckedInAt() == null) {
            if (appointment.getSlot() == null || appointment.getDoctor() == null) {
                throw new BadRequestException("This appointment has no time slot, so it has no place in the queue");
            }
            appointment.setTokenNumber(tokenFor(appointment));
            appointment.setCheckedInAt(now());
            appointmentRepository.save(appointment);
            logger.info("Appointment {} checked in, token {}", appointmentId, appointment.getTokenNumber());
        }
        return AppointmentMapper.toDTO(appointment);
    }

    /** Undoes a check-in made by mistake - not once the doctor has called the patient in. */
    @Transactional
    public AppointmentDTO undoCheckIn(long appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        if (appointment.getCalledAt() != null) {
            throw new ConflictException("The doctor has already called this patient in");
        }
        appointment.clearQueueState();
        return AppointmentMapper.toDTO(appointmentRepository.save(appointment));
    }

    // To the second - enough for a queue - so the answer shows the same time as later reads (the clock is more precise
    // than the database's microseconds)
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private int tokenFor(Appointment appointment) {
        int token = slotService.positionInDay(appointment.getSlot());
        List<Integer> givenOut = appointmentRepository.findTokensGivenOut(appointment.getDoctor().getId(),
                appointment.getDate(), appointment.getAppointmentID());
        return givenOut.contains(token) ? Collections.max(givenOut) + 1 : token;
    }

    private Appointment findAppointment(long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
    }
}
