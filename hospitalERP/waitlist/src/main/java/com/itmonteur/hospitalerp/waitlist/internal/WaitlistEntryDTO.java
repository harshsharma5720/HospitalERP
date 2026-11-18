package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.scheduling.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A wait (docs/WAITLIST_PLAN.md). {@code name}: the person waiting; {@code patientName}: the patient record (the
 * account holder of a family member); {@code phone}: the record's, where the SMS goes; {@code shift} null: either.
 */
public record WaitlistEntryDTO(Long id, Long patientId, Long relativeId, String name, String patientName, String phone,
                               boolean hasLogin, Long doctorId, String doctorName, LocalDate date, Shift shift,
                               WaitlistStatus status, LocalDateTime joinedAt, String joinedBy, LocalDateTime toldAt) {

    static WaitlistEntryDTO of(WaitlistEntry e) {
        return new WaitlistEntryDTO(e.getId(), e.getPatient().getPatientId(),
                e.getRelative() == null ? null : e.getRelative().getId(), e.personName(), e.getPatient().getPatientName(),
                e.getPatient().getContactNo(), e.getPatient().getUser() != null, e.getDoctor().getId(),
                e.getDoctor().getName(), e.getDate(), e.getShift(), e.getStatus(), e.getJoinedAt(), e.getJoinedBy(),
                e.getToldAt());
    }
}
