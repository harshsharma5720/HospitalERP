package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Gender;
import ITmonteur.example.hospitalERP.entities.Shift;
import java.time.LocalDate;

public class AppointmentDTO {

    private long appointmentID;    // Optional for booking, system generated
    private String patientName;
    private Gender gender;
    private int age;
    private String doctorName;     // Patient selects doctor by name
    private Shift shift;
    private LocalDate date;        // Appointment date
    private String message;        // Optional note for doctor
    private Long ptInfoId;         // Optional, link to patient info

    public AppointmentDTO() {}

    public AppointmentDTO(long appointmentID, String patientName, Gender gender, int age,
                          String doctorName, Shift shift, LocalDate date, String message,
                          Long ptInfoId) {
        this.appointmentID = appointmentID;
        this.patientName = patientName;
        this.gender = gender;
        this.age = age;
        this.doctorName = doctorName;
        this.shift = shift;
        this.date = date;
        this.message = message;
        this.ptInfoId = ptInfoId;
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

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getPtInfoId() { return ptInfoId; }
    public void setPtInfoId(Long ptInfoId) { this.ptInfoId = ptInfoId; }
}
