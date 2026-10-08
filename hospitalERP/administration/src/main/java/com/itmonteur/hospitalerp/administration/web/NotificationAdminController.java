package com.itmonteur.hospitalerp.administration.web;

import com.itmonteur.hospitalerp.notifications.NotificationOutbox;
import com.itmonteur.hospitalerp.notifications.OutboxMessageDTO;
import com.itmonteur.hospitalerp.notifications.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Appointment emails / SMS in the notifications outbox (docs/RELIABLE_NOTIFICATIONS_PLAN.md): the admin sees what
 * couldn't be delivered and can send it again. Under /api/admin, so admins only.
 */
@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationAdminController {

    /** One page, in the same shape as the audit log's. */
    public record NotificationPage(List<OutboxMessageDTO> entries, int page, int size, long totalEntries, int totalPages) {
    }

    private final NotificationOutbox outbox;

    public NotificationAdminController(NotificationOutbox outbox) {
        this.outbox = outbox;
    }

    /** {@code status}: PENDING, SENDING, SENT, SKIPPED or FAILED (all when missing); newest first. */
    @GetMapping
    public ResponseEntity<NotificationPage> list(@RequestParam(required = false) OutboxStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        Page<OutboxMessageDTO> result = outbox.search(status, page, size);
        return ResponseEntity.ok(new NotificationPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    /** Only failed or skipped messages (409 otherwise): due at once, retried for another 24 hours. */
    @PostMapping("/{id}/resend")
    public ResponseEntity<OutboxMessageDTO> resend(@PathVariable Long id) {
        return ResponseEntity.ok(outbox.resend(id));
    }
}
