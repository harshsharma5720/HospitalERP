package ITmonteur.example.hospitalERP.repositories;

import ITmonteur.example.hospitalERP.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the custom JPQL queries against an in-memory H2 database. */
@DataJpaTest
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:test", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class RepositoryQueriesTest {

    @Autowired private TestEntityManager em;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private SlotRepository slotRepository;

    private Doctor doctor;
    private PtInfo patient;
    private User doctorUser;
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        doctorUser = user("drrao", Role.DOCTOR);
        doctor = new Doctor();
        doctor.setName("Dr Rao");
        doctor.setEmail("drrao@example.com");
        doctor.setPhoneNumber("+911111111111");
        doctor.setSpecialist(Specialist.CARDIOLOGY);
        doctor.setUser(doctorUser);
        em.persist(doctor);

        patient = new PtInfo();
        patient.setPatientName("Asha");
        patient.setEmail("asha@example.com");
        patient.setGender(Gender.FEMALE);
        patient.setUser(user("asha", Role.PATIENT));
        em.persist(patient);
    }

    @Test
    void pendingAndCompletedAreDerivedFromStatusIncludingLegacyRows() {
        Appointment scheduled = appointment(AppointmentStatus.SCHEDULED, LocalTime.of(9, 0));
        Appointment completed = appointment(AppointmentStatus.COMPLETED, LocalTime.of(9, 10));
        appointment(AppointmentStatus.CANCELLED_BY_DOCTOR, LocalTime.of(9, 20));
        Appointment legacy = appointment(AppointmentStatus.SCHEDULED, LocalTime.of(9, 30));
        em.flush();
        // Rows written before status and isCompleted were kept in sync
        em.getEntityManager().createNativeQuery("UPDATE appointments SET is_completed = TRUE WHERE appointmentid = ?1")
                .setParameter(1, legacy.getAppointmentID()).executeUpdate();
        em.clear();

        assertThat(appointmentRepository.findPendingByPatientId(patient.getPatientId()))
                .extracting(Appointment::getAppointmentID).containsExactly(scheduled.getAppointmentID());
        assertThat(appointmentRepository.findCompletedByPatientId(patient.getPatientId()))
                .extracting(Appointment::getAppointmentID)
                .containsExactlyInAnyOrder(completed.getAppointmentID(), legacy.getAppointmentID());
        assertThat(appointmentRepository.countPendingByDoctorId(doctor.getId())).isEqualTo(1);
        assertThat(appointmentRepository.countCompletedByDoctorId(doctor.getId())).isEqualTo(2);
        assertThat(appointmentRepository.findAllPending()).hasSize(1);
        assertThat(appointmentRepository.findPendingByDoctorId(doctor.getId())).hasSize(1);
    }

    @Test
    void findByDoctorNameWorks() {
        appointment(AppointmentStatus.SCHEDULED, LocalTime.of(10, 0));
        em.flush();

        assertThat(appointmentRepository.findByDoctor_Name("Dr Rao")).hasSize(1);
        assertThat(appointmentRepository.findByDoctor_Name("Nobody")).isEmpty();
    }

    @Test
    void clearRelativeKeepsTheAppointment() {
        PtRelative relative = new PtRelative();
        relative.setName("Ravi");
        relative.setRelationship(RelationShip.SON);
        relative.setPtInfo(patient);
        em.persist(relative);
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED, LocalTime.of(11, 0));
        appointment.setRelative(relative);
        em.flush();

        appointmentRepository.clearRelative(relative.getId());
        em.clear();

        Appointment reloaded = em.find(Appointment.class, appointment.getAppointmentID());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getRelative()).isNull();
    }

    @Test
    void leaveOverlapIgnoresRejectedLeaves() {
        leave(LeaveStatus.REJECTED, tomorrow, tomorrow.plusDays(2));
        assertThat(leaveRequestRepository.existsOverlapping(doctorUser.getId(), tomorrow, tomorrow, null)).isFalse();

        LeaveRequest pending = leave(LeaveStatus.PENDING, tomorrow, tomorrow.plusDays(2));
        assertThat(leaveRequestRepository.existsOverlapping(doctorUser.getId(), tomorrow.plusDays(1), tomorrow.plusDays(5), null)).isTrue();
        // Updating the same request must not conflict with itself
        assertThat(leaveRequestRepository.existsOverlapping(doctorUser.getId(), tomorrow, tomorrow, pending.getId())).isFalse();
    }

    @Test
    void approvedLeaveCoversItsDates() {
        leave(LeaveStatus.APPROVED, tomorrow, tomorrow.plusDays(1));

        assertThat(leaveRequestRepository.isOnApprovedLeave(doctorUser.getId(), tomorrow)).isTrue();
        assertThat(leaveRequestRepository.isOnApprovedLeave(doctorUser.getId(), tomorrow.plusDays(2))).isFalse();
    }

    @Test
    void slotLockQueryAndDoctorCleanupWork() {
        Slot slot = new Slot(tomorrow, LocalTime.of(9, 0), LocalTime.of(9, 10), doctor, Shift.MORNING);
        em.persist(slot);
        em.flush();

        assertThat(slotRepository.findByIdForUpdate(slot.getId())).isPresent();
        slotRepository.deleteByDoctorId(doctor.getId());
        em.clear();
        assertThat(slotRepository.findById(slot.getId())).isEmpty();
    }

    private Appointment appointment(AppointmentStatus status, LocalTime start) {
        Slot slot = new Slot(tomorrow, start, start.plusMinutes(10), doctor, Shift.MORNING);
        em.persist(slot);
        Appointment appointment = new Appointment();
        appointment.setPatientName("Asha");
        appointment.setGender(Gender.FEMALE);
        appointment.setShift(Shift.MORNING);
        appointment.setDate(tomorrow);
        appointment.setDoctor(doctor);
        appointment.setPtInfo(patient);
        appointment.setSlot(slot);
        appointment.setStatus(status);
        return em.persist(appointment);
    }

    private LeaveRequest leave(LeaveStatus status, LocalDate start, LocalDate end) {
        LeaveRequest leave = new LeaveRequest();
        leave.setUser(doctorUser);
        leave.setStatus(status);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setReason("test");
        return em.persist(leave);
    }

    private User user(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("x");
        user.setRole(role);
        user.setPhoneNumber("+919999999999");
        return em.persist(user);
    }
}
