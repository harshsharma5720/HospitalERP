package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.ReceptionistDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.ReceptionistRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
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
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final UserAccountService userAccountService;

    public ReceptionistService(ReceptionistRepository receptionistRepository, UserRepository userRepository,
                               ModelMapper modelMapper, CurrentUserService currentUserService,
                               FileStorageService fileStorageService, UserAccountService userAccountService) {
        this.receptionistRepository = receptionistRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.userAccountService = userAccountService;
    }

    // By user ID (the receptionist themself or an admin)
    public ReceptionistDTO getReceptionistByID(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return this.convertToDTO(receptionistByUserId(userId));
    }

    public List<ReceptionistDTO> getAllReceptionist() {
        return receptionistRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    // By receptionist ID (admin only; also deletes the login)
    public boolean deleteReceptionist(Long receptionistID) {
        Receptionist receptionist = receptionistRepository.findById(receptionistID)
                .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistID));
        if (receptionist.getUser() != null) {
            userAccountService.deleteUser(receptionist.getUser().getId());
        } else {
            receptionistRepository.delete(receptionist);
        }
        logger.info("Receptionist deleted successfully with ID: {}", receptionistID);
        return true;
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
            user.setEmail(receptionist.getEmail());
            if (receptionist.getPhone() != null) {
                user.setPhoneNumber(receptionist.getPhone());
            }
            userRepository.save(user);
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
}
