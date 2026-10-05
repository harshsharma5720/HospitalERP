package com.itmonteur.hospitalerp.administration.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.staff.Receptionist;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ConflictException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.appointments.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import com.itmonteur.hospitalerp.staff.ReceptionistService;
import com.itmonteur.hospitalerp.scheduling.DoctorScheduleService;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.clinical.ConsultationService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Account lifecycle (docs/ACCOUNT_DEACTIVATION_PLAN.md).
 *
 * <p><b>Deactivate</b> (the normal way, also what the old "delete" endpoints do now): no login any
 * more, hidden from directories and booking, upcoming appointments cancelled with a notification —
 * and all history (appointments, consultations, prescriptions, leaves) kept. <b>Reactivate</b> undoes it.
 *
 * <p><b>Delete permanently</b>: only for accounts without any appointment (e.g. created by mistake),
 * because medical records must be kept. It removes everything that references the user, in an order
 * the foreign keys allow (leaves → consultations → appointments → slots/schedule → profile → user).
 *
 * <p>Every change of an account goes into the audit log (docs/AUDIT_LOG_PLAN.md): created, deactivated,
 * reactivated, deleted. A call that changes nothing (e.g. deactivating twice) records nothing.
 */
@Service
public class UserAccountService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountService.class);

    private final UserService userService;
    private final PtInfoService ptInfoService;
    private final DoctorService doctorService;
    private final ReceptionistService receptionistService;
    private final LeaveRequestService leaveRequestService;
    private final AppointmentService appointmentService;
    private final ConsultationService consultationService;
    private final SlotService slotService;
    private final DoctorScheduleService doctorScheduleService;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserService currentUserService;
    private final AuditLog auditLog;
    private final EntityManager entityManager;

    // Only other modules' services are used, never their repositories (plan rule 2)
    public UserAccountService(UserService userService, PtInfoService ptInfoService, DoctorService doctorService,
                              ReceptionistService receptionistService, LeaveRequestService leaveRequestService,
                              AppointmentService appointmentService, ConsultationService consultationService,
                              SlotService slotService, DoctorScheduleService doctorScheduleService,
                              ApplicationEventPublisher eventPublisher, CurrentUserService currentUserService,
                              AuditLog auditLog, EntityManager entityManager) {
        this.userService = userService;
        this.ptInfoService = ptInfoService;
        this.doctorService = doctorService;
        this.receptionistService = receptionistService;
        this.leaveRequestService = leaveRequestService;
        this.appointmentService = appointmentService;
        this.consultationService = consultationService;
        this.slotService = slotService;
        this.doctorScheduleService = doctorScheduleService;
        this.eventPublisher = eventPublisher;
        this.currentUserService = currentUserService;
        this.auditLog = auditLog;
        this.entityManager = entityManager;
    }

    /** Called by the admin's "create user" use cases once the account (and its profile) exists. */
    public void recordCreated(User user) {
        audit(AuditAction.USER_CREATED, user, patientIdOf(user));
    }

    // ------------------------------------------------------------------ deactivate / reactivate

    @Transactional
    public User deactivate(Long userId) {
        User user = findUser(userId);
        requireNotOwnAdminAccount(user, "deactivate");
        if (!user.isActive()) {
            return user; // already deactivated
        }
        switch (user.getRole()) {
            case PATIENT -> ptInfoService.findPatientEntityByUserId(userId)
                    .ifPresent(patient -> appointmentService.cancelUpcomingForPatient(patient.getPatientId()));
            case DOCTOR -> doctorService.findDoctorEntityByUserId(userId)
                    .ifPresent(doctor -> appointmentService.cancelUpcomingForDoctor(doctor.getId()));
            default -> { } // receptionists and admins have no bookings
        }
        userService.deactivate(user);
        logger.info("Deactivated user {} ({})", userId, user.getRole());
        audit(AuditAction.ACCOUNT_DEACTIVATED, user, patientIdOf(user));
        return user;
    }

    @Transactional
    public User reactivate(Long userId) {
        User user = findUser(userId);
        if (!user.isActive()) {
            userService.reactivate(user);
            logger.info("Reactivated user {} ({})", userId, user.getRole());
            audit(AuditAction.ACCOUNT_REACTIVATED, user, patientIdOf(user));
        }
        return user;
    }

    // ------------------------------------------------------------------ permanent delete (no history only)

    @Transactional
    public void deletePermanently(Long userId) {
        User user = findUser(userId);
        requireNotOwnAdminAccount(user, "delete");
        if (hasHistory(user)) {
            throw new ConflictException("This account has appointments or medical records, which must be kept. "
                    + "Deactivate it instead.");
        }
        Long patientId = patientIdOf(user); // looked up before the profile is gone
        deleteUser(user);
        audit(AuditAction.ACCOUNT_DELETED, user, patientId);
    }

    private boolean hasHistory(User user) {
        Long userId = user.getId();
        return switch (user.getRole()) {
            case PATIENT -> ptInfoService.findPatientEntityByUserId(userId)
                    .map(patient -> appointmentService.hasAnyAppointmentForPatient(patient.getPatientId()))
                    .orElse(false);
            case DOCTOR -> doctorService.findDoctorEntityByUserId(userId)
                    .map(doctor -> appointmentService.hasAnyAppointmentForDoctor(doctor.getId()))
                    .orElse(false);
            default -> false; // receptionists and admins hold no medical records
        };
    }

    private Long patientIdOf(User user) {
        return user.getRole() == Role.PATIENT
                ? ptInfoService.findPatientEntityByUserId(user.getId()).map(PtInfo::getPatientId).orElse(null)
                : null;
    }

    // The target's username and role go into the details: they stay readable after a permanent delete
    private void audit(AuditAction action, User user, Long patientId) {
        auditLog.record(action, patientId, user.getId(), user.getUsername() + " (" + user.getRole() + ")");
    }

    private User findUser(Long userId) {
        return userService.findUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void requireNotOwnAdminAccount(User user, String action) {
        if (user.getRole() == Role.ADMIN && Objects.equals(currentUserService.getCurrentUserId(), user.getId())) {
            throw new BadRequestException("Admins cannot " + action + " their own account");
        }
    }

    private void deleteUser(User user) {
        Long userId = user.getId();
        leaveRequestService.deleteAllForUser(userId);

        switch (user.getRole()) {
            case PATIENT -> {
                ptInfoService.findPatientEntityByUserId(userId).ifPresent(this::deletePatientProfile);
                userService.deleteUserById(userId); // by id: the persistence context was cleared
            }
            case DOCTOR -> {
                // The profile goes first (it references the user), then the login account
                doctorService.findDoctorEntityByUserId(userId).ifPresent(this::deleteDoctorProfile);
                userService.deleteUserById(userId); // by id: the persistence context was cleared
            }
            case RECEPTIONIST -> {
                receptionistService.findReceptionistEntityByUserId(userId)
                        .ifPresent(receptionistService::deleteReceptionistEntity);
                userService.deleteUser(user);
            }
            default -> userService.deleteUser(user);
        }
        logger.info("Deleted user {} ({})", userId, user.getRole());
    }

    // ------------------------------------------------------------------ old "delete" endpoints → deactivate
    // (moved here from PtInfoService / DoctorService / ReceptionistService in step 1.7; URLs unchanged)

    /** DELETE /api/patient/deleteAccount/{userId}: the patient themself, or an admin. Only an admin can reactivate. */
    @Transactional
    public void deactivatePatientAccount(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        deactivate(userId);
    }

    /** DELETE /api/doctor/delete/{doctorId} (admin): doctor.id, not the user id. */
    @Transactional
    public void deactivateDoctorByDoctorId(Long doctorId) {
        Doctor doctor = doctorService.findDoctorEntity(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        if (doctor.getUser() != null) {
            deactivate(doctor.getUser().getId());
        } else if (appointmentService.hasAnyAppointmentForDoctor(doctorId)) {
            // legacy doctor row without a login: nothing to deactivate, and its history must stay
            throw new ConflictException("This doctor has appointments and no login account to deactivate.");
        } else {
            deleteDoctorProfile(doctor);
            auditLog.record(AuditAction.ACCOUNT_DELETED, null, null, "doctor profile " + doctorId + " (no login account)");
        }
    }

    /** DELETE /api/receptionist/delete/{receptionistId} (admin): receptionist.id, not the user id. */
    @Transactional
    public void deactivateReceptionistByReceptionistId(Long receptionistId) {
        Receptionist receptionist = receptionistService.findReceptionistEntity(receptionistId)
                .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistId));
        if (receptionist.getUser() != null) {
            deactivate(receptionist.getUser().getId());
        } else {
            receptionistService.deleteReceptionistEntity(receptionist); // legacy row without a login, no records
            auditLog.record(AuditAction.ACCOUNT_DELETED, null, null,
                    "receptionist profile " + receptionistId + " (no login account)");
        }
    }

    private void deletePatientProfile(PtInfo patient) {
        Long patientId = patient.getPatientId();
        List<Appointment> upcoming = appointmentService.findUpcomingForPatient(patientId);
        upcoming.forEach(a -> slotService.releaseSlot(a.getSlot()));
        writeAndForgetLoadedEntities();
        consultationService.deleteAllForPatient(patientId);
        appointmentService.deleteAllForPatient(patientId);
        ptInfoService.deletePatientEntity(patientId); // relatives are removed by cascade
    }

    /** Also used for legacy doctor rows that have no linked user. */
    @Transactional
    public void deleteDoctorProfile(Doctor doctor) {
        Long doctorId = doctor.getId();
        List<Appointment> upcoming = appointmentService.findUpcomingForDoctor(doctorId);
        upcoming.forEach(a -> eventPublisher.publishEvent(new AppointmentNotificationEvent(
                AppointmentNotificationEvent.Kind.CANCELLED, AppointmentService.notificationInfo(a))));
        writeAndForgetLoadedEntities();
        consultationService.deleteAllForDoctor(doctorId);
        appointmentService.deleteAllForDoctor(doctorId);
        slotService.deleteAllForDoctor(doctorId);
        doctorScheduleService.deleteAllForDoctor(doctorId);
        doctorService.deleteDoctorEntity(doctorId);
    }

    /**
     * The bulk deletes below bypass Hibernate's in-memory state. Appointments and slots loaded
     * above would otherwise stay "managed" while pointing at rows that no longer exist, and the
     * final flush fails ("references an unsaved transient instance"). So: write pending changes
     * (e.g. released slots) now, then detach everything; later deletes work by id.
     */
    private void writeAndForgetLoadedEntities() {
        entityManager.flush();
        entityManager.clear();
    }
}
