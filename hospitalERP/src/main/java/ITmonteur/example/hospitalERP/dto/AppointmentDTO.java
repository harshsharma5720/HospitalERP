package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.AppointmentStatus;
import ITmonteur.example.hospitalERP.entities.Gender;
import ITmonteur.example.hospitalERP.entities.Shift;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentDTO {

    private long appointmentID;    // Optional for booking, system generated
    private String patientName;
    private Gender gender;
    private int age;
    private Long doctorId;         // Read-only, taken from the slot
    private String doctorName;     // Read-only, taken from the slot
    private Shift shift;
    private LocalDate date;        // Appointment date
    private LocalTime startTime;   // Read-only, taken from the slot
    private LocalTime endTime;     // Read-only, taken from the slot
    private String message;        // Optional note for doctor
    private Long ptInfoId;         // Patient who owns the appointment (forced to the caller for patients)
    private Long relativeId;       // Optional, set when booking for a relative
    private Long slotId;
    private AppointmentStatus status;

    public AppointmentDTO() {}

    public AppointmentDTO(long appointmentID, String patientName, Gender gender, int age,
                          String doctorName, Shift shift, LocalDate date, String message,
                          Long ptInfoId, Long slotId) {
        this.appointmentID = appointmentID;
        this.patientName = patientName;
        this.gender = gender;
        this.age = age;
        this.doctorName = doctorName;
        this.shift = shift;
        this.date = date;
        this.message = message;
        this.ptInfoId = ptInfoId;
        this.slotId=slotId;
    }

    // --- Getters and Setters ---
    public long getAppointmentID() { return appointmentID; }
    public void setAppointmentID(long appointmentID) { this.appointmentID = appointmentID; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getPtInfoId() { return ptInfoId; }
    public void setPtInfoId(Long ptInfoId) { this.ptInfoId = ptInfoId; }

    public Long getRelativeId() { return relativeId; }
    public void setRelativeId(Long relativeId) { this.relativeId = relativeId; }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
}
