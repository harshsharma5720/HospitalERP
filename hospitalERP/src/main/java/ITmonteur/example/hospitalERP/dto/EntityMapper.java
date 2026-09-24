package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.AppointmentStatus;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.entities.Slot;

/**
 * Explicit entity → DTO mapping for the entities with nested relations.
 * ModelMapper's implicit matching is ambiguous for these (e.g. ptInfoId could come from
 * ptInfo.patientId, ptInfo.user.id or relative.ptInfo.patientId), so they are mapped by hand.
 */
public final class EntityMapper {

    private EntityMapper() {}

    public static AppointmentDTO toAppointmentDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setAppointmentID(appointment.getAppointmentID());
        dto.setPatientName(appointment.getPatientName());
        dto.setGender(appointment.getGender());
        dto.setAge(appointment.getAge());
        dto.setShift(appointment.getShift());
        dto.setDate(appointment.getDate());
        dto.setMessage(appointment.getMessage());
        // Older rows may have isCompleted=true while status is still SCHEDULED
        dto.setStatus(appointment.isCompleted() ? AppointmentStatus.COMPLETED : appointment.getStatus());
        Doctor doctor = appointment.getDoctor();
        if (doctor != null) {
            dto.setDoctorId(doctor.getId());
            dto.setDoctorName(doctor.getName());
        }
        Slot slot = appointment.getSlot();
        if (slot != null) {
            dto.setSlotId(slot.getId());
            dto.setStartTime(slot.getStartTime());
            dto.setEndTime(slot.getEndTime());
        }
        if (appointment.getPtInfo() != null) {
            dto.setPtInfoId(appointment.getPtInfo().getPatientId());
        }
        if (appointment.getRelative() != null) {
            dto.setRelativeId(appointment.getRelative().getId());
        }
        return dto;
    }

    public static DoctorDTO toDoctorDTO(Doctor doctor) {
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
        return dto;
    }

    public static PtInfoDTO toPtInfoDTO(PtInfo ptInfo) {
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
