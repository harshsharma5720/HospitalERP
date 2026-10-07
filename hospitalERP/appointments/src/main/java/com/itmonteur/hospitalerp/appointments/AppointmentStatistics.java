package com.itmonteur.hospitalerp.appointments;

import com.itmonteur.hospitalerp.appointments.internal.AppointmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Appointment figures for the admin dashboard (docs/ADMIN_DASHBOARD_PLAN.md), all by appointment date and
 * computed with grouped count queries. "Completed" follows the same rule as the rest of the module: status
 * COMPLETED, or an old row with the is_completed flag set.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentStatistics {

    private final AppointmentRepository appointmentRepository;

    public AppointmentStatistics(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /** One entry per day from {@code from} to {@code to} (both included), days without appointments as zeros. */
    public List<DailyAppointmentCounts> perDay(LocalDate from, LocalDate to) {
        Map<LocalDate, long[]> days = new TreeMap<>(); // completed, open, cancelled by patient, cancelled by doctor
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            days.put(day, new long[4]);
        }
        for (Object[] row : appointmentRepository.countPerDayStatusAndCompletedFlag(from, to)) {
            long[] counts = days.get((LocalDate) row[0]);
            AppointmentStatus status = (AppointmentStatus) row[1];
            boolean completedFlag = Boolean.TRUE.equals(row[2]);
            long count = ((Number) row[3]).longValue();
            if (completedFlag || status == AppointmentStatus.COMPLETED) {
                counts[0] += count;
            } else if (status == AppointmentStatus.CANCELLED_BY_PATIENT) {
                counts[2] += count;
            } else if (status == AppointmentStatus.CANCELLED_BY_DOCTOR) {
                counts[3] += count;
            } else {
                counts[1] += count; // SCHEDULED, CONFIRMED
            }
        }
        List<DailyAppointmentCounts> result = new ArrayList<>();
        days.forEach((day, c) -> result.add(new DailyAppointmentCounts(day, c[0], c[1], c[2], c[3])));
        return result;
    }

    /** Specializations with the most appointments (cancelled ones not counted), most first. */
    public List<SpecializationCount> busiestSpecializations(LocalDate from, LocalDate to, int limit) {
        return appointmentRepository.countPerSpecialization(from, to, PageRequest.of(0, limit));
    }

    /** Doctors with the most appointments (cancelled ones not counted), most first. */
    public List<DoctorAppointmentCount> busiestDoctors(LocalDate from, LocalDate to, int limit) {
        return appointmentRepository.countPerDoctor(from, to, PageRequest.of(0, limit));
    }
}
