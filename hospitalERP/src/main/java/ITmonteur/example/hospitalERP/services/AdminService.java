package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.*;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.repositories.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private AuthService authService;
    @Autowired
    private PtInfoRepository ptInfoRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ReceptionistRepository receptionistRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private SmsService smsService;
    @Autowired
    private SlotRepository slotRepository;

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    // -------------------- Create Patient --------------------
    public PtInfoDTO createPatient(RegisterRequestDTO registerRequestDTO) {
        logger.info("Admin creating new patient: {}", registerRequestDTO.getUsername());
        authService.register(registerRequestDTO);
        PtInfo patient = ptInfoRepository.findByUserName(registerRequestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Patient not found after registration"));
        return modelMapper.map(patient, PtInfoDTO.class);
    }

    // -------------------- Create Doctor --------------------
    public DoctorDTO createDoctor(RegisterRequestDTO registerRequestDTO) {
        logger.info("Admin creating new doctor: {}", registerRequestDTO.getUsername());

        // Use AuthService
        authService.register(registerRequestDTO);

        Doctor doctor = doctorRepository.findByUserName(registerRequestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found after registration"));

        return modelMapper.map(doctor, DoctorDTO.class);
    }

    // -------------------- Create Receptionist --------------------
    public ReceptionistDTO createReceptionist(RegisterRequestDTO registerRequestDTO) {
        logger.info("Admin creating new receptionist: {}", registerRequestDTO.getUsername());

        // Use AuthService
        authService.register(registerRequestDTO);

        Receptionist receptionist = receptionistRepository.findByUserName(registerRequestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Receptionist not found after registration"));

        return modelMapper.map(receptionist, ReceptionistDTO.class);
    }

    public LeaveRequest approveLeave(Long leaveId) {
        LeaveRequest leave = this.leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        leave.setStatus(LeaveStatus.APPROVED);
        this.leaveRequestRepository.save(leave);
        if (leave.getRole().equalsIgnoreCase("ROLE_DOCTOR")) {
            Doctor doctor = this.doctorRepository.findByUserId(leave.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found for leave approval"));
            List<Appointment> appointments = this.appointmentRepository
                    .findByDoctor_IdAndDateBetween(
                            doctor.getId(),
                            leave.getStartDate(),
                            leave.getEndDate()
                    );
            logger.info("Cancelling {} appointments for doctor ID {}", appointments.size(), leave.getUser().getId());
            for (Appointment appt : appointments) {
                appt.setStatus(AppointmentStatus.CANCELLED_BY_DOCTOR);
                this.appointmentRepository.save(appt);
                String formattedDate = appt.getDate().toString();
                this.smsService.sendDoctorLeaveCancelSms(
                        appt.getPtInfo().getContactNo(),
                        appt.getPatientName(),
                        appt.getDoctor().getName(),
                        formattedDate
                );
                try {
                    this.emailService.sendDoctorLeaveCancelEmail(
                            appt.getPtInfo().getEmail(),
                            appt.getPatientName(),
                            appt.getDoctor().getName(),
                            formattedDate
                    );
                } catch (Exception e) {
                    logger.error("Failed to send leave cancellation email for appointment {}",
                            appt.getAppointmentID(), e);
                }
            }
            // Disable slots for leave interval
            List<Slot> slots = this.slotRepository
                    .findByDoctor_IdAndDateBetween(doctor.getId(), leave.getStartDate(), leave.getEndDate());
            for (Slot slot : slots) {
                slot.setAvailable(false);  // block slot
                this.slotRepository.save(slot);
            }
            logger.info("Slots disabled between {} and {} for doctor ID {}",
                    leave.getStartDate(), leave.getEndDate(), doctor.getId());
        }
        return leave;
    }



}
