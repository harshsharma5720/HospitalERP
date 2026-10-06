package com.itmonteur.hospitalerp;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentStatistics;
import com.itmonteur.hospitalerp.appointments.AppointmentStatus;
import com.itmonteur.hospitalerp.appointments.DailyAppointmentCounts;
import com.itmonteur.hospitalerp.appointments.DoctorAppointmentCount;
import com.itmonteur.hospitalerp.appointments.SpecializationCount;
import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.scheduling.Shift;
import com.itmonteur.hospitalerp.scheduling.Slot;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.LeaveRequest;
import com.itmonteur.hospitalerp.staff.LeaveStatus;
import com.itmonteur.hospitalerp.staff.Specialist;
import com.itmonteur.hospitalerp.staff.internal.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The admin dashboard's count queries (docs/ADMIN_DASHBOARD_PLAN.md, step B.2) on H2 with known data.
 * H2 schema from the entities, as in RepositoryQueriesTest.
 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:dashboard", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import({AppointmentStatistics.class, UserService.class})
class DashboardStatisticsTest {

    @Autowired private TestEntityManager em;
    @Autowired private AppointmentStatistics statistics;
    @Autowired private UserService userService;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    private final LocalDate today = LocalDate.now();
    private Doctor rao;    // cardiology
    private Doctor mehta;  // dentistry
    private PtInfo patient;
    private int slotMinute;

    @BeforeEach
    void setUp() {
        rao = doctor("drrao", "Dr Rao", Specialist.CARDIOLOGY);
        mehta = doctor("drmehta", "Dr Mehta", Specialist.DENTISTRY);
        patient = new PtInfo();
        patient.setPatientName("Asha");
        patient.setEmail("asha@example.com");
        patient.setGender(Gender.FEMALE);
        patient.setUser(user("asha", Role.PATIENT, null));
        em.persist(patient);
    }

    @Test
    void appointmentsPerDayAreGroupedByOutcome() {
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate yesterday = today.minusDays(1);
        appointment(rao, twoDaysAgo, AppointmentStatus.COMPLETED);
        appointment(rao, twoDaysAgo, AppointmentStatus.SCHEDULED);          // never completed: open
        appointment(rao, twoDaysAgo, AppointmentStatus.CANCELLED_BY_PATIENT);
        Appointment legacy = appointment(rao, yesterday, AppointmentStatus.SCHEDULED);
        appointment(rao, yesterday, AppointmentStatus.CANCELLED_BY_DOCTOR);
        appointment(rao, today, AppointmentStatus.CONFIRMED);
        appointment(rao, today.minusDays(10), AppointmentStatus.COMPLETED); // outside the period
        em.flush();
        // An old row: completed flag set, status never updated -> counts as completed, as everywhere else
        em.getEntityManager().createNativeQuery("UPDATE appointments SET is_completed = TRUE WHERE appointmentid = ?1")
                .setParameter(1, legacy.getAppointmentID()).executeUpdate();
        em.clear();

        assertThat(statistics.perDay(today.minusDays(3), today)).containsExactly(
                new DailyAppointmentCounts(today.minusDays(3), 0, 0, 0, 0), // a day without appointments
                new DailyAppointmentCounts(twoDaysAgo, 1, 1, 1, 0),
                new DailyAppointmentCounts(yesterday, 1, 0, 0, 1),
                new DailyAppointmentCounts(today, 0, 1, 0, 0));
        assertThat(statistics.perDay(twoDaysAgo, twoDaysAgo).get(0).total()).isEqualTo(3);
    }

    @Test
    void busiestSpecializationsAndDoctorsLeaveOutCancelledAppointments() {
        appointment(rao, today, AppointmentStatus.COMPLETED);
        appointment(rao, today, AppointmentStatus.COMPLETED);
        appointment(rao, today, AppointmentStatus.SCHEDULED);
        appointment(rao, today, AppointmentStatus.CANCELLED_BY_PATIENT);
        appointment(rao, today, AppointmentStatus.CANCELLED_BY_DOCTOR);
        appointment(mehta, today, AppointmentStatus.SCHEDULED);
        appointment(mehta, today.minusDays(40), AppointmentStatus.COMPLETED); // outside the period
        em.flush();
        em.clear();
        LocalDate from = today.minusDays(29);

        assertThat(statistics.busiestSpecializations(from, today, 5)).containsExactly(
                new SpecializationCount(Specialist.CARDIOLOGY, 3),
                new SpecializationCount(Specialist.DENTISTRY, 1));
        assertThat(statistics.busiestDoctors(from, today, 5)).containsExactly(
                new DoctorAppointmentCount(rao.getId(), "Dr Rao", Specialist.CARDIOLOGY, 3, 2),
                new DoctorAppointmentCount(mehta.getId(), "Dr Mehta", Specialist.DENTISTRY, 1, 0));
        assertThat(statistics.busiestDoctors(from, today, 1)).extracting(DoctorAppointmentCount::doctorName)
                .containsExactly("Dr Rao");
        assertThat(statistics.busiestSpecializations(today.minusDays(60), today.minusDays(50), 5)).isEmpty();
    }

    @Test
    void activeAccountsPerRoleAndNewAccountsPerDay() {
        // From setUp: drrao, drmehta, asha - all active, created before users.created_at existed (no time)
        LocalDateTime yesterdayMorning = today.minusDays(1).atTime(9, 30);
        user("ravi", Role.PATIENT, yesterdayMorning);
        user("meera", Role.PATIENT, today.atTime(8, 0));
        User closed = user("gone", Role.PATIENT, today.atTime(11, 0));
        closed.setActive(false); // still a new account of that day, but not active
        user("old", Role.PATIENT, today.minusDays(5).atTime(12, 0)); // outside the period
        user("desk", Role.RECEPTIONIST, today.atTime(10, 0));
        em.flush();

        Map<Role, Long> active = userService.countActiveAccountsPerRole();
        assertThat(active).containsEntry(Role.PATIENT, 4L) // asha, ravi, meera, old
                .containsEntry(Role.DOCTOR, 2L)
                .containsEntry(Role.RECEPTIONIST, 1L)
                .containsEntry(Role.ADMIN, 0L);
        assertThat(userService.countNewAccountsPerDay(Role.PATIENT, today.minusDays(1), today))
                .containsExactly(Map.entry(today.minusDays(1), 1L), Map.entry(today, 2L));
        assertThat(userService.countNewAccountsPerDay(Role.RECEPTIONIST, today, today)).containsExactly(Map.entry(today, 1L));
        assertThat(userService.firstAccountCreationTime()).contains(today.minusDays(5).atTime(12, 0));
    }

    @Test
    void doctorsOnLeaveCountsEachActiveDoctorOnceAndOnlyApprovedLeaves() {
        leave(rao.getUser(), "DOCTOR", today.minusDays(1), today.plusDays(1), LeaveStatus.APPROVED);
        leave(rao.getUser(), "DOCTOR", today, today, LeaveStatus.APPROVED);                // same doctor again
        leave(mehta.getUser(), "DOCTOR", today, today, LeaveStatus.PENDING);               // not approved
        leave(mehta.getUser(), "DOCTOR", today.plusDays(3), today.plusDays(4), LeaveStatus.APPROVED); // later
        leave(user("desk", Role.RECEPTIONIST, null), "RECEPTIONIST", today, today, LeaveStatus.APPROVED);
        Doctor gone = doctor("drgone", "Dr Gone", Specialist.NEUROLOGY);
        gone.getUser().setActive(false);
        leave(gone.getUser(), "DOCTOR", today, today, LeaveStatus.APPROVED);              // deactivated
        em.flush();

        assertThat(leaveRequestRepository.countDoctorsOnApprovedLeave(today)).isEqualTo(1);
        assertThat(leaveRequestRepository.countDoctorsOnApprovedLeave(today.plusDays(3))).isEqualTo(1);
        assertThat(leaveRequestRepository.countDoctorsOnApprovedLeave(today.plusDays(10))).isZero();
    }

    // ------------------------------------------------------------------ data

    private Doctor doctor(String username, String name, Specialist specialist) {
        Doctor doctor = new Doctor();
        doctor.setName(name);
        doctor.setEmail(username + "@example.com");
        doctor.setPhoneNumber("+911111111111");
        doctor.setSpecialist(specialist);
        doctor.setUser(user(username, Role.DOCTOR, null));
        return em.persist(doctor);
    }

    private User user(String username, Role role, LocalDateTime createdAt) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("x");
        user.setRole(role);
        user.setPhoneNumber("+919999999999");
        user.setCreatedAt(createdAt);
        return em.persist(user);
    }

    private Appointment appointment(Doctor doctor, LocalDate date, AppointmentStatus status) {
        LocalTime start = LocalTime.of(9, 0).plusMinutes(10L * slotMinute++);
        Slot slot = new Slot(date, start, start.plusMinutes(10), doctor, Shift.MORNING);
        em.persist(slot);
        Appointment appointment = new Appointment();
        appointment.setPatientName("Asha");
        appointment.setGender(Gender.FEMALE);
        appointment.setShift(Shift.MORNING);
        appointment.setDate(date);
        appointment.setDoctor(doctor);
        appointment.setPtInfo(patient);
        appointment.setSlot(slot);
        appointment.setStatus(status);
        return em.persist(appointment);
    }

    private void leave(User user, String role, LocalDate start, LocalDate end, LeaveStatus status) {
        LeaveRequest leave = new LeaveRequest();
        leave.setUser(user);
        leave.setRole(role);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setReason("test");
        leave.setStatus(status);
        em.persist(leave);
    }
}
