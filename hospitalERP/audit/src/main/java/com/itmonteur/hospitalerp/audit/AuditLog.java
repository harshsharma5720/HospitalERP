package com.itmonteur.hospitalerp.audit;

import com.itmonteur.hospitalerp.audit.internal.AuditEntry;
import com.itmonteur.hospitalerp.audit.internal.AuditEntryRecorded;
import com.itmonteur.hospitalerp.audit.internal.AuditEntryRepository;
import com.itmonteur.hospitalerp.audit.internal.ClientAddress;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * The audit log (docs/AUDIT_LOG_PLAN.md): who viewed or changed which patient record, and when.
 * Other modules call {@link #record}; administration shows the entries with {@link #search}.
 */
@Service
public class AuditLog {

    private static final Logger logger = LoggerFactory.getLogger(AuditLog.class);
    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_DETAILS_LENGTH = 255;

    private final ApplicationEventPublisher events;
    private final AuditEntryRepository repository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public AuditLog(ApplicationEventPublisher events, AuditEntryRepository repository,
                    CurrentUserService currentUserService, Clock clock) {
        this.events = events;
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    /**
     * Records that the logged-in user (none for a scheduled job) did {@code action}. Time and IP address are
     * taken now; the entry is written once the surrounding transaction commits (right away without one).
     * Never throws: a problem is logged, and the caller's work goes on.
     *
     * @param patientId the patient whose data it is, or null
     * @param targetId  the appointment, patient or user id, as {@link AuditAction#target()} says, or null
     * @param details   short context such as changed field names, never medical content; cut to 255 characters
     */
    public void record(AuditAction action, Long patientId, Long targetId, String details) {
        try {
            Actor actor = currentActor();
            AuditEntry entry = new AuditEntry(LocalDateTime.now(clock), actor.userId(), actor.username(), actor.role(),
                    action, patientId, targetId, cut(details), ClientAddress.current());
            events.publishEvent(new AuditEntryRecorded(entry));
        } catch (RuntimeException e) {
            logger.error("Audit entry {} for patient {} / target {} could not be recorded", action, patientId, targetId, e);
        }
    }

    /** Entries matching the filter, newest first. {@code size} is capped at 100. */
    @Transactional(readOnly = true)
    public Page<AuditEntryDTO> search(AuditFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        String username = filter.username() == null || filter.username().isBlank() ? null : filter.username().trim();
        LocalDateTime from = filter.from() == null ? null : filter.from().atStartOfDay();
        LocalDateTime to = filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay(); // whole last day
        return repository.search(filter.patientId(), username, filter.action(), from, to, pageable).map(AuditLog::toDTO);
    }

    private record Actor(Long userId, String username, String role) {
        static final Actor NONE = new Actor(null, null, null);
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Actor.NONE;
        }
        try {
            User user = currentUserService.getCurrentUser();
            return new Actor(user.getId(), user.getUsername(), user.getRole() == null ? null : user.getRole().name());
        } catch (AuthenticationException e) {
            return new Actor(null, authentication.getName(), null); // logged in, but the account is gone
        }
    }

    private static String cut(String details) {
        return details == null || details.length() <= MAX_DETAILS_LENGTH ? details : details.substring(0, MAX_DETAILS_LENGTH);
    }

    private static AuditEntryDTO toDTO(AuditEntry entry) {
        AuditEntryDTO dto = new AuditEntryDTO();
        dto.setId(entry.getId());
        dto.setOccurredAt(entry.getOccurredAt());
        dto.setActorUserId(entry.getActorUserId());
        dto.setActorUsername(entry.getActorUsername());
        dto.setActorRole(entry.getActorRole());
        dto.setAction(entry.getAction());
        dto.setTargetType(entry.getAction().target());
        dto.setTargetId(entry.getTargetId());
        dto.setPatientId(entry.getPatientId());
        dto.setDetails(entry.getDetails());
        dto.setIpAddress(entry.getIpAddress());
        return dto;
    }
}
