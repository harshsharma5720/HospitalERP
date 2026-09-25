package ITmonteur.example.hospitalERP.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationDTO {

    // --- read-only, filled by the server ---
    private Long id;
    private Long appointmentId;
    private LocalDate appointmentDate;
    private String patientName;
    private String doctorName;
    private String doctorSpecialist;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- written by the doctor ---
    @Size(max = 2000)
    private String symptoms;
    @NotBlank(message = "Diagnosis is required")
    @Size(max = 2000)
    private String diagnosis;
    @Size(max = 4000)
    private String notes;
    @Pattern(regexp = "^$|^\\d{2,3}/\\d{2,3}$", message = "Blood pressure must look like 120/80")
    private String bloodPressure;
    @Min(value = 20, message = "Pulse looks too low") @Max(value = 250, message = "Pulse looks too high")
    private Integer pulse;
    @DecimalMin(value = "30.0", message = "Temperature must be in °C") @DecimalMax(value = "45.0", message = "Temperature must be in °C")
    private Double temperature;
    @DecimalMin(value = "0.5") @DecimalMax(value = "500")
    private Double weightKg;
    private LocalDate followUpDate;
    @Valid
    @Size(max = 30, message = "At most 30 medicines")
    private List<PrescriptionItemDTO> medicines = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialist() { return doctorSpecialist; }
    public void setDoctorSpecialist(String doctorSpecialist) { this.doctorSpecialist = doctorSpecialist; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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

    public List<PrescriptionItemDTO> getMedicines() { return medicines; }
    public void setMedicines(List<PrescriptionItemDTO> medicines) { this.medicines = medicines == null ? new ArrayList<>() : medicines; }
}
