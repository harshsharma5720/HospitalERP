package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.EntityMapper;
import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.AppointmentStatus;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final UserAccountService userAccountService;

    public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
                         UserRepository userRepository, CurrentUserService currentUserService,
                         FileStorageService fileStorageService, UserAccountService userAccountService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.userAccountService = userAccountService;
    }

    // Get all doctors (public directory)
    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findAll().stream().map(EntityMapper::toDoctorDTO).toList();
    }

    public DoctorDTO getDoctorByDoctorId(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        return EntityMapper.toDoctorDTO(doctor);
    }

    // Get doctor by user ID (the doctor themself or an admin)
    public DoctorDTO getDoctorByUserId(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return EntityMapper.toDoctorDTO(doctorByUserId(userId));
    }

    public List<DoctorDTO> findDoctorsBySpecialization(Specialist specialization) {
        return doctorRepository.findBySpecialist(specialization)
                .orElse(List.of())
                .stream()
                .map(EntityMapper::toDoctorDTO)
                .toList();
    }

    /** Parses a specialization name; unknown values return null so callers can answer with an empty list. */
    public static Specialist parseSpecialist(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Specialist.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Update doctor details (the doctor themself or an admin). Username cannot be changed here.
    @Transactional
    public DoctorDTO updateDoctor(Long userId, DoctorDTO doctorDTO, MultipartFile profileImage) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        Doctor doctor = doctorByUserId(userId);

        if (doctorDTO.getName() != null && !doctorDTO.getName().isBlank()) {
            doctor.setName(doctorDTO.getName().trim());
        }
        if (doctorDTO.getEmail() != null && !doctorDTO.getEmail().isBlank()) {
            doctor.setEmail(doctorDTO.getEmail().trim());
        }
        if (doctorDTO.getPhoneNumber() != null && !doctorDTO.getPhoneNumber().isBlank()) {
            doctor.setPhoneNumber(doctorDTO.getPhoneNumber().trim());
        }
        if (doctorDTO.getSpecialist() != null && !doctorDTO.getSpecialist().isBlank()) {
            Specialist specialist = parseSpecialist(doctorDTO.getSpecialist());
            if (specialist == null) {
                throw new BadRequestException("Unknown specialization: " + doctorDTO.getSpecialist());
            }
            doctor.setSpecialist(specialist);
        }
        if (profileImage != null && !profileImage.isEmpty()) {
            doctor.setProfileImage(fileStorageService.storeProfileImage(profileImage));
        }
        // Keep the login account's contact details in sync with the profile
        User user = doctor.getUser();
        if (user != null) {
            user.setEmail(doctor.getEmail());
            user.setPhoneNumber(doctor.getPhoneNumber());
            userRepository.save(user);
        }
        Doctor updatedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor profile updated for user {}", userId);
        return EntityMapper.toDoctorDTO(updatedDoctor);
    }

    // Delete doctor by doctor ID (admin only), including their appointments, slots and login
    @Transactional
    public boolean deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        if (doctor.getUser() != null) {
            userAccountService.deleteUser(doctor.getUser().getId());
        } else {
            userAccountService.deleteDoctorProfile(doctor);
        }
        logger.info("Doctor deleted with ID: {}", doctorId);
        return true;
    }

    // Only the doctor who owns the appointment (or an admin) can complete it
    @Transactional
    public String markAsCompleted(long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
        if (!currentUserService.hasRole(Role.ADMIN)) {
            Long userId = currentUserService.getCurrentUserId();
            boolean ownsAppointment = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                    && Objects.equals(appointment.getDoctor().getUser().getId(), userId);
            if (!ownsAppointment) {
                throw new ForbiddenException("You can only complete your own appointments");
            }
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.isCompleted()) {
            return "Appointment is already marked as completed.";
        }
        if (!appointment.isActive()) {
            throw new BadRequestException("A cancelled appointment cannot be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        return "Appointment marked as completed successfully.";
    }

    // userId = the doctor's user id; allowed for the doctor, admins and receptionists
    public List<AppointmentDTO> getAllPendingAppointmentsByDoctorId(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN, Role.RECEPTIONIST);
        Doctor doctor = doctorByUserId(userId);
        return appointmentRepository.findPendingByDoctorId(doctor.getId()).stream()
                .map(EntityMapper::toAppointmentDTO).toList();
    }

    public List<AppointmentDTO> getAllCompletedAppointmentsByDoctorId(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN, Role.RECEPTIONIST);
        Doctor doctor = doctorByUserId(userId);
        return appointmentRepository.findCompletedByDoctorId(doctor.getId()).stream()
                .map(EntityMapper::toAppointmentDTO).toList();
    }

    public long getPendingCount(Long doctorId) {
        return appointmentRepository.countPendingByDoctorId(doctorId);
    }

    public long getCompletedCount(Long doctorId) {
        return appointmentRepository.countCompletedByDoctorId(doctorId);
    }

    private Doctor doctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
    }
}
