package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.PageResult;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * The front desk. Walk-in patients (docs/WALK_IN_REGISTRATION_PLAN.md): find patients by phone, a doctor's next free
 * slots, and register-and-book in one transaction - if the slot is gone, no patient record is left behind. Patient
 * search and the appointment list (docs/PATIENT_SEARCH_PLAN.md): one search box, one patient's details, the filtered
 * appointment list - each a page at a time.
 */
@Service
public class FrontDeskService {

    public static final int MAX_FREE_SLOTS = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_AGE = 130;

    private final PtInfoService ptInfoService;
    private final AppointmentService appointmentService;
    private final SlotService slotService;
    private final AppointmentRepository appointmentRepository;
    private final QueueService queueService;
    private final Clock clock;

    public FrontDeskService(PtInfoService ptInfoService, AppointmentService appointmentService, SlotService slotService,
                            AppointmentRepository appointmentRepository, QueueService queueService, Clock clock) {
        this.ptInfoService = ptInfoService;
        this.appointmentService = appointmentService;
        this.slotService = slotService;
        this.appointmentRepository = appointmentRepository;
        this.queueService = queueService;
        this.clock = clock;
    }

    /** The one search box: a name, a phone number or a patient ID; empty lists everyone (audited). */
    public PageResult<PtInfoDTO> searchPatients(String query, int page, int size) {
        return ptInfoService.searchPatients(query, page, size);
    }

    /** One patient's details (audited as a profile view). */
    public PtInfoDTO patient(long patientId) {
        return ptInfoService.getPatient(patientId);
    }

    /** The appointment list: every filter optional, by date and time, one page (from 0) of at most 100. */
    @Transactional(readOnly = true)
    public PageResult<AppointmentDTO> listAppointments(AppointmentListQuery query, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Ask for page 0 or later, with 1 to " + MAX_PAGE_SIZE + " appointments per page");
        }
        Page<Appointment> found = appointmentRepository.findAll(AppointmentFilters.matching(query),
                PageRequest.of(page, size, AppointmentFilters.order(query.order())));
        return PageResult.of(found.getContent().stream().map(AppointmentMapper::toDTO).toList(),
                page, size, found.getTotalElements());
    }

    /** Patients with this phone number (recorded in the audit log). */
    public List<PtInfoDTO> findPatientsByPhone(String phone) {
        return ptInfoService.findByPhone(phone);
    }

    public List<FreeSlotDTO> nextFreeSlots(Long doctorId, int limit) {
        if (limit < 1 || limit > MAX_FREE_SLOTS) {
            throw new BadRequestException("Ask for 1 to " + MAX_FREE_SLOTS + " slots");
        }
        return slotService.nextFreeSlots(doctorId, limit).stream().map(FreeSlotDTO::of).toList();
    }

    /**
     * Books the slot for an existing patient or for a new walk-in patient, who is registered first. One transaction:
     * a refused booking (slot taken meanwhile, doctor deactivated, ...) registers no one. The usual booking
     * notifications and audit entries follow the commit.
     */
    @Transactional
    public WalkInBookingDTO registerAndBook(WalkInBookingRequest request) {
        if ((request.patientId() == null) == (request.newPatient() == null)) {
            throw new BadRequestException("Choose an existing patient or enter a new one");
        }
        if (request.slotId() == null) {
            throw new BadRequestException("Please select a slot");
        }
        if (request.age() != null && (request.age() < 0 || request.age() > MAX_AGE)) {
            throw new BadRequestException("Please enter an age between 0 and " + MAX_AGE);
        }
        PtInfo existing = request.patientId() == null ? null : ptInfoService.findPatientEntity(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        LocalDate dob = existing != null ? existing.getDob() : request.newPatient().dob();
        if (request.age() == null && dob == null) {
            throw new BadRequestException("Please enter the patient's age (or date of birth)");
        }
        PtInfo patient = existing != null ? existing : ptInfoService.registerWalkIn(request.newPatient());

        AppointmentDTO booking = new AppointmentDTO();
        booking.setPtInfoId(patient.getPatientId());
        booking.setSlotId(request.slotId());
        booking.setAge(request.age() != null ? request.age() : 0); // 0: from the date of birth
        booking.setMessage(request.message() == null || request.message().isBlank() ? null : request.message().trim());
        AppointmentDTO appointment = appointmentService.createAppointment(booking);
        if (LocalDate.now(clock).equals(appointment.getDate())) {
            // At the desk already: in today's queue straight away (docs/TODAYS_QUEUE_PLAN.md)
            appointment = queueService.checkIn(appointment.getAppointmentID());
        }
        return new WalkInBookingDTO(patient.getPatientId(), existing == null, appointment);
    }
}
