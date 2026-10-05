package com.itmonteur.hospitalerp.administration.internal;

import com.itmonteur.hospitalerp.audit.AuditEntryDTO;
import com.itmonteur.hospitalerp.audit.AuditFilter;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The admin's view of the audit log (docs/AUDIT_LOG_PLAN.md): entries newest first, with the patients'
 * current names added here, because the audit module can't depend on patients.
 */
@Service
public class AuditLogQuery {

    private final AuditLog auditLog;
    private final PtInfoService ptInfoService;

    public AuditLogQuery(AuditLog auditLog, PtInfoService ptInfoService) {
        this.auditLog = auditLog;
        this.ptInfoService = ptInfoService;
    }

    public AuditLogPage search(AuditFilter filter, int page, int size) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        Page<AuditEntryDTO> result = auditLog.search(filter, page, size);
        Set<Long> patientIds = result.getContent().stream()
                .map(AuditEntryDTO::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = patientIds.isEmpty() ? Map.of() : ptInfoService.findPatientNames(patientIds);
        result.getContent().forEach(entry ->
                entry.setPatientName(entry.getPatientId() == null ? null : names.get(entry.getPatientId())));
        return new AuditLogPage(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }
}
