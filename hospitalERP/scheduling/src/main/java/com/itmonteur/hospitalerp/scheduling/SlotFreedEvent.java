package com.itmonteur.hospitalerp.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Published by the scheduling module (SlotService.releaseSlot) when a booked time becomes bookable again: a visit
 * cancelled or moved, or the visits of a deactivated account. Not when the doctor is away that day. Inside the
 * freeing transaction; the waitlist tells who is waiting when it commits (docs/WAITLIST_PLAN.md). Belongs to the
 * scheduling module.
 */
public record SlotFreedEvent(Long slotId, Long doctorId, LocalDate date, Shift shift, LocalTime startTime,
                             LocalTime endTime) {
}
