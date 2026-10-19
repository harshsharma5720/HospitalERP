package com.itmonteur.hospitalerp.staff;

import java.time.LocalDate;

/**
 * A doctor (or another member of staff) applied for leave (docs/NOTIFICATION_CENTRE_PLAN.md): the admins are told.
 * Published by LeaveRequestService inside the request's transaction. Belongs to the staff module.
 *
 * @param requester the doctor's name, or the user name without a doctor profile
 */
public record LeaveRequestedEvent(Long leaveId, String requester, LocalDate startDate, LocalDate endDate, String reason) {
}
