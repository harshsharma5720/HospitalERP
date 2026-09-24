package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.DoctorScheduleDTO;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.DoctorSchedule;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.Shift;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/** Weekly working hours per doctor (7 days × MORNING/EVENING). */
@Service
public class DoctorScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorScheduleService.class);

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final SlotService slotService;
    private final CurrentUserService currentUserService;

    public DoctorScheduleService(DoctorScheduleRepository scheduleRepository, DoctorRepository doctorRepository,
                                 SlotService slotService, CurrentUserService currentUserService) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.slotService = slotService;
        this.currentUserService = currentUserService;
    }

    /** Full week for the doctor with this user id; unset days/shifts show the hospital default. */
    public List<DoctorScheduleDTO> getSchedule(Long userId) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        Doctor doctor = doctorByUserId(userId);
        Map<String, DoctorSchedule> stored = new HashMap<>();
        for (DoctorSchedule s : scheduleRepository.findByDoctor_Id(doctor.getId())) {
            stored.put(s.getDayOfWeek() + "-" + s.getShift(), s);
        }
        List<DoctorScheduleDTO> week = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Shift shift : Shift.values()) {
                DoctorSchedule s = stored.get(day + "-" + shift);
                if (s != null) {
                    week.add(new DoctorScheduleDTO(day, shift, s.isWorking(), s.getStartTime(), s.getEndTime(),
                            s.getSlotMinutes(), false));
                } else {
                    ScheduleDefaults.Hours d = ScheduleDefaults.forShift(shift);
                    week.add(new DoctorScheduleDTO(day, shift, d.working(), d.start(), d.end(), d.slotMinutes(), true));
                }
            }
        }
        return week;
    }

    /**
     * Replaces the doctor's weekly schedule. Existing bookings are kept; unbooked future
     * slots are rebuilt from the new hours on the next availability request.
     */
    @Transactional
    public List<DoctorScheduleDTO> updateSchedule(Long userId, List<DoctorScheduleDTO> entries) {
        currentUserService.requireSelfOrRole(userId, Role.ADMIN);
        Doctor doctor = doctorByUserId(userId);
        validate(entries);

        scheduleRepository.deleteByDoctorId(doctor.getId());
        scheduleRepository.flush();
        List<DoctorSchedule> rows = entries.stream().map(dto -> {
            DoctorSchedule row = new DoctorSchedule();
            row.setDoctor(doctor);
            row.setDayOfWeek(dto.getDayOfWeek());
            row.setShift(dto.getShift());
            row.setWorking(dto.isWorking());
            row.setStartTime(dto.getStartTime());
            row.setEndTime(dto.getEndTime());
            row.setSlotMinutes(dto.getSlotMinutes());
            return row;
        }).toList();
        scheduleRepository.saveAll(rows);
        slotService.resetUnusedFutureSlots(doctor.getId());
        logger.info("Schedule updated for doctor {} ({} entries)", doctor.getId(), rows.size());
        return getSchedule(userId);
    }

    static void validate(List<DoctorScheduleDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new BadRequestException("Schedule is empty");
        }
        Map<String, DoctorScheduleDTO> seen = new HashMap<>();
        for (DoctorScheduleDTO e : entries) {
            if (e.getDayOfWeek() == null || e.getShift() == null) {
                throw new BadRequestException("Each entry needs dayOfWeek and shift");
            }
            String key = e.getDayOfWeek() + "-" + e.getShift();
            if (seen.put(key, e) != null) {
                throw new BadRequestException("Duplicate entry for " + label(e));
            }
            if (!e.isWorking()) {
                continue;
            }
            if (e.getStartTime() == null || e.getEndTime() == null) {
                throw new BadRequestException(label(e) + ": start and end time are required");
            }
            if (!e.getStartTime().isBefore(e.getEndTime())) {
                throw new BadRequestException(label(e) + ": start time must be before end time");
            }
            if (e.getSlotMinutes() < 5 || e.getSlotMinutes() > 120) {
                throw new BadRequestException(label(e) + ": slot length must be 5-120 minutes");
            }
            long minutes = java.time.Duration.between(e.getStartTime(), e.getEndTime()).toMinutes();
            if (minutes < e.getSlotMinutes()) {
                throw new BadRequestException(label(e) + ": the shift is shorter than one slot");
            }
        }
        // Morning and evening of the same day must not overlap
        for (DayOfWeek day : DayOfWeek.values()) {
            DoctorScheduleDTO morning = seen.get(day + "-" + Shift.MORNING);
            DoctorScheduleDTO evening = seen.get(day + "-" + Shift.EVENING);
            if (morning != null && evening != null && morning.isWorking() && evening.isWorking()
                    && overlaps(morning.getStartTime(), morning.getEndTime(), evening.getStartTime(), evening.getEndTime())) {
                throw new BadRequestException(pretty(day) + ": morning and evening shifts overlap");
            }
        }
    }

    private static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private static String label(DoctorScheduleDTO e) {
        return pretty(e.getDayOfWeek()) + " " + e.getShift().name().toLowerCase();
    }

    private static String pretty(DayOfWeek day) {
        String name = day.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private Doctor doctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
    }
}
