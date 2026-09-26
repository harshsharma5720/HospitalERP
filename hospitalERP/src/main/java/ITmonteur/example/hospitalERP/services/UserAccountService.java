package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Appointment;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.staff.Receptionist;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.events.AppointmentNotificationEvent;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import com.itmonteur.hospitalerp.staff.ReceptionistService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Deletes a user together with everything that references it, in an order the
 * foreign keys allow (leaves → consultations → appointments → slots/schedule → profile → user).
 * Future appointments are cancelled with a notification before being removed.
 *
 * TODO: hospitals usually must keep records, so switch to soft delete (an "active" flag)
 *  once there is a migration tool to add the column safely.
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
    private final EntityManager entityManager;

    // Only other modules' services are used, never their repositories (plan rule 2)
    public UserAccountService(UserService userService, PtInfoService ptInfoService, DoctorService doctorService,
                              ReceptionistService receptionistService, LeaveRequestService leaveRequestService,
                              AppointmentService appointmentService, ConsultationService consultationService,
                              SlotService slotService, DoctorScheduleService doctorScheduleService,
                              ApplicationEventPublisher eventPublisher, CurrentUserService currentUserService,
                              EntityManager entityManager) {
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
        this.entityManager = entityManager;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userService.findUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() == Role.ADMIN
                && Objects.equals(currentUserService.getCurrentUserId(), userId)) {
            throw new BadRequestException("Admins cannot delete their own account");
        }
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

    // ------------------------------------------------------------------ entry points
    // (moved here from PtInfoService / DoctorService / ReceptionistService in step 1.7)

    /** DELETE /api/patient/deleteAccount/{userId}: the patient themself, or an admin. */
    @Transactional
    public void deletePatientAccount(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        deleteUser(userId);
    }

    /** DELETE /api/doctor/delete/{doctorId} (admin): doctor.id, not the user id. */
    @Transactional
    public void deleteDoctorByDoctorId(Long doctorId) {
        Doctor doctor = doctorService.findDoctorEntity(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        if (doctor.getUser() != null) {
            deleteUser(doctor.getUser().getId());
        } else {
            deleteDoctorProfile(doctor); // legacy doctor row without a login
        }
        logger.info("Doctor deleted with ID: {}", doctorId);
    }

    /** DELETE /api/receptionist/delete/{receptionistId} (admin): receptionist.id, not the user id. */
    @Transactional
    public void deleteReceptionistByReceptionistId(Long receptionistId) {
        Receptionist receptionist = receptionistService.findReceptionistEntity(receptionistId)
                .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistId));
        if (receptionist.getUser() != null) {
            deleteUser(receptionist.getUser().getId());
        } else {
            receptionistService.deleteReceptionistEntity(receptionist); // legacy row without a login
        }
        logger.info("Receptionist deleted with ID: {}", receptionistId);
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
