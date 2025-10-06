package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.PtInfoDTO;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PtInfoService {
    @Autowired
    private PtInfoRepository ptInfoRepository;

    @Autowired
    private ModelMapper modelMapper;

    public List<PtInfoDTO> getAllPtInfo(){
        List<PtInfo> ptInfos = this.ptInfoRepository.findAll();
        List<PtInfoDTO> ptInfoDTOS = ptInfos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ptInfoDTOS;

    }

    public PtInfoDTO getPtInfoById(long ptId){
        PtInfo ptInfo = this.ptInfoRepository.findById(ptId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient","patientID", ptId));
        PtInfoDTO ptInfoDTO = this.convertToDto(ptInfo);
        return ptInfoDTO;
    }

    public boolean deletePtInfoById(long ptId){
        this.ptInfoRepository.deleteById(ptId);
        return true;
    }

    public PtInfoDTO updatePtInfoById(PtInfoDTO ptInfoDTO,long ptId){
        PtInfo ptInfo = this.ptInfoRepository.findById(ptId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + ptId));
        ptInfo.setPatientName(ptInfoDTO.getPatientName());
        ptInfo.setDob(ptInfoDTO.getDob());
        ptInfo.setEmail(ptInfoDTO.getEmail());
        ptInfo.setGender(ptInfoDTO.getGender());
        ptInfo.setContactNo(ptInfoDTO.getContactNo());
        ptInfo.setPatientAadharNo(ptInfoDTO.getPatientAadharNo());
        ptInfo.setPatientAddress(ptInfoDTO.getPatientAddress());
        // ptInfo.setAppointment(ptInfoDTO.getAppointment());
        return convertToDto(ptInfoRepository.save(ptInfo));
    }

    private PtInfoDTO convertToDto(PtInfo ptInfo){
        return modelMapper.map(ptInfo, PtInfoDTO.class);

    }
    private PtInfo covertToEntities(PtInfoDTO ptInfoDTO){
        return modelMapper.map(ptInfoDTO,PtInfo.class);
    }
}
