package ITmonteur.example.hospitalERP.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long appointmentID;
    @Column(nullable = false)
    private String patientName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; // MALE, FEMALE, OTHER
    private int age;
    @ManyToOne
    @JoinColumn(name = "doctor_id", referencedColumnName = "id")
    @JsonIgnore
    private Doctor doctor; // Doctor entity reference
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift; // MORNING, EVENING
    @ManyToOne
    @JoinColumn(name = "slot_id")
    private Slot slot;
    @Column(nullable = false)
    private LocalDate date;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED; // SCHEDULED, COMPLETED, CANCELLED

    @ManyToOne
    @JoinColumn(name = "patient_id", referencedColumnName = "patientId")
    private PtInfo ptInfo;
    // Set when the appointment is booked for one of the patient's relatives
    @ManyToOne
    @JoinColumn(name = "relative_id")
    private PtRelative relative;
    // Legacy column kept in sync with status so existing rows stay readable
    @Column(nullable = false)
    private boolean isCompleted = false;
    // Set once the day-before reminder has been sent (nullable so existing rows need no migration)
    private Boolean reminderSent;

    // --- Constructors ---
    public Appointment() {}

    public Appointment(long appointmentID, String patientName,Gender gender, int age,
                       Doctor doctor,Shift shift, Slot slot ,LocalDate date,
                       String message, AppointmentStatus status ,PtInfo ptInfo , boolean isCompleted) {
        this.appointmentID = appointmentID;
        this.patientName = patientName;
        this.gender = gender;
        this.age = age;
        this.doctor = doctor;
        this.shift = shift;
        this.slot=slot;
        this.date = date;
        this.message = message;
        this.status = status;
        this.ptInfo = ptInfo;
        this.isCompleted = isCompleted;
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

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
        this.isCompleted = status == AppointmentStatus.COMPLETED;
    }

    // SCHEDULED or CONFIRMED and not completed — can still be cancelled or rescheduled
    public boolean isActive() {
        return !isCompleted
                && (status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED);
    }

    public boolean isReminderSent() { return Boolean.TRUE.equals(reminderSent); }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }

    public PtRelative getRelative() { return relative; }
    public void setRelative(PtRelative relative) { this.relative = relative; }

    public PtInfo getPtInfo() { return ptInfo; }
    public void setPtInfo(PtInfo ptInfo) { this.ptInfo = ptInfo; }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
        if (completed) {
            this.status = AppointmentStatus.COMPLETED;
        }
    }
}
