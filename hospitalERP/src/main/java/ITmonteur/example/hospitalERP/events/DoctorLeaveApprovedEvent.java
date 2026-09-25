package ITmonteur.example.hospitalERP.events;

import java.time.LocalDate;

/**
 * Published by the staff module (LeaveRequestService) when an admin approves a doctor's leave.
 * Synchronous listeners in the same transaction react to it:
 * the scheduling module blocks the doctor's slots, and the appointments module cancels the
 * bookings in the period and notifies the patients. Belongs to the staff module.
 *
 * @param doctorId  the doctor's id (doctor.id, not the user id)
 * @param startDate first day of the leave (inclusive)
 * @param endDate   last day of the leave (inclusive)
 */
public record DoctorLeaveApprovedEvent(Long doctorId, LocalDate startDate, LocalDate endDate) {
}
