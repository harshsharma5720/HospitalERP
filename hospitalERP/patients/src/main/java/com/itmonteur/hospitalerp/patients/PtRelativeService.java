package com.itmonteur.hospitalerp.patients;

import java.util.Optional;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ForbiddenException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.patients.internal.AbhaRegistry;
import com.itmonteur.hospitalerp.patients.internal.PtInfoRepository;
import com.itmonteur.hospitalerp.patients.internal.PtRelativeRepository;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// Relatives belong to a patient; only that patient (or an admin) can see or change them.
@Service
public class PtRelativeService {

    private static final Logger logger = LoggerFactory.getLogger(PtRelativeService.class);

    private final PtRelativeRepository ptRelativeRepository;
    private final PtInfoRepository ptInfoRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final AbhaRegistry abhaRegistry;
    private final AuditLog auditLog;

    public PtRelativeService(PtRelativeRepository ptRelativeRepository, PtInfoRepository ptInfoRepository,
                             CurrentUserService currentUserService, ApplicationEventPublisher eventPublisher,
                             AbhaRegistry abhaRegistry, AuditLog auditLog) {
        this.ptRelativeRepository = ptRelativeRepository;
        this.ptInfoRepository = ptInfoRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
        this.abhaRegistry = abhaRegistry;
        this.auditLog = auditLog;
    }

    /** Patients always add to their own account; admins must pass patientId. */
    public PtRelativeDTO addRelative(PtRelativeDTO dto) {
        PtInfo patient;
        if (currentUserService.hasRole(Role.ADMIN)) {
            if (dto.getPatientId() == null) {
                throw new BadRequestException("patientId is required");
            }
            patient = ptInfoRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "patientId", dto.getPatientId()));
        } else {
            Long userId = currentUserService.getCurrentUserId();
            patient = ptInfoRepository.findByUser_Id(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
        }
        PtRelative relative = new PtRelative();
        copyFields(dto, relative);
        setAbha(relative, dto.getAbhaNumber());
        relative.setPtInfo(patient);
        PtRelative saved = ptRelativeRepository.save(relative);
        logger.info("Relative {} added for patient {}", saved.getId(), patient.getPatientId());
        return convertToDTO(saved);
    }

    public List<PtRelativeDTO> getRelativesByPatient(Long patientId) {
        PtInfo patient = ptInfoRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "patientId", patientId));
        requireOwner(patient);
        return ptRelativeRepository.findByPtInfoPatientId(patientId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public PtRelativeDTO getRelativeById(Long id) {
        PtRelative relative = findRelative(id);
        requireOwner(relative.getPtInfo());
        return convertToDTO(relative);
    }

    public PtRelativeDTO updateRelative(Long id, PtRelativeDTO dto) {
        PtRelative relative = findRelative(id);
        requireOwner(relative.getPtInfo());
        copyFields(dto, relative);
        setAbha(relative, dto.getAbhaNumber()); // the form sends them all: an empty one removes it
        return convertToDTO(ptRelativeRepository.save(relative));
    }

    // Past appointments booked for the relative are kept; only the link to the relative is removed
    @Transactional
    public String deleteRelative(Long id) {
        PtRelative relative = findRelative(id);
        requireOwner(relative.getPtInfo());
        // The appointments module unlinks the relative from bookings (synchronously, same transaction)
        eventPublisher.publishEvent(new RelativeDeletedEvent(id));
        ptRelativeRepository.delete(relative);
        logger.info("Relative deleted with ID: {}", id);
        return "Relative removed successfully!";
    }

    // ---------------------------------------------------------------- the front desk (docs/ABHA_NUMBER_PLAN.md)

    /** A patient's family members, for the front desk's patient page (access is checked on its URL). */
    public List<PtRelativeDTO> relativesForDesk(long patientId) {
        if (!ptInfoRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", "patientId", patientId);
        }
        return ptRelativeRepository.findByPtInfoPatientId(patientId).stream().map(this::convertToDTO).toList();
    }

    /** The front desk sets a family member's ABHA number; the family member must be the patient's. */
    @Transactional
    public PtRelativeDTO setAbhaNumber(long patientId, long relativeId, String abhaNumber) {
        PtRelative relative = findRelative(relativeId);
        if (relative.getPtInfo() == null || !Objects.equals(relative.getPtInfo().getPatientId(), patientId)) {
            throw new ResourceNotFoundException("Relative", "relativeId", relativeId);
        }
        String before = relative.getAbhaNumber();
        setAbha(relative, abhaNumber);
        PtRelativeDTO updated = convertToDTO(ptRelativeRepository.save(relative));
        auditLog.record(AuditAction.PATIENT_PROFILE_UPDATED, patientId, patientId,
                Objects.equals(before, relative.getAbhaNumber()) ? "no change" : "changed: a family member's ABHA number");
        return updated;
    }

    // 14 digits or none; refused when it is on another record
    private void setAbha(PtRelative relative, String abhaNumber) {
        String abha = AbhaNumber.normalize(abhaNumber);
        abhaRegistry.requireFree(abha, null, relative.getId());
        relative.setAbhaNumber(abha);
    }

    private void requireOwner(PtInfo patient) {
        if (currentUserService.hasRole(Role.ADMIN)) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (patient == null || patient.getUser() == null || !Objects.equals(patient.getUser().getId(), userId)) {
            throw new ForbiddenException("You can only manage your own relatives");
        }
    }

    private PtRelative findRelative(Long id) {
        return ptRelativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relative", "relativeId", id));
    }

    private PtRelativeDTO convertToDTO(PtRelative relative) {
        PtRelativeDTO dto = new PtRelativeDTO(
                relative.getId(),
                relative.getName(),
                relative.getGender(),
                relative.getDob(),
                relative.getRelationship(),
                relative.getPatientAadharNo(),
                relative.getPtInfo() != null ? relative.getPtInfo().getPatientId() : null
        );
        dto.setAbhaNumber(relative.getAbhaNumber());
        return dto;
    }

    private static void copyFields(PtRelativeDTO dto, PtRelative relative) {
        if (dto.getPatientAadharNo() != null && String.valueOf(dto.getPatientAadharNo()).length() != 12) {
            throw new BadRequestException("Aadhaar number must have 12 digits");
        }
        relative.setName(dto.getName());
        relative.setGender(dto.getGender());
        relative.setDob(dto.getDob());
        relative.setRelationship(dto.getRelationship());
        relative.setPatientAadharNo(dto.getPatientAadharNo());
    }

    // Module API (used by the appointments module instead of PtRelativeRepository)
    public Optional<PtRelative> findRelativeEntity(Long relativeId) {
        return ptRelativeRepository.findById(relativeId);
    }
}
