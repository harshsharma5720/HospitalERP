package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.DoctorMapper;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
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
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final UserAccountService userAccountService;

    public DoctorService(DoctorRepository doctorRepository,
                         UserRepository userRepository, CurrentUserService currentUserService,
                         FileStorageService fileStorageService, UserAccountService userAccountService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.userAccountService = userAccountService;
    }

    // Get all doctors (public directory)
    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findAll().stream().map(DoctorMapper::toDTO).toList();
    }

    public DoctorDTO getDoctorByDoctorId(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        return DoctorMapper.toDTO(doctor);
    }

    // Get doctor by user ID (the doctor themself or an admin)
    public DoctorDTO getDoctorByUserId(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return DoctorMapper.toDTO(doctorByUserId(userId));
    }

    public List<DoctorDTO> findDoctorsBySpecialization(Specialist specialization) {
        return doctorRepository.findBySpecialist(specialization)
                .orElse(List.of())
                .stream()
                .map(DoctorMapper::toDTO)
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
        return DoctorMapper.toDTO(updatedDoctor);
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

    private Doctor doctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
    }
}
