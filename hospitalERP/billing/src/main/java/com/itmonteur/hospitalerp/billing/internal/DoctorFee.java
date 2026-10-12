package com.itmonteur.hospitalerp.billing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A doctor's consultation fee in rupees, set by an admin (docs/BILLING_PLAN.md). Keyed by the doctor's id. */
@Entity
@Table(name = "doctor_fees")
public class DoctorFee {

    @Id
    private Long doctorId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String updatedBy;

    protected DoctorFee() {
    }

    public DoctorFee(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getDoctorId() { return doctorId; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
