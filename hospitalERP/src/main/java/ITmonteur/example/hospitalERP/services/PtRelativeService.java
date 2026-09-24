package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.PtRelativeDTO;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.entities.PtRelative;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.PtRelativeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;

    public PtRelativeService(PtRelativeRepository ptRelativeRepository, PtInfoRepository ptInfoRepository,
                             AppointmentRepository appointmentRepository, CurrentUserService currentUserService) {
        this.ptRelativeRepository = ptRelativeRepository;
        this.ptInfoRepository = ptInfoRepository;
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
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
        return convertToDTO(ptRelativeRepository.save(relative));
    }

    // Past appointments booked for the relative are kept; only the link to the relative is removed
    @Transactional
    public String deleteRelative(Long id) {
        PtRelative relative = findRelative(id);
        requireOwner(relative.getPtInfo());
        appointmentRepository.clearRelative(id);
        ptRelativeRepository.delete(relative);
        logger.info("Relative deleted with ID: {}", id);
        return "Relative removed successfully!";
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
        return new PtRelativeDTO(
                relative.getId(),
                relative.getName(),
                relative.getGender(),
                relative.getDob(),
                relative.getRelationship(),
                relative.getPatientAadharNo(),
                relative.getPtInfo() != null ? relative.getPtInfo().getPatientId() : null
        );
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
}
