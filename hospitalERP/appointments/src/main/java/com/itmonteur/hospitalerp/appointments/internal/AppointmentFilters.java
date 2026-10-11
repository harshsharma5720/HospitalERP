package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentStatus;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.LikePattern;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The front desk's appointment list filters as a JPA query (docs/PATIENT_SEARCH_PLAN.md). The status groups use the
 * module's usual rules (AppointmentRepository): "completed" also counts old rows with only the is_completed flag.
 */
final class AppointmentFilters {

    enum StatusGroup { ALL, UPCOMING, COMPLETED, CANCELLED }

    private AppointmentFilters() {}

    static Specification<Appointment> matching(AppointmentListQuery query) {
        StatusGroup status = statusGroup(query.status());
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new BadRequestException("The start date must not be after the end date");
        }
        String patient = query.patient() == null ? "" : query.patient().trim().replaceAll("\\s+", " ");
        return (root, criteria, cb) -> {
            List<Predicate> where = new ArrayList<>();
            Path<LocalDate> date = root.get("date");
            if (query.from() != null) where.add(cb.greaterThanOrEqualTo(date, query.from()));
            if (query.to() != null) where.add(cb.lessThanOrEqualTo(date, query.to()));
            if (query.doctorId() != null) where.add(cb.equal(root.get("doctor").get("id"), query.doctorId()));
            if (query.patientId() != null) where.add(cb.equal(root.get("ptInfo").get("patientId"), query.patientId()));
            if (!patient.isEmpty()) {
                where.add(cb.like(cb.lower(root.get("patientName")), LikePattern.containsIgnoringCase(patient),
                        LikePattern.ESCAPE));
            }
            Path<AppointmentStatus> state = root.get("status");
            Path<Boolean> completedFlag = root.get("isCompleted");
            switch (status) {
                case UPCOMING -> where.add(cb.and(cb.isFalse(completedFlag),
                        state.in(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)));
                case COMPLETED -> where.add(cb.or(cb.isTrue(completedFlag), cb.equal(state, AppointmentStatus.COMPLETED)));
                case CANCELLED -> where.add(state.in(AppointmentStatus.CANCELLED_BY_DOCTOR,
                        AppointmentStatus.CANCELLED_BY_PATIENT));
                case ALL -> { }
            }
            return cb.and(where.toArray(new Predicate[0]));
        };
    }

    /** By date and time - a visit whose slot row is gone still counts (outer join) - oldest or newest first. */
    static Sort order(String order) {
        String value = order == null ? "asc" : order.trim().toLowerCase(Locale.ROOT);
        Sort.Direction direction = switch (value) {
            case "", "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException("The order must be asc or desc");
        };
        return Sort.by(direction, "date", "slot.startTime", "appointmentID");
    }

    private static StatusGroup statusGroup(String status) {
        if (status == null || status.isBlank()) {
            return StatusGroup.ALL;
        }
        try {
            return StatusGroup.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The status must be upcoming, completed, cancelled or all");
        }
    }
}
