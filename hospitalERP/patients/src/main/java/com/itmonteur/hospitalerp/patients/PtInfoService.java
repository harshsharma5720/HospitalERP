package com.itmonteur.hospitalerp.patients;

import java.util.Optional;
import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditLog;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Patient profile. "ptId" parameters are the patient's *user* id (as issued in the JWT).
// Views, updates and the all-patients list go into the audit log (docs/AUDIT_LOG_PLAN.md).
@Service
public class PtInfoService {

    private static final Logger logger = LoggerFactory.getLogger(PtInfoService.class);

    private final PtInfoRepository ptInfoRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final AuditLog auditLog;

    public PtInfoService(PtInfoRepository ptInfoRepository, UserService userService,
                         CurrentUserService currentUserService, FileStorageService fileStorageService,
                         AuditLog auditLog) {
        this.ptInfoRepository = ptInfoRepository;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.auditLog = auditLog;
    }

    // Get all patients info (admin / receptionist)
    public List<PtInfoDTO> getAllPtInfo() {
        List<PtInfoDTO> patients = ptInfoRepository.findAll().stream().map(PatientMapper::toDTO).toList();
        auditLog.record(AuditAction.PATIENT_LIST_VIEWED, null, null, patients.size() + " patients");
        return patients;
    }

    // Get patient info by user ID (the patient themself or an admin)
    public PtInfoDTO getPtInfoById(long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        PtInfo patient = patientByUserId(userId);
        auditLog.record(AuditAction.PATIENT_PROFILE_VIEWED, patient.getPatientId(), patient.getPatientId(), null);
        return PatientMapper.toDTO(patient);
    }

    // Update patient info by user ID (self or admin). Username cannot be changed here.
    @Transactional
    public PtInfoDTO updatePtInfoById(PtInfoDTO dto, long userId, MultipartFile profileImage) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        PtInfo ptInfo = patientByUserId(userId);
        Map<String, Object> before = auditedFields(ptInfo);

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
        auditLog.record(AuditAction.PATIENT_PROFILE_UPDATED, ptInfo.getPatientId(), ptInfo.getPatientId(),
                changedFields(before, auditedFields(ptInfo)));
        return updated;
    }

    // The audit entry names the changed fields, never their values (e.g. the Aadhaar number)
    private static Map<String, Object> auditedFields(PtInfo p) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", p.getPatientName());
        fields.put("email", p.getEmail());
        fields.put("date of birth", p.getDob());
        fields.put("gender", p.getGender());
        fields.put("phone", p.getContactNo());
        fields.put("Aadhaar number", p.getPatientAadharNo());
        fields.put("address", p.getPatientAddress());
        fields.put("photo", p.getProfileImage());
        return fields;
    }

    private static String changedFields(Map<String, Object> before, Map<String, Object> after) {
        List<String> changed = before.keySet().stream()
                .filter(field -> !Objects.equals(before.get(field), after.get(field)))
                .toList();
        return changed.isEmpty() ? "no change" : "changed: " + String.join(", ", changed);
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

    /** Current names by patient id, in one query (e.g. for a page of the audit log). Unknown ids are left out. */
    public Map<Long, String> findPatientNames(Collection<Long> patientIds) {
        Map<Long, String> names = new HashMap<>();
        ptInfoRepository.findAllById(patientIds).forEach(patient -> names.put(patient.getPatientId(), patient.getPatientName()));
        return names;
    }

    /** Deletes the patient row; relatives go with it (cascade). Callers remove bookings first. */
    @Transactional
    public void deletePatientEntity(Long patientId) {
        ptInfoRepository.deleteById(patientId);
    }
}
