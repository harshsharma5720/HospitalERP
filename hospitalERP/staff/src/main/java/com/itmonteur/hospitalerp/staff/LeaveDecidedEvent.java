package com.itmonteur.hospitalerp.staff;

import java.time.LocalDate;

/**
 * An admin approved or rejected a leave request (docs/NOTIFICATION_CENTRE_PLAN.md): the one who asked is told. Published
 * by LeaveRequestService inside the decision's transaction, next to {@link DoctorLeaveApprovedEvent}. Belongs to the
 * staff module.
 */
public record LeaveDecidedEvent(Long leaveId, long userId, boolean approved, LocalDate startDate, LocalDate endDate) {
}
