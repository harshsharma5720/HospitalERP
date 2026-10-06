package com.itmonteur.hospitalerp.audit;

import java.time.LocalDate;

/**
 * Filters for {@link AuditLog#search}; every field may be null (= no filter).
 *
 * @param username the acting user's username, exact
 * @param from     first day to include
 * @param to       last day to include
 */
public record AuditFilter(Long patientId, String username, AuditAction action, LocalDate from, LocalDate to) {

    public static AuditFilter none() {
        return new AuditFilter(null, null, null, null, null);
    }
}
