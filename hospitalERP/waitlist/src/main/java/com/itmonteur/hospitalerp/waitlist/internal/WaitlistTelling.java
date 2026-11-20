package com.itmonteur.hospitalerp.waitlist.internal;

import com.itmonteur.hospitalerp.scheduling.SlotFreedEvent;
import com.itmonteur.hospitalerp.staff.DoctorLeaveApprovedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Telling the people waiting (docs/WAITLIST_PLAN.md). A freed time is looked at as the freeing change commits - still
 * in its transaction, so the messages commit with it - when the change is complete: a doctor being deactivated is
 * inactive by then, so nobody is told about times that are going away. An approved leave ends the waits for its days.
 */
@Component
public class WaitlistTelling {

    private final WaitlistService waitlistService;

    public WaitlistTelling(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onTimeFreed(SlotFreedEvent event) {
        waitlistService.tellFreed(event);
    }

    @EventListener
    public void onLeaveApproved(DoctorLeaveApprovedEvent event) {
        waitlistService.endForLeave(event.doctorId(), event.startDate(), event.endDate());
    }
}
