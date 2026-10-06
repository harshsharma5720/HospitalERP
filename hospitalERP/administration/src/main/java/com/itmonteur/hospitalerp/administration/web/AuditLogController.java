package com.itmonteur.hospitalerp.administration.web;

import com.itmonteur.hospitalerp.administration.internal.AuditLogPage;
import com.itmonteur.hospitalerp.administration.internal.AuditLogQuery;
import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditFilter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * The audit log for admins (docs/AUDIT_LOG_PLAN.md). Under /api/admin, so admins only. Every filter is
 * optional; {@code from} and {@code to} are days (yyyy-MM-dd), both included.
 */
@RestController
@RequestMapping("/api/admin/audit-log")
public class AuditLogController {

    private final AuditLogQuery auditLogQuery;

    public AuditLogController(AuditLogQuery auditLogQuery) {
        this.auditLogQuery = auditLogQuery;
    }

    @GetMapping
    public ResponseEntity<AuditLogPage> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLogQuery.search(new AuditFilter(patientId, username, action, from, to), page, size));
    }
}
