package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * A doctor's working hours for one weekday + shift.
 * Days/shifts without a row use the hospital defaults (see ScheduleDefaults).
 */
@Entity
@Table(name = "doctor_schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "day_of_week", "shift"}))
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;

    @Column(nullable = false)
    private boolean working;
    private LocalTime startTime;
    private LocalTime endTime;
    @Column(nullable = false)
    private int slotMinutes;

    public Long getId() { return id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

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
}
