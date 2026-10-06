package com.itmonteur.hospitalerp.administration.internal;

import com.itmonteur.hospitalerp.audit.AuditEntryDTO;

import java.util.List;

/** One page of the audit log as GET /api/admin/audit-log returns it; {@code page} counts from 0. */
public record AuditLogPage(List<AuditEntryDTO> entries, int page, int size, long totalEntries, int totalPages) {
}
