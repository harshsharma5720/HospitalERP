package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.PtInfo;

/** Patient (PtInfo) → DTO. Belongs to the patients module. */
public final class PatientMapper {

    private PatientMapper() {}

    public static PtInfoDTO toDTO(PtInfo ptInfo) {
        PtInfoDTO dto = new PtInfoDTO();
        dto.setPatientId(ptInfo.getPatientId());
        dto.setPatientName(ptInfo.getPatientName());
        dto.setEmail(ptInfo.getEmail());
        dto.setPatientAddress(ptInfo.getPatientAddress());
        dto.setPatientAadharNo(ptInfo.getPatientAadharNo());
        dto.setContactNo(ptInfo.getContactNo());
        dto.setDob(ptInfo.getDob());
        dto.setGender(ptInfo.getGender());
        dto.setUserName(ptInfo.getUserName());
        dto.setProfileImage(ptInfo.getProfileImage());
        return dto;
    }
}
