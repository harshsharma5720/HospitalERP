package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.scheduling.Shift;
import com.itmonteur.hospitalerp.scheduling.Slot;

import java.time.LocalDate;
import java.time.LocalTime;

/** A free slot offered at the front desk. */
public record FreeSlotDTO(Long slotId, LocalDate date, Shift shift, LocalTime startTime, LocalTime endTime) {

    static FreeSlotDTO of(Slot slot) {
        return new FreeSlotDTO(slot.getId(), slot.getDate(), slot.getShift(), slot.getStartTime(), slot.getEndTime());
    }
}
