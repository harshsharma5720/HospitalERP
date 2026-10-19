package com.itmonteur.hospitalerp.inbox;

import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.inbox.internal.InboxStore;
import org.springframework.stereotype.Service;

/**
 * Public API of the notification centre (docs/NOTIFICATION_CENTRE_PLAN.md): a message to one user, or to every active
 * user of a role. Written in the caller's transaction, so a change that rolls back leaves no message. The inbox's own
 * listeners call it for the other modules' events; texts longer than a message holds are cut.
 */
@Service
public class Inbox {

    private final InboxStore store;
    private final UserService userService;

    public Inbox(InboxStore store, UserService userService) {
        this.store = store;
        this.userService = userService;
    }

    /** To one user; nothing for a null user (e.g. a walk-in patient without a login). */
    public void toUser(Long userId, InboxKind kind, String title, String body, String link) {
        if (userId != null) {
            store.add(userId, kind, title, body, link);
        }
    }

    /** To every active account of the role, e.g. all receptionists. */
    public void toRole(Role role, InboxKind kind, String title, String body, String link) {
        userService.activeUserIds(role).forEach(userId -> store.add(userId, kind, title, body, link));
    }
}
