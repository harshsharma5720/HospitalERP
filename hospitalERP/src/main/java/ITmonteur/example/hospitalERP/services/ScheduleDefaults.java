package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Shift;

import java.time.LocalTime;

/** Hospital-wide working hours used when a doctor has not configured a day/shift. */
public final class ScheduleDefaults {

    public static final int SLOT_MINUTES = 10;

    private ScheduleDefaults() {}

    public record Hours(boolean working, LocalTime start, LocalTime end, int slotMinutes) {}

    public static Hours forShift(Shift shift) {
        return switch (shift) {
            case MORNING -> new Hours(true, LocalTime.of(9, 0), LocalTime.of(12, 0), SLOT_MINUTES);
            case EVENING -> new Hours(true, LocalTime.of(15, 0), LocalTime.of(19, 0), SLOT_MINUTES);
        };
    }
}
