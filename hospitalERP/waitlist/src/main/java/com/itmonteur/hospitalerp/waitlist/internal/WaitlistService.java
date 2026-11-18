package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.patients.PtRelative;
import com.itmonteur.hospitalerp.patients.PtRelativeService;
import com.itmonteur.hospitalerp.scheduling.Shift;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * The waitlist's rules (docs/WAITLIST_PLAN.md): a patient - or one of their family members - waits for a doctor on a
 * day that has no free time left, in one shift or either; at most {@value #MAX_WAITING} waits per account. Joined by
 * the patient in the app or by the front desk; left by the patient, removed by the desk; ended by a booking with the
 * doctor that day or a deactivated account.
 */
@Service
public class WaitlistService {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistService.class);
    public static final int MAX_WAITING = 5;

    private final WaitlistRepository repository;
    private final PtInfoService ptInfoService;
    private final PtRelativeService ptRelativeService;
    private final DoctorService doctorService;
    private final SlotService slotService;
    private final LeaveRequestService leaveRequestService;
    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public WaitlistService(WaitlistRepository repository, PtInfoService ptInfoService, PtRelativeService ptRelativeService,
                           DoctorService doctorService, SlotService slotService, LeaveRequestService leaveRequestService,
                           AppointmentService appointmentService, CurrentUserService currentUserService, Clock clock) {
        this.repository = repository;
        this.ptInfoService = ptInfoService;
        this.ptRelativeService = ptRelativeService;
        this.doctorService = doctorService;
        this.slotService = slotService;
        this.leaveRequestService = leaveRequestService;
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- the patient

    /** The logged-in patient joins, for themself or one of their family members ({@code patientId} is ignored). */
    public WaitlistEntryDTO joinAsPatient(JoinRequest request) {
        return join(currentPatient(), request);
    }

    /** The logged-in patient's waits - their own and their family members' - from today on, soonest first. */
    @Transactional(readOnly = true)
    public List<WaitlistEntryDTO> mine() {
        return repository.findWaitingForPatient(currentPatient().getPatientId(), today()).stream()
                .map(WaitlistEntryDTO::of).toList();
    }

    /** The patient leaves one of their waits; another account's is "not found". */
    @Transactional
    public void leave(long entryId) {
        Long me = currentPatient().getPatientId();
        WaitlistEntry entry = repository.findById(entryId)
                .filter(e -> Objects.equals(e.getPatient().getPatientId(), me))
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry", "id", entryId));
        delete(entry);
    }

    // ---------------------------------------------------------------- the front desk (access is checked on its URL)

    /** The desk puts a patient (any record, also a walk-in's) or one of their family members on a waitlist. */
    public WaitlistEntryDTO joinForPatient(JoinRequest request) {
        if (request.patientId() == null) {
            throw new BadRequestException("Please choose the patient");
        }
        PtInfo patient = ptInfoService.findPatientEntity(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        return join(patient, request);
    }

    /** A day's waits (all doctors, or one), by doctor and in the order they were joined - also the ended ones. */
    @Transactional(readOnly = true)
    public List<WaitlistEntryDTO> forDay(LocalDate date, Long doctorId) {
        return repository.findForDay(date == null ? today() : date, doctorId).stream().map(WaitlistEntryDTO::of).toList();
    }

    @Transactional
    public void remove(long entryId) {
        delete(repository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry", "id", entryId)));
    }

    // ---------------------------------------------------------------- ending (listeners, in the change's transaction)

    /** A booking with the doctor that day ends the person's wait for it. */
    @Transactional
    public void closeBooked(Appointment appointment) {
        if (appointment.getPtInfo() == null || appointment.getDoctor() == null || appointment.getDate() == null) {
            return;
        }
        repository.findWaiting(appointment.getPtInfo().getPatientId(),
                        appointment.getRelative() == null ? null : appointment.getRelative().getId(),
                        appointment.getDoctor().getId(), appointment.getDate())
                .ifPresent(entry -> entry.end(WaitlistStatus.BOOKED, now()));
    }

    /** A deactivated patient's waits end, and everyone's waits for a deactivated doctor. */
    @Transactional
    public void endForAccount(Long userId, Role role) {
        List<WaitlistEntry> ending = switch (role) {
            case PATIENT -> ptInfoService.findPatientEntityByUserId(userId)
                    .map(p -> repository.findWaitingForPatient(p.getPatientId(), today())).orElse(List.of());
            case DOCTOR -> doctorService.findDoctorEntityByUserId(userId)
                    .map(d -> repository.findWaitingForDoctor(d.getId(), today())).orElse(List.of());
            default -> List.of();
        };
        ending.forEach(entry -> entry.end(WaitlistStatus.ENDED, now()));
        if (!ending.isEmpty()) {
            logger.info("Ended {} waits of deactivated user {} ({})", ending.size(), userId, role);
        }
    }

    // ---------------------------------------------------------------- the rules

    // Not in one transaction: SlotService makes a day's slots on first sight in its own (see generateSlots)
    private WaitlistEntryDTO join(PtInfo patient, JoinRequest request) {
        if (request.doctorId() == null || request.date() == null) {
            throw new BadRequestException("Please choose a doctor and a day");
        }
        PtRelative relative = request.relativeId() == null ? null : ptRelativeService.findRelativeEntity(request.relativeId())
                .filter(r -> r.getPtInfo() != null && Objects.equals(r.getPtInfo().getPatientId(), patient.getPatientId()))
                .orElseThrow(() -> new ResourceNotFoundException("Relative", "id", request.relativeId()));
        Doctor doctor = doctorService.findDoctorEntity(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.doctorId()));
        LocalDate date = request.date();
        String dr = "Dr. " + doctor.getName();
        if (date.isBefore(today()) || date.isAfter(today().plusDays(SlotService.BOOKING_WINDOW_DAYS - 1L))) {
            throw new BadRequestException("Choose a day from today up to " + SlotService.BOOKING_WINDOW_DAYS + " days ahead");
        }
        if (!doctor.isAccountActive()) {
            throw new BadRequestException(dr + " isn't seeing patients");
        }
        if (doctor.getUser() != null && leaveRequestService.isOnApprovedLeave(doctor.getUser().getId(), date)) {
            throw new BadRequestException(dr + " is away that day");
        }
        String name = relative != null ? relative.getName() : patient.getPatientName();
        Long relativeId = relative == null ? null : relative.getId();
        if (appointmentService.hasBookingWith(patient.getPatientId(), relativeId, doctor.getId(), date)) {
            throw new ConflictException(name + " already has an appointment with " + dr + " that day");
        }
        // Only a full shift: one with a time ahead that may free up, and no free time now
        List<Shift> shifts = request.shift() == null ? List.of(Shift.values()) : List.of(request.shift());
        boolean timesAhead = false;
        for (Shift shift : shifts) {
            if (!slotService.getAvailableSlots(doctor.getId(), date, shift).isEmpty()) {
                throw new ConflictException("There are free times then. Please book one.");
            }
            timesAhead |= slotService.hasTimesAhead(doctor.getId(), date, shift);
        }
        if (!timesAhead) {
            throw new BadRequestException(dr + " has no times then" + (date.equals(today()) ? " any more today" : ""));
        }

        WaitlistEntry existing = repository.findWaiting(patient.getPatientId(), relativeId, doctor.getId(), date).orElse(null);
        if (existing != null) { // joined again: the shift changes
            existing.setShift(request.shift());
            return WaitlistEntryDTO.of(repository.save(existing));
        }
        if (repository.findWaitingForPatient(patient.getPatientId(), today()).size() >= MAX_WAITING) {
            throw new ConflictException("At most " + MAX_WAITING + " waits at a time for one account. Please leave one first.");
        }
        WaitlistEntry saved = repository.save(new WaitlistEntry(patient, relative, doctor, date, request.shift(),
                currentUserService.getCurrentUser().getUsername(), now()));
        logger.info("Waitlist entry {}: patient {} for doctor {} on {}", saved.getId(), patient.getPatientId(), doctor.getId(), date);
        return WaitlistEntryDTO.of(saved);
    }

    private void delete(WaitlistEntry entry) {
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new ConflictException("This wait has already ended");
        }
        repository.delete(entry);
    }

    private PtInfo currentPatient() {
        Long userId = currentUserService.getCurrentUserId();
        return ptInfoService.findPatientEntityByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
