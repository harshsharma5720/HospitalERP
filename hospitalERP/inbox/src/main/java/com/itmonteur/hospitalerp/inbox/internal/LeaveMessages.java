package com.itmonteur.hospitalerp.inbox.internal;

import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.inbox.Inbox;
import com.itmonteur.hospitalerp.inbox.InboxKind;
import com.itmonteur.hospitalerp.staff.LeaveDecidedEvent;
import com.itmonteur.hospitalerp.staff.LeaveRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Leave in the notification centre (docs/NOTIFICATION_CENTRE_PLAN.md): a new request to every admin, the decision to
 * the one who asked. Written in the request's or the decision's transaction.
 */
@Component
public class LeaveMessages {

    private final Inbox inbox;

    public LeaveMessages(Inbox inbox) {
        this.inbox = inbox;
    }

    @EventListener
    public void on(LeaveRequestedEvent event) {
        String reason = event.reason() == null || event.reason().isBlank() ? "" : ": " + event.reason().strip();
        inbox.toRole(Role.ADMIN, InboxKind.LEAVE_REQUESTED, "Leave request: " + event.requester(),
                Texts.days(event.startDate(), event.endDate()) + reason, "/admin/leave-approval");
    }

    @EventListener
    public void on(LeaveDecidedEvent event) {
        String days = Texts.days(event.startDate(), event.endDate());
        if (event.approved()) {
            inbox.toUser(event.userId(), InboxKind.LEAVE_APPROVED, "Your leave is approved",
                    days + ". Appointments on those days are cancelled and the patients told.", "/doctor/leave-management");
        } else {
            inbox.toUser(event.userId(), InboxKind.LEAVE_REJECTED, "Your leave was not approved", days + ".",
                    "/doctor/leave-management");
        }
    }
}
