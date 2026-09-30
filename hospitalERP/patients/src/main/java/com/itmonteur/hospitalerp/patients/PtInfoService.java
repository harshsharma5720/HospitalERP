package com.itmonteur.hospitalerp.patients;

import java.util.Optional;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.patients.internal.PtInfoRepository;
import com.itmonteur.hospitalerp.common.FileStorageService;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.UserService;
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
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public PtInfoService(PtInfoRepository ptInfoRepository, UserService userService,
                         CurrentUserService currentUserService, FileStorageService fileStorageService) {
        this.ptInfoRepository = ptInfoRepository;
        this.userService = userService;
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
            userService.updateContactDetails(user, ptInfo.getEmail(), ptInfo.getContactNo());
        }
        PtInfoDTO updated = PatientMapper.toDTO(ptInfoRepository.save(ptInfo));
        logger.info("Patient profile updated for user {}", userId);
        return updated;
    }

    private PtInfo patientByUserId(long userId) {
        return ptInfoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
    }

    // ------------------------------------------------------------------ module API
    // (used by other modules instead of PtInfoRepository)

    public Optional<PtInfo> findPatientEntity(Long patientId) {
        return ptInfoRepository.findById(patientId);
    }

    public Optional<PtInfo> findPatientEntityByUserId(Long userId) {
        return ptInfoRepository.findByUser_Id(userId);
    }

    /** Deletes the patient row; relatives go with it (cascade). Callers remove bookings first. */
    @Transactional
    public void deletePatientEntity(Long patientId) {
        ptInfoRepository.deleteById(patientId);
    }
}
