package ITmonteur.example.hospitalERP.events;

/**
 * Published by the scheduling module (DoctorScheduleService.updateSchedule) after a doctor's
 * weekly schedule was replaced. The appointments module then tells scheduling which future
 * slots are still used by bookings, so every other future slot can be removed and regenerated
 * from the new hours. Synchronous, same transaction. Belongs to the scheduling module.
 *
 * @param doctorId the doctor's id (doctor.id, not the user id)
 */
public record DoctorScheduleChangedEvent(Long doctorId) {
}
