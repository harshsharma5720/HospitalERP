package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Shift;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

// One weekday + shift of a doctor's weekly schedule
public class DoctorScheduleDTO {

    @NotNull(message = "dayOfWeek is required")
    private DayOfWeek dayOfWeek;
    @NotNull(message = "shift is required")
    private Shift shift;
    private boolean working;
    private LocalTime startTime;
    private LocalTime endTime;
    @Min(value = 5, message = "Slot length must be at least 5 minutes")
    @Max(value = 120, message = "Slot length must be at most 120 minutes")
    private int slotMinutes = 10;
    // read-only: true when no custom value is stored and the hospital default applies
    private boolean usingDefault;

    public DoctorScheduleDTO() {}

    public DoctorScheduleDTO(DayOfWeek dayOfWeek, Shift shift, boolean working, LocalTime startTime,
                             LocalTime endTime, int slotMinutes, boolean usingDefault) {
        this.dayOfWeek = dayOfWeek;
        this.shift = shift;
        this.working = working;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes;
        this.usingDefault = usingDefault;
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }

    public boolean isWorking() { return working; }
    public void setWorking(boolean working) { this.working = working; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }

    public boolean isUsingDefault() { return usingDefault; }
    public void setUsingDefault(boolean usingDefault) { this.usingDefault = usingDefault; }
}
