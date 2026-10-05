package com.itmonteur.hospitalerp.audit.internal;

/** Published by {@code AuditLog.record}, written by {@link AuditEntryWriter} once the transaction commits. */
public record AuditEntryRecorded(AuditEntry entry) {
}
