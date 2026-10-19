package com.itmonteur.hospitalerp.inbox.web;

import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.inbox.internal.InboxDTO;
import com.itmonteur.hospitalerp.inbox.internal.InboxMessageDTO;
import com.itmonteur.hospitalerp.inbox.internal.InboxStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** The logged-in user's notification centre (docs/NOTIFICATION_CENTRE_PLAN.md); always their own messages. */
@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final InboxStore store;
    private final CurrentUserService currentUserService;

    public InboxController(InboxStore store, CurrentUserService currentUserService) {
        this.store = store;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<InboxDTO> newest() {
        return ResponseEntity.ok(store.newest(me()));
    }

    // Asked every 30 seconds by the app: kept small
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", store.unread(me())));
    }

    @PostMapping("/{messageId}/read")
    public ResponseEntity<InboxMessageDTO> read(@PathVariable long messageId) {
        return ResponseEntity.ok(store.markRead(me(), messageId));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> readAll() {
        return ResponseEntity.ok(Map.of("read", store.markAllRead(me())));
    }

    private long me() {
        return currentUserService.getCurrentUserId();
    }
}
