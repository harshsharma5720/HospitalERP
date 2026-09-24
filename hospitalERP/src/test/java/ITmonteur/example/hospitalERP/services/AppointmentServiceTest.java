package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.ConflictException;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.PtRelativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PtInfoRepository ptInfoRepository;
    @Mock private PtRelativeRepository ptRelativeRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private SlotService slotService;
    @Mock private NotificationService notificationService;
    @Mock private CurrentUserService currentUserService;

    private AppointmentService service;
    private PtInfo me;
    private Slot slot;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, ptInfoRepository, ptRelativeRepository,
                doctorRepository, slotService, notificationService, currentUserService);
        me = patient(10L, 100L, "Asha");
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr Rao");
        slot = new Slot(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(9, 10), doctor, Shift.MORNING);
        lenient().when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void patientBookingIgnoresPtInfoIdFromRequest() {
        when(currentUserService.isStaff()).thenReturn(false);
        when(currentUserService.hasRole(Role.PATIENT)).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(ptInfoRepository.findByUser_Id(100L)).thenReturn(Optional.of(me));
        when(slotService.lockAndBook(55L)).thenReturn(slot);

        AppointmentDTO request = new AppointmentDTO();
        request.setSlotId(55L);
        request.setPtInfoId(999L); // someone else's patient id

        AppointmentDTO result = service.createAppointment(request);

        assertThat(result.getPtInfoId()).isEqualTo(10L);
        assertThat(result.getDoctorName()).isEqualTo("Dr Rao");
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(ptInfoRepository, never()).findById(999L);
        verify(notificationService).appointmentBooked(any());
    }

    @Test
    void bookingForSomeoneElsesRelativeIsRejected() {
        when(currentUserService.isStaff()).thenReturn(false);
        when(currentUserService.hasRole(Role.PATIENT)).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(ptInfoRepository.findByUser_Id(100L)).thenReturn(Optional.of(me));
        PtRelative strangersRelative = new PtRelative();
        strangersRelative.setPtInfo(patient(11L, 101L, "Other"));
        when(ptRelativeRepository.findById(3L)).thenReturn(Optional.of(strangersRelative));

        AppointmentDTO request = new AppointmentDTO();
        request.setSlotId(55L);
        request.setRelativeId(3L);

        assertThatThrownBy(() -> service.createAppointment(request)).isInstanceOf(ForbiddenException.class);
        verify(slotService, never()).lockAndBook(any());
    }

    @Test
    void patientCannotCancelAnotherPatientsAppointment() {
        Appointment others = appointmentOf(patient(11L, 101L, "Other"));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(others));
        when(currentUserService.isStaff()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(100L);

        assertThatThrownBy(() -> service.cancelAppointment(1L)).isInstanceOf(ForbiddenException.class);
        verify(slotService, never()).releaseSlot(any());
    }

    @Test
    void cancelKeepsRecordAndReleasesSlot() {
        Appointment mine = appointmentOf(me);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mine));
        when(currentUserService.isStaff()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(100L);

        AppointmentDTO result = service.cancelAppointment(1L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED_BY_PATIENT);
        verify(appointmentRepository, never()).deleteById(any());
        verify(slotService).releaseSlot(slot);
    }

    @Test
    void rescheduleBooksNewSlotBeforeReleasingOldOne() {
        Appointment mine = appointmentOf(me);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mine));
        when(currentUserService.isStaff()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        Slot newSlot = new Slot(LocalDate.now().plusDays(2), LocalTime.of(15, 0), LocalTime.of(15, 10),
                slot.getDoctor(), Shift.EVENING);
        when(slotService.lockAndBook(56L)).thenReturn(newSlot);

        AppointmentDTO request = new AppointmentDTO();
        request.setSlotId(56L);
        service.updateAppointmentById(1L, request);

        InOrder order = inOrder(slotService);
        order.verify(slotService).lockAndBook(56L);
        order.verify(slotService).releaseSlot(slot);
        assertThat(mine.getSlot()).isSameAs(newSlot);
        assertThat(mine.getShift()).isEqualTo(Shift.EVENING);
    }

    @Test
    void rescheduleToTakenSlotKeepsTheOldSlot() {
        Appointment mine = appointmentOf(me);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mine));
        when(currentUserService.isStaff()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(slotService.lockAndBook(56L)).thenThrow(new ConflictException("taken"));

        AppointmentDTO request = new AppointmentDTO();
        request.setSlotId(56L);

        assertThatThrownBy(() -> service.updateAppointmentById(1L, request)).isInstanceOf(ConflictException.class);
        verify(slotService, never()).releaseSlot(any());
        assertThat(mine.getSlot()).isSameAs(slot);
    }

    private Appointment appointmentOf(PtInfo owner) {
        Appointment appointment = new Appointment();
        appointment.setPtInfo(owner);
        appointment.setSlot(slot);
        appointment.setDoctor(slot.getDoctor());
        appointment.setDate(slot.getDate());
        appointment.setShift(slot.getShift());
        appointment.setPatientName(owner.getPatientName());
        appointment.setGender(Gender.OTHER);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }

    private static PtInfo patient(Long patientId, Long userId, String name) {
        User user = new User();
        user.setId(userId);
        PtInfo ptInfo = new PtInfo();
        ptInfo.setPatientId(patientId);
        ptInfo.setPatientName(name);
        ptInfo.setGender(Gender.FEMALE);
        ptInfo.setUser(user);
        return ptInfo;
    }
}
