package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// The doctor's record of a completed appointment: findings, diagnosis and prescription.
@Entity
@Table(name = "consultations")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "appointment_id", unique = true, nullable = false)
    private Appointment appointment;

    @Column(columnDefinition = "TEXT")
    private String symptoms;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String diagnosis;
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Vitals (all optional)
    private String bloodPressure;   // e.g. "120/80"
    private Integer pulse;          // beats per minute
    private Double temperature;     // °C
    private Double weightKg;

    private LocalDate followUpDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PrescriptionItem> medicines = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public Integer getPulse() { return pulse; }
    public void setPulse(Integer pulse) { this.pulse = pulse; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<PrescriptionItem> getMedicines() { return medicines; }

    /** Replaces the prescription while keeping the managed collection (required by orphanRemoval). */
    public void replaceMedicines(List<PrescriptionItem> items) {
        medicines.clear();
        for (PrescriptionItem item : items) {
            item.setConsultation(this);
            medicines.add(item);
        }
    }
}
