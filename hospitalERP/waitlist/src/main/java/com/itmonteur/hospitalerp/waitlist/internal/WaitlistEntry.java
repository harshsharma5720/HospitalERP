package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtRelative;
import com.itmonteur.hospitalerp.scheduling.Shift;
import com.itmonteur.hospitalerp.staff.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One person waiting for a doctor on a day (docs/WAITLIST_PLAN.md): the patient, or one of their family members
 * ({@code relative}), in one shift or either ({@code shift} null). Deleted with its patient, family member or doctor.
 */
@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The patient record: the person waiting, or the account holder of a family member waiting
    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PtInfo patient;

    @ManyToOne
    @JoinColumn(name = "relative_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PtRelative relative;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Doctor doctor;

    @Column(name = "visit_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WaitlistStatus status;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    // The username of who put them on it: the patient, or someone at the front desk
    @Column(nullable = false, length = 100)
    private String joinedBy;

    // When they were last told about a free time
    private LocalDateTime toldAt;

    private LocalDateTime endedAt;

    protected WaitlistEntry() {
    }

    WaitlistEntry(PtInfo patient, PtRelative relative, Doctor doctor, LocalDate date, Shift shift, String joinedBy,
                  LocalDateTime joinedAt) {
        this.patient = patient;
        this.relative = relative;
        this.doctor = doctor;
        this.date = date;
        this.shift = shift;
        this.joinedBy = joinedBy;
        this.joinedAt = joinedAt;
        this.status = WaitlistStatus.WAITING;
    }

    void end(WaitlistStatus how, LocalDateTime when) {
        status = how;
        endedAt = when;
    }

    // The name of the person waiting
    String personName() {
        return relative != null ? relative.getName() : patient.getPatientName();
    }

    public Long getId() { return id; }
    public PtInfo getPatient() { return patient; }
    public PtRelative getRelative() { return relative; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getDate() { return date; }
    public Shift getShift() { return shift; }
    void setShift(Shift shift) { this.shift = shift; }
    public WaitlistStatus getStatus() { return status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public String getJoinedBy() { return joinedBy; }
    public LocalDateTime getToldAt() { return toldAt; }
    void setToldAt(LocalDateTime toldAt) { this.toldAt = toldAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
