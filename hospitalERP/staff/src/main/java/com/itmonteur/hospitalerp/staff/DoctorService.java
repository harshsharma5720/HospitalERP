package com.itmonteur.hospitalerp.staff;

import java.util.Optional;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.staff.internal.DoctorRepository;
import com.itmonteur.hospitalerp.common.FileStorageService;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
public class DoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public DoctorService(DoctorRepository doctorRepository,
                         UserService userService, CurrentUserService currentUserService,
                         FileStorageService fileStorageService) {
        this.doctorRepository = doctorRepository;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    // Get all doctors (public directory). Deactivated doctors are listed for admins only (Manage Doctors).
    public List<DoctorDTO> getAllDoctors() {
        return listed(doctorRepository.findAll());
    }

    // Public doctor profile; a deactivated doctor is "not found" except for admins
    public DoctorDTO getDoctorByDoctorId(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(d -> d.isAccountActive() || currentUserService.hasRole(Role.ADMIN))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        return DoctorMapper.toDTO(doctor);
    }

    private List<DoctorDTO> listed(List<Doctor> doctors) {
        boolean admin = currentUserService.hasRole(Role.ADMIN);
        return doctors.stream()
                .filter(doctor -> admin || doctor.isAccountActive())
                .map(DoctorMapper::toDTO)
                .toList();
    }

    // Get doctor by user ID (the doctor themself or an admin)
    public DoctorDTO getDoctorByUserId(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return DoctorMapper.toDTO(doctorByUserId(userId));
    }

    public List<DoctorDTO> findDoctorsBySpecialization(Specialist specialization) {
        return listed(doctorRepository.findBySpecialist(specialization).orElse(List.of()));
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
            userService.updateContactDetails(user, doctor.getEmail(), doctor.getPhoneNumber());
        }
        Doctor updatedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor profile updated for user {}", userId);
        return DoctorMapper.toDTO(updatedDoctor);
    }

    private Doctor doctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
    }

    // ------------------------------------------------------------------ module API
    // (used by other modules instead of DoctorRepository)

    public Optional<Doctor> findDoctorEntity(Long doctorId) {
        return doctorRepository.findById(doctorId);
    }

    public Optional<Doctor> findDoctorEntityByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    /** Deletes the doctor row. Callers remove bookings, slots and schedule first. */
    @Transactional
    public void deleteDoctorEntity(Long doctorId) {
        doctorRepository.deleteById(doctorId);
    }
}
