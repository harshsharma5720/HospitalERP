package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.*;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final UserRepository userRepository;
    private final PtInfoRepository ptInfoRepository;
    private final DoctorRepository doctorRepository;
    private final ReceptionistRepository receptionistRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final SlotService slotService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public UserAccountService(UserRepository userRepository, PtInfoRepository ptInfoRepository,
                              DoctorRepository doctorRepository, ReceptionistRepository receptionistRepository,
                              AppointmentRepository appointmentRepository, SlotRepository slotRepository,
                              LeaveRequestRepository leaveRequestRepository,
                              ConsultationRepository consultationRepository,
                              DoctorScheduleRepository doctorScheduleRepository, SlotService slotService,
                              NotificationService notificationService, CurrentUserService currentUserService,
                              EntityManager entityManager) {
        this.userRepository = userRepository;
        this.ptInfoRepository = ptInfoRepository;
        this.doctorRepository = doctorRepository;
        this.receptionistRepository = receptionistRepository;
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.consultationRepository = consultationRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.slotService = slotService;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() == ITmonteur.example.hospitalERP.entities.Role.ADMIN
                && Objects.equals(currentUserService.getCurrentUserId(), userId)) {
            throw new BadRequestException("Admins cannot delete their own account");
        }
        leaveRequestRepository.deleteByUserId(userId);

        switch (user.getRole()) {
            case PATIENT -> {
                ptInfoRepository.findByUser_Id(userId).ifPresent(this::deletePatientProfile);
                userRepository.deleteById(userId); // by id: the persistence context was cleared
            }
            case DOCTOR -> {
                // The profile goes first (it references the user), then the login account
                doctorRepository.findByUserId(userId).ifPresent(this::deleteDoctorProfile);
                userRepository.deleteById(userId); // by id: the persistence context was cleared
            }
            case RECEPTIONIST -> {
                receptionistRepository.findByUser_Id(userId).ifPresent(receptionistRepository::delete);
                userRepository.delete(user);
            }
            default -> userRepository.delete(user);
        }
        logger.info("Deleted user {} ({})", userId, user.getRole());
    }

    private void deletePatientProfile(PtInfo patient) {
        Long patientId = patient.getPatientId();
        List<Appointment> upcoming = appointmentRepository.findPendingByPatientId(patientId);
        upcoming.forEach(a -> slotService.releaseSlot(a.getSlot()));
        writeAndForgetLoadedEntities();
        consultationRepository.deleteItemsByPatientId(patientId);
        consultationRepository.deleteByPatientId(patientId);
        appointmentRepository.deleteByPatientId(patientId);
        ptInfoRepository.deleteById(patientId); // relatives are removed by cascade
    }

    /** Also used for legacy doctor rows that have no linked user. */
    @Transactional
    public void deleteDoctorProfile(Doctor doctor) {
        Long doctorId = doctor.getId();
        List<Appointment> upcoming = appointmentRepository.findPendingByDoctorId(doctorId);
        upcoming.forEach(a -> notificationService.appointmentCancelled(AppointmentService.notificationInfo(a)));
        writeAndForgetLoadedEntities();
        consultationRepository.deleteItemsByDoctorId(doctorId);
        consultationRepository.deleteByDoctorId(doctorId);
        appointmentRepository.deleteByDoctorId(doctorId);
        slotRepository.deleteByDoctorId(doctorId);
        doctorScheduleRepository.deleteByDoctorId(doctorId);
        doctorRepository.deleteById(doctorId);
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
