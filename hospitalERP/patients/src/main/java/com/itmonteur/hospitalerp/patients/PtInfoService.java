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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

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
    private final Clock clock;

    private static final Pattern PHONE = Pattern.compile("\\+?\\d{10,14}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public PtInfoService(PtInfoRepository ptInfoRepository, UserService userService,
                         CurrentUserService currentUserService, FileStorageService fileStorageService,
                         AuditLog auditLog, Clock clock) {
        this.ptInfoRepository = ptInfoRepository;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.auditLog = auditLog;
        this.clock = clock;
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

    // ------------------------------------------------------------------ front desk (docs/WALK_IN_REGISTRATION_PLAN.md)

    /**
     * Registers a walk-in patient: a patient record without a login. Name, phone (10-14 digits; spaces, dashes and
     * brackets are allowed and removed) and gender are required; date of birth and email are optional.
     */
    @Transactional
    public PtInfo registerWalkIn(WalkInPatient details) {
        String name = details.name() == null ? "" : details.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new BadRequestException("Please enter the patient's name (at most 100 characters)");
        }
        String phone = cleanPhone(details.phone());
        if (!PHONE.matcher(phone).matches()) {
            throw new BadRequestException("Please enter a phone number with 10 to 14 digits");
        }
        if (details.gender() == null) {
            throw new BadRequestException("Please choose the patient's gender");
        }
        if (details.dob() != null && details.dob().isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date of birth cannot be in the future");
        }
        String email = details.email() == null || details.email().isBlank() ? null : details.email().trim();
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Please enter a valid email address, or leave it empty");
        }
        PtInfo patient = new PtInfo();
        patient.setPatientName(name);
        patient.setContactNo(phone);
        patient.setGender(details.gender());
        patient.setDob(details.dob());
        patient.setEmail(email);
        patient.setCreatedAt(LocalDateTime.now(clock));
        PtInfo saved = ptInfoRepository.save(patient);
        auditLog.record(AuditAction.WALK_IN_REGISTERED, saved.getPatientId(), saved.getPatientId(), null);
        logger.info("Walk-in patient {} registered", saved.getPatientId());
        return saved;
    }

    /**
     * Patients whose phone number ends with the same 10 digits (so "+91 98765 43210" and "9876543210" match).
     * Several can share a number - a family. Recorded in the audit log.
     */
    @Transactional(readOnly = true)
    public List<PtInfoDTO> findByPhone(String phone) {
        String digits = digitsOf(phone);
        if (digits.length() < 10) {
            throw new BadRequestException("Please enter at least 10 digits of the phone number");
        }
        String last10 = digits.substring(digits.length() - 10);
        List<PtInfoDTO> matches = ptInfoRepository.findByContactNoEndingWith(last10.substring(6)).stream()
                .filter(p -> digitsOf(p.getContactNo()).endsWith(last10))
                .map(PatientMapper::toDTO)
                .toList();
        auditLog.record(AuditAction.PATIENTS_SEARCHED, null, null,
                "by phone ..." + last10.substring(6) + ", " + matches.size() + " found");
        return matches;
    }

    /** Walk-in records (patients without a login) - for the admin dashboard. */
    @Transactional(readOnly = true)
    public long countWalkInPatients() {
        return ptInfoRepository.countByUserIsNull();
    }

    /** New walk-in records per day from {@code from} to {@code to} (both included); only days with some. */
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> countNewWalkInsPerDay(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> perDay = new TreeMap<>();
        ptInfoRepository.findWalkInCreationTimes(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .forEach(createdAt -> perDay.merge(createdAt.toLocalDate(), 1L, Long::sum));
        return perDay;
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> firstWalkInCreationTime() {
        return ptInfoRepository.findFirstWalkInCreationTime();
    }

    private static String cleanPhone(String phone) {
        return phone == null ? "" : phone.trim().replaceAll("[\\s().-]", "");
    }

    private static String digitsOf(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
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
