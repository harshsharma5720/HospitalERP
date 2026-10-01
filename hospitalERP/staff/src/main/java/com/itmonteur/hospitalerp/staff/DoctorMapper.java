package com.itmonteur.hospitalerp.staff;


/** Doctor → DTO. Belongs to the staff module. */
public final class DoctorMapper {

    private DoctorMapper() {}

    public static DoctorDTO toDTO(Doctor doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setName(doctor.getName());
        dto.setEmail(doctor.getEmail());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setSpecialist(doctor.getSpecialist() != null ? doctor.getSpecialist().toString() : null);
        dto.setUserName(doctor.getUserName());
        dto.setProfileImage(doctor.getProfileImage());
        if (doctor.getUser() != null) {
            dto.setUserId(doctor.getUser().getId());
        }
        dto.setActive(doctor.isAccountActive());
        return dto;
    }
}
