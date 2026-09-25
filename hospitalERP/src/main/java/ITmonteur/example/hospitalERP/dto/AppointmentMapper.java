package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.AppointmentStatus;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Slot;

/**
 * Appointment → DTO, mapped by hand: ModelMapper's implicit matching is ambiguous here
 * (e.g. ptInfoId could come from ptInfo.patientId, ptInfo.user.id or relative.ptInfo.patientId).
 * Belongs to the appointments module.
 */
public final class AppointmentMapper {

    private AppointmentMapper() {}

    public static AppointmentDTO toDTO(Appointment appointment) {
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
}
