package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.PtRelativeDTO;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.entities.PtRelative;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.PtRelativeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PtRelativeService {

    private static final Logger logger = LoggerFactory.getLogger(PtRelativeService.class);

    @Autowired
    private PtRelativeRepository ptRelativeRepository;

    @Autowired
    private PtInfoRepository ptInfoRepository;

    public PtRelativeDTO addRelative(PtRelativeDTO dto) {
        logger.info("Adding new relative for patientId: {}", dto.getPatientId());
        try {
            PtInfo patient = ptInfoRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Patient", "patientId", dto.getPatientId()));
            PtRelative relative = convertToEntity(dto, patient);
            PtRelative saved = ptRelativeRepository.save(relative);
            logger.info("Relative added successfully for patientId: {}", dto.getPatientId());
            return convertToDTO(saved);
        } catch (Exception e) {
            logger.error("Error while adding relative for patientId {}: {}", dto.getPatientId(), e.getMessage(), e);
            throw e;
        }
    }

    public List<PtRelativeDTO> getRelativesByPatient(Long patientId) {
        logger.info("Fetching all relatives for patientId: {}", patientId);
        try {
            List<PtRelative> relatives = ptRelativeRepository.findByPtInfoPatientId(patientId);
            List<PtRelativeDTO> relativesDTOs = relatives.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            logger.info("Total relatives retrieved: {}", relatives.size());
            return relativesDTOs;
        } catch (Exception e) {
            logger.error("Error fetching relatives for patientId {}: {}", patientId, e.getMessage(), e);
            throw e;
        }
    }

    public PtRelativeDTO getRelativeById(Long id) {
        logger.info("Fetching relative with ID: {}", id);
        try {
            PtRelative relative = ptRelativeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Relative", "relativeId", id));
            logger.info("Relative found: {}", relative.getName());
            return convertToDTO(relative);
        } catch (Exception e) {
            logger.error("Error fetching relative with ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public PtRelativeDTO updateRelative(Long id, PtRelativeDTO dto) {
        logger.info("Updating relative with ID: {}", id);
        try {
            PtRelative relative = ptRelativeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Relative", "relativeId", id));
            relative.setName(dto.getName());
            relative.setGender(dto.getGender());
            relative.setDob(dto.getDob());
            relative.setRelationship(dto.getRelationship());
            relative.setPatientAadharNo(dto.getPatientAadharNo());
            PtRelative updated = ptRelativeRepository.save(relative);
            logger.info("Relative updated successfully with ID: {}", id);
            return convertToDTO(updated);
        } catch (Exception e) {
            logger.error("Error updating relative with ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public String deleteRelative(Long id) {
        logger.info("Deleting relative with ID: {}", id);
        try {
            PtRelative relative = ptRelativeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Relative", "relativeId", id));

            ptRelativeRepository.delete(relative);

            logger.info("Relative deleted successfully with ID: {}", id);
            return "Relative removed successfully!";

        } catch (Exception e) {
            logger.error("Error deleting relative with ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
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

    private PtRelative convertToEntity(PtRelativeDTO dto, PtInfo patient) {
        PtRelative relative = new PtRelative();
        relative.setId(dto.getId());
        relative.setName(dto.getName());
        relative.setGender(dto.getGender());
        relative.setDob(dto.getDob());
        relative.setRelationship(dto.getRelationship());
        relative.setPatientAadharNo(dto.getPatientAadharNo());
        relative.setPtInfo(patient);
        return relative;
    }
}
