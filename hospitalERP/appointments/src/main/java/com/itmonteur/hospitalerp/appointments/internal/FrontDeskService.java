package com.itmonteur.hospitalerp.appointments.internal;

import com.itmonteur.hospitalerp.appointments.AppointmentDTO;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.scheduling.SlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Walk-in patients at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md): find patients by phone, a doctor's next
 * free slots, and register-and-book in one transaction - if the slot is gone, no patient record is left behind.
 */
@Service
public class FrontDeskService {

    public static final int MAX_FREE_SLOTS = 20;
    private static final int MAX_AGE = 130;

    private final PtInfoService ptInfoService;
    private final AppointmentService appointmentService;
    private final SlotService slotService;

    public FrontDeskService(PtInfoService ptInfoService, AppointmentService appointmentService, SlotService slotService) {
        this.ptInfoService = ptInfoService;
        this.appointmentService = appointmentService;
        this.slotService = slotService;
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
        return new WalkInBookingDTO(patient.getPatientId(), existing == null, appointment);
    }
}
