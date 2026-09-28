package com.itmonteur.hospitalerp.staff;

import java.util.Optional;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.staff.internal.ReceptionistRepository;
import com.itmonteur.hospitalerp.common.FileStorageService;
import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.identity.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

// Receptionist profiles. Appointment handling for the front desk is delegated to AppointmentService.
@Service
public class ReceptionistService {

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistService.class);

    private final ReceptionistRepository receptionistRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public ReceptionistService(ReceptionistRepository receptionistRepository, UserService userService,
                               ModelMapper modelMapper, CurrentUserService currentUserService,
                               FileStorageService fileStorageService) {
        this.receptionistRepository = receptionistRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    // By user ID (the receptionist themself or an admin)
    public ReceptionistDTO getReceptionistByID(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return this.convertToDTO(receptionistByUserId(userId));
    }

    public List<ReceptionistDTO> getAllReceptionist() {
        return receptionistRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    // By user ID (self or admin). Username cannot be changed here.
    @Transactional
    public ReceptionistDTO updateReceptionist(Long userId, ReceptionistDTO dto, MultipartFile profileImage) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        Receptionist receptionist = receptionistByUserId(userId);

        if (dto.getName() != null && !dto.getName().isBlank()) receptionist.setName(dto.getName().trim());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) receptionist.setEmail(dto.getEmail().trim());
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) receptionist.setPhone(dto.getPhone().trim());
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            try {
                receptionist.setGender(Gender.valueOf(dto.getGender().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Gender must be MALE, FEMALE or OTHER");
            }
        }
        if (dto.getAge() < 0 || dto.getAge() > 120) {
            throw new BadRequestException("Age must be between 0 and 120");
        }
        if (dto.getAge() != 0) receptionist.setAge(dto.getAge());
        if (profileImage != null && !profileImage.isEmpty()) {
            receptionist.setProfileImage(fileStorageService.storeProfileImage(profileImage));
        }
        User user = receptionist.getUser();
        if (user != null) {
            userService.updateContactDetails(user, receptionist.getEmail(), receptionist.getPhone());
        }
        return this.convertToDTO(receptionistRepository.save(receptionist));
    }

    private Receptionist receptionistByUserId(Long userId) {
        return receptionistRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "userId", userId));
    }

    private ReceptionistDTO convertToDTO(Receptionist receptionist) {
        return modelMapper.map(receptionist, ReceptionistDTO.class);
    }

    // ------------------------------------------------------------------ module API
    // (used by other modules instead of ReceptionistRepository)

    public Optional<Receptionist> findReceptionistEntity(Long receptionistId) {
        return receptionistRepository.findById(receptionistId);
    }

    public Optional<Receptionist> findReceptionistEntityByUserId(Long userId) {
        return receptionistRepository.findByUser_Id(userId);
    }

    public void deleteReceptionistEntity(Receptionist receptionist) {
        receptionistRepository.delete(receptionist);
    }
}
