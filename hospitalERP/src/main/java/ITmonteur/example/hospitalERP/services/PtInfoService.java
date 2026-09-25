package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.PatientMapper;
import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

// Patient profile. "ptId" parameters are the patient's *user* id (as issued in the JWT).
@Service
public class PtInfoService {

    private static final Logger logger = LoggerFactory.getLogger(PtInfoService.class);

    private final PtInfoRepository ptInfoRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public PtInfoService(PtInfoRepository ptInfoRepository, UserRepository userRepository,
                         CurrentUserService currentUserService, FileStorageService fileStorageService) {
        this.ptInfoRepository = ptInfoRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    // Get all patients info (admin / receptionist)
    public List<PtInfoDTO> getAllPtInfo() {
        return ptInfoRepository.findAll().stream().map(PatientMapper::toDTO).toList();
    }

    // Get patient info by user ID (the patient themself or an admin)
    public PtInfoDTO getPtInfoById(long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        return PatientMapper.toDTO(patientByUserId(userId));
    }

    // Update patient info by user ID (self or admin). Username cannot be changed here.
    @Transactional
    public PtInfoDTO updatePtInfoById(PtInfoDTO dto, long userId, MultipartFile profileImage) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        PtInfo ptInfo = patientByUserId(userId);

        if (dto.getPatientName() != null && !dto.getPatientName().isBlank()) {
            ptInfo.setPatientName(dto.getPatientName().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            ptInfo.setEmail(dto.getEmail().trim());
        }
        if (dto.getDob() != null) {
            if (dto.getDob().isAfter(LocalDate.now())) {
                throw new BadRequestException("Date of birth cannot be in the future");
            }
            ptInfo.setDob(dto.getDob());
        }
        if (dto.getGender() != null) {
            ptInfo.setGender(dto.getGender());
        }
        if (dto.getContactNo() != null && !dto.getContactNo().isBlank()) {
            ptInfo.setContactNo(dto.getContactNo().trim());
        }
        if (dto.getPatientAadharNo() != null && dto.getPatientAadharNo() != 0) {
            if (String.valueOf(dto.getPatientAadharNo()).length() != 12) {
                throw new BadRequestException("Aadhaar number must have 12 digits");
            }
            ptInfo.setPatientAadharNo(dto.getPatientAadharNo());
        }
        if (dto.getPatientAddress() != null) {
            ptInfo.setPatientAddress(dto.getPatientAddress());
        }
        if (profileImage != null && !profileImage.isEmpty()) {
            ptInfo.setProfileImage(fileStorageService.storeProfileImage(profileImage));
        }
        // Keep the login account's contact details in sync with the profile
        User user = ptInfo.getUser();
        if (user != null) {
            user.setEmail(ptInfo.getEmail());
            if (ptInfo.getContactNo() != null) {
                user.setPhoneNumber(ptInfo.getContactNo());
            }
            userRepository.save(user);
        }
        PtInfoDTO updated = PatientMapper.toDTO(ptInfoRepository.save(ptInfo));
        logger.info("Patient profile updated for user {}", userId);
        return updated;
    }

    private PtInfo patientByUserId(long userId) {
        return ptInfoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
    }
}
