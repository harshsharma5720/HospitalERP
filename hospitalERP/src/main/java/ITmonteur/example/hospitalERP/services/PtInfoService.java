package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PtInfoService {

    private static final Logger logger = LoggerFactory.getLogger(PtInfoService.class);

    @Autowired
    private PtInfoRepository ptInfoRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Get all patients info
    public List<PtInfoDTO> getAllPtInfo() {
        logger.info("Fetching all patient information");
        List<PtInfo> ptInfos = ptInfoRepository.findAll();
        List<PtInfoDTO> ptInfoDTOS = ptInfos.stream()
                .map(ptInfo -> {
                    PtInfoDTO dto = convertToDto(ptInfo);
                    dto.setAppointment(null);
                    return dto;
                })
                .collect(Collectors.toList());
        logger.info("Total patients retrieved: {}", ptInfoDTOS.size());
        return ptInfoDTOS;
    }

    // Get patient info by ID
    public PtInfoDTO getPtInfoById(long ptId) {
        logger.info("Fetching patient information with ID: {}", ptId);
        try {
            PtInfo ptInfo = ptInfoRepository.findById(ptId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "patientID", ptId));
            PtInfoDTO ptInfoDTO = convertToDto(ptInfo);
            logger.info("Patient information retrieved: {}", ptInfoDTO.getPatientName());
            return ptInfoDTO;
        } catch (ResourceNotFoundException e) {
            logger.warn("Patient not found with ID: {}", ptId);
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching patient information with ID {}: {}", ptId, e.getMessage(), e);
            throw e;
        }
    }

    // Delete patient info by ID
    public boolean deletePtInfoById(long ptId) {
        logger.info("Deleting patient information with ID: {}", ptId);
        try {
            ptInfoRepository.deleteById(ptId);
            logger.info("Patient information deleted successfully for ID: {}", ptId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting patient information with ID {}: {}", ptId, e.getMessage(), e);
            return false;
        }
    }

    // Update patient info by ID
    public PtInfoDTO updatePtInfoById(PtInfoDTO ptInfoDTO, long ptId) {
        logger.info("Updating patient information with ID: {}", ptId);
        try {
            PtInfo ptInfo = ptInfoRepository.findById(ptId)
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + ptId));

            ptInfo.setPatientName(ptInfoDTO.getPatientName());
            ptInfo.setDob(ptInfoDTO.getDob());
            ptInfo.setEmail(ptInfoDTO.getEmail());
            ptInfo.setGender(ptInfoDTO.getGender());
            ptInfo.setContactNo(ptInfoDTO.getContactNo());
            ptInfo.setPatientAadharNo(ptInfoDTO.getPatientAadharNo());
            ptInfo.setPatientAddress(ptInfoDTO.getPatientAddress());
            ptInfo.setUserName(ptInfoDTO.getUserName());
            // ptInfo.setAppointment(ptInfoDTO.getAppointment());

            PtInfoDTO updatedPtInfo = convertToDto(ptInfoRepository.save(ptInfo));
            logger.info("Patient information updated successfully for ID: {}", ptId);
            return updatedPtInfo;

        } catch (Exception e) {
            logger.error("Error updating patient information with ID {}: {}", ptId, e.getMessage(), e);
            throw e;
        }
    }

    // Convert entity to DTO
    private PtInfoDTO convertToDto(PtInfo ptInfo) {
        return modelMapper.map(ptInfo, PtInfoDTO.class);
    }

    // Convert DTO to entity
    private PtInfo covertToEntities(PtInfoDTO ptInfoDTO) {
        return modelMapper.map(ptInfoDTO, PtInfo.class);
    }
}
