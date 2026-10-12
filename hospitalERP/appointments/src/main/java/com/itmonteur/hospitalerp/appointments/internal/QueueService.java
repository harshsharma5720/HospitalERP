package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentCheckedInEvent;
import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.AppointmentLeftQueueEvent;
import com.itmonteur.hospitalerp.appointments.AppointmentStatus;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ForbiddenException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.Specialist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Today's queue (docs/TODAYS_QUEUE_PLAN.md). Check-in: the patient has arrived and gets a token - the appointment's
 * place in the doctor's day (its slot among all of the doctor's slots that day, in time order), kept from then on.
 * Should that number already be someone else's that day (the day's slots changed after a schedule change), the next
 * free number is used, so no two patients of a doctor share a token. The doctor calls the waiting patients in by
 * appointment time; the waiting-room screen shows the token being served and the next ones, nothing else.
 */
@Service
public class QueueService {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private static final int SCREEN_NEXT = 5;
    // By appointment time; a visit without its slot (old data) last
    private static final Comparator<Appointment> BY_TIME = Comparator
            .comparing((Appointment a) -> a.getSlot() == null ? null : a.getSlot().getStartTime(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Appointment::getAppointmentID);

    private final AppointmentRepository appointmentRepository;
    private final SlotService slotService;
    private final CurrentUserService currentUserService;
    private final DoctorService doctorService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public QueueService(AppointmentRepository appointmentRepository, SlotService slotService,
                        CurrentUserService currentUserService, DoctorService doctorService,
                        ApplicationEventPublisher eventPublisher, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.slotService = slotService;
        this.currentUserService = currentUserService;
        this.doctorService = doctorService;
        this.eventPublisher = eventPublisher;
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
            eventPublisher.publishEvent(new AppointmentCheckedInEvent(appointmentId));
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
        boolean wasCheckedIn = appointment.getCheckedInAt() != null;
        appointment.clearQueueState();
        Appointment saved = appointmentRepository.save(appointment);
        if (wasCheckedIn) {
            eventPublisher.publishEvent(new AppointmentLeftQueueEvent(appointmentId));
        }
        return AppointmentMapper.toDTO(saved);
    }

    // ---------------------------------------------------------------- the doctor

    /** The logged-in doctor's queue today. */
    @Transactional(readOnly = true)
    public DoctorQueueDTO myQueue() {
        LocalDate today = LocalDate.now(clock);
        List<Appointment> day = doctorsDay(currentDoctor(), today);
        List<Appointment> withDoctor = day.stream().filter(a -> a.isActive() && a.getCalledAt() != null)
                .sorted(Comparator.comparing(Appointment::getCalledAt).reversed()).toList();
        return new DoctorQueueDTO(today, toDTOs(withDoctor), toDTOs(waiting(day)),
                toDTOs(day.stream().filter(a -> a.isActive() && a.getCheckedInAt() == null).toList()),
                toDTOs(day.stream().filter(QueueService::isDone).toList()));
    }

    /** Calls in the first waiting patient by appointment time. */
    @Transactional
    public AppointmentDTO callNext() {
        return waiting(doctorsDay(currentDoctor(), LocalDate.now(clock))).stream().findFirst()
                .map(this::callIn)
                .orElseThrow(() -> new ConflictException("Nobody is waiting"));
    }

    /** Calls in this waiting patient (e.g. an urgent one); calling someone already called in changes nothing. */
    @Transactional
    public AppointmentDTO call(long appointmentId) {
        Appointment appointment = ownTodaysActive(appointmentId);
        if (appointment.getCheckedInAt() == null) {
            throw new BadRequestException("This patient has not checked in yet");
        }
        return appointment.getCalledAt() != null ? AppointmentMapper.toDTO(appointment) : callIn(appointment);
    }

    /** The patient called in did not come: back to waiting, with the same token and place. */
    @Transactional
    public AppointmentDTO notHere(long appointmentId) {
        Appointment appointment = ownTodaysActive(appointmentId);
        if (appointment.getCalledAt() == null) {
            throw new BadRequestException("This patient has not been called in");
        }
        appointment.setCalledAt(null);
        return AppointmentMapper.toDTO(appointmentRepository.save(appointment));
    }

    // ---------------------------------------------------------------- the waiting-room screen

    /** Per doctor with anyone checked in today: the token being served and the next ones. No patient data. */
    @Transactional(readOnly = true)
    public QueueScreenDTO screen() {
        LocalDate today = LocalDate.now(clock);
        Map<Doctor, List<Appointment>> byDoctor = new LinkedHashMap<>();
        appointmentRepository.findCheckedInOn(today).stream()
                .filter(a -> a.getDoctor() != null)
                .sorted(BY_TIME)
                .forEach(a -> byDoctor.computeIfAbsent(a.getDoctor(), d -> new ArrayList<>()).add(a));
        List<QueueScreenDTO.ScreenDoctor> doctors = byDoctor.entrySet().stream()
                .map(entry -> new QueueScreenDTO.ScreenDoctor(
                        displayName(entry.getKey()),
                        specialist(entry.getKey()),
                        entry.getValue().stream().filter(a -> a.getCalledAt() != null)
                                .max(Comparator.comparing(Appointment::getCalledAt))
                                .map(Appointment::getTokenNumber).orElse(null),
                        waiting(entry.getValue()).stream().limit(SCREEN_NEXT).map(Appointment::getTokenNumber).toList()))
                .sorted(Comparator.comparing(QueueScreenDTO.ScreenDoctor::doctorName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new QueueScreenDTO(today, doctors);
    }

    // ---------------------------------------------------------------- helpers

    private List<Appointment> doctorsDay(Doctor doctor, LocalDate date) {
        return appointmentRepository.findByDoctor_IdAndDate(doctor.getId(), date).stream().sorted(BY_TIME).toList();
    }

    // Checked in, not called in yet, still booked - in the order of the given list
    private static List<Appointment> waiting(List<Appointment> appointments) {
        return appointments.stream()
                .filter(a -> a.isActive() && a.getCheckedInAt() != null && a.getCalledAt() == null)
                .toList();
    }

    private static boolean isDone(Appointment appointment) {
        return appointment.isCompleted() || appointment.getStatus() == AppointmentStatus.COMPLETED;
    }

    private AppointmentDTO callIn(Appointment appointment) {
        appointment.setCalledAt(callTime(appointment));
        appointmentRepository.save(appointment);
        logger.info("Appointment {} called in, token {}", appointment.getAppointmentID(), appointment.getTokenNumber());
        return AppointmentMapper.toDTO(appointment);
    }

    // Later than every other call of the doctor's day: the system clock can be coarse, and two calls in one tick
    // would leave "the last one called in" (the screen's now serving) undecided
    private LocalDateTime callTime(Appointment appointment) {
        LocalDateTime now = now();
        LocalDateTime latest = doctorsDay(appointment.getDoctor(), appointment.getDate()).stream()
                .map(Appointment::getCalledAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return latest != null && !now.isAfter(latest) ? latest.plus(1, ChronoUnit.MICROS) : now;
    }

    private Doctor currentDoctor() {
        if (!currentUserService.hasRole(Role.DOCTOR)) {
            throw new ForbiddenException("Only doctors have a queue");
        }
        return doctorService.findDoctorEntityByUserId(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ForbiddenException("Only doctors have a queue"));
    }

    // One of the logged-in doctor's appointments today, still booked
    private Appointment ownTodaysActive(long appointmentId) {
        Doctor doctor = currentDoctor();
        Appointment appointment = findAppointment(appointmentId);
        if (appointment.getDoctor() == null || !Objects.equals(appointment.getDoctor().getId(), doctor.getId())) {
            throw new ForbiddenException("This is another doctor's patient");
        }
        if (!LocalDate.now(clock).equals(appointment.getDate()) || !appointment.isActive()) {
            throw new BadRequestException("Only today's booked appointments are in the queue");
        }
        return appointment;
    }

    private static String displayName(Doctor doctor) {
        return doctor.getName() != null && !doctor.getName().isBlank() ? doctor.getName() : doctor.getUserName();
    }

    private static String specialist(Doctor doctor) {
        Specialist specialist = doctor.getSpecialist();
        return specialist == null || specialist == Specialist.NOT_ASSIGNED ? null : specialist.name();
    }

    private static List<AppointmentDTO> toDTOs(List<Appointment> appointments) {
        return appointments.stream().map(AppointmentMapper::toDTO).toList();
    }

    // To the microsecond, as the database keeps it: the answer shows the same time as later reads (the clock can be
    // more precise), and two patients called in within the same second are still in order
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
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
