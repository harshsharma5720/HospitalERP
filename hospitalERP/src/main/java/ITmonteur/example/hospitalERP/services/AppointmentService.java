package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private PtInfoRepository ptInfoRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private JWTService jwtService;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private SlotService slotService;
    @Autowired
    private EmailService emailService;

    public List<AppointmentDTO> getAllAppointments(){
        logger.info("Fetching all appointments");
        List<Appointment> appointments = this.appointmentRepository.findAll();
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("Total appointments fetched: {}", appointmentDTOList.size());
        return appointmentDTOList;
    }

    public List<AppointmentDTO>  getAppointmentsByToken(String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = this.jwtService.extractUserId(jwt);
        Optional<PtInfo> patient = this.ptInfoRepository.findByUser_Id(userId);
        if (patient.isEmpty()) {
            throw new RuntimeException("No patient found for this user!");
        }
        Long patientId = patient.get().getPatientId();
        logger.info("Fetching appointment with ID: {}", patientId);
        List<Appointment> appointmentList = this.appointmentRepository.findByPtInfo_PatientId(patientId);
        return appointmentList.stream()
                .map(appointment -> modelMapper.map(appointment,AppointmentDTO.class))
                .collect(Collectors.toList());
    }

    public AppointmentDTO getAppointmentByID(long appointmentID){
        logger.info("Fetching appointment with ID: {}", appointmentID);
        Appointment appointment = this.appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> {
                    logger.warn("Appointment not found with ID: {}", appointmentID);
                    return new ResourceNotFoundException("Patient","appointmentID", appointmentID);
                });
        AppointmentDTO appointmentDTO = this.convertToDTO(appointment);
        return appointmentDTO;
    }

    public boolean createAppointment(AppointmentDTO appointmentDTO) {
        Slot slot = slotRepository.findById(appointmentDTO.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", appointmentDTO.getSlotId()));
        if (!slot.isAvailable()) {
            throw new RuntimeException("Slot already booked");
        }
        Appointment appointment = convertToEntities(appointmentDTO);
        PtInfo ptInfo = ptInfoRepository.findById(appointmentDTO.getPtInfoId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", appointmentDTO.getPtInfoId()));
        appointment.setPtInfo(ptInfo);
        Doctor doctor = slot.getDoctor();
        appointment.setDoctor(doctor);
        appointment.setShift(slot.getShift());
        appointment.setDate(slot.getDate());
        appointment.setSlot(slot);
        slotService.bookSlot(slot.getId());
        appointmentRepository.save(appointment);
        logger.info("Appointment created successfully for slot {}", slot.getId());
        try {
            emailService.sendBookingEmail(
                    ptInfo.getEmail(),
                    ptInfo.getPatientName(),
                    doctor.getName(),
                    slot.getDate().toString(),
                    slot.getStartTime() + " - " + slot.getEndTime()
            );
            logger.info("Appointment email sent to {}", ptInfo.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send appointment email: {}", e.getMessage());
        }

        return true;
    }

    public List<AppointmentDTO> getAllPatientCompletedAppointments(Long userId){
        logger.info("Fetching completed appointments for patient ID: {}", userId);
        Optional<PtInfo> patient = this.ptInfoRepository.findByUser_Id(userId);
        if (patient.isEmpty()) {
            throw new ResourceNotFoundException("Patient","userId", userId);
        }
        Long patientId = patient.get().getPatientId();
        List<Appointment> appointments = this.appointmentRepository.findByPtInfo_PatientIdAndIsCompletedTrue(patientId);
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("Total completed appointments fetched for patient ID {}: {}", patientId, appointmentDTOList.size());
        return appointmentDTOList;

    }

    public List<AppointmentDTO> getAllPatientPendingAppointments(Long userId){
        logger.info("Fetching pending appointments for patient ID: {}", userId);
        Optional<PtInfo> patient = this.ptInfoRepository.findByUser_Id(userId);
        if (patient.isEmpty()) {
            throw new ResourceNotFoundException("Patient","userId", userId);
        }
        Long patientId = patient.get().getPatientId();
        List<Appointment> appointments = this.appointmentRepository.findByPtInfo_PatientIdAndIsCompletedFalse(patientId);
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("Total pending appointments fetched for patient ID {}: {}", patientId, appointmentDTOList.size());
        return appointmentDTOList;
    }

    public List<AppointmentDTO> getAppointmentsForDoctor(String token) {

        logger.info("Extracting JWT token to fetch doctor appointments");
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtService.extractUserId(jwt);
        logger.info("Extracted userId from token: {}", userId);
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logger.error("No doctor found for userId: {}", userId);
                    return new ResourceNotFoundException("Doctor ","userId",userId);
                });
        logger.info("Doctor found: {}, fetching appointments", doctor.getName());
        List<Appointment> appointments = appointmentRepository.findByDoctor_Id(doctor.getId());
        logger.info("Total appointments found for doctor {}: {}", doctor.getName(), appointments.size());
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(app -> modelMapper.map(app, AppointmentDTO.class))
                .collect(Collectors.toList());
        return appointmentDTOList;
    }

    public boolean deleteAppointmentByID(long appointmentID){
        logger.info("Deleting appointment with ID: {}", appointmentID);

        Appointment appointment = this.appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> {
                    logger.warn("Appointment not found with ID: {}", appointmentID);
                    return new ResourceNotFoundException("Appointment", "id", appointmentID);
                });

        PtInfo ptInfo = appointment.getPtInfo();
        Doctor doctor = appointment.getDoctor();
        Slot slot = appointment.getSlot();

        String patientEmail = ptInfo.getEmail();
        String patientName = ptInfo.getPatientName();
        String doctorName = doctor.getName();
        String date = slot.getDate().toString();
        String time = slot.getStartTime() + " - " + slot.getEndTime();

        // Delete appointment
        this.appointmentRepository.deleteById(appointmentID);
        logger.info("Appointment deleted successfully with ID: {}", appointmentID);

        // Release slot
        slotService.releaseSlot(slot.getId());
        try {
            emailService.sendDeleteEmail(
                    patientEmail,
                    patientName,
                    doctorName,
                    date,
                    time
            );
            logger.info("Delete email sent successfully to {}", patientEmail);
        } catch (Exception e) {
            logger.error("Failed to send delete email: {}", e.getMessage());
        }

        return true;
    }

    public boolean deleteAllAppointments(){
        long count = appointmentRepository.count();
        if (count == 0) {
            logger.warn("No appointments to delete");
            return false;
        }
        this.appointmentRepository.deleteAll();
        logger.info("All appointments deleted successfully");
        return true;
    }

    public AppointmentDTO updateAppointmentById(long appointmentID, AppointmentDTO appointmentDTO) {
        logger.info("Updating appointment with ID: {}", appointmentID);
        Appointment appointment = this.appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> {
                    logger.warn("Appointment not found with ID: {}", appointmentID);
                    return new ResourceNotFoundException("Appointment", "appointmentID", appointmentID);
                });

        appointment.setPatientName(appointmentDTO.getPatientName());
        appointment.setAge(appointmentDTO.getAge());
        appointment.setGender(appointmentDTO.getGender());
        appointment.setShift(appointmentDTO.getShift());
        appointment.setDate(appointmentDTO.getDate());
        appointment.setMessage(appointmentDTO.getMessage());

        if (appointmentDTO.getDoctorName() != null && !appointmentDTO.getDoctorName().isEmpty()) {
            Doctor doctor = doctorRepository.findByName(appointmentDTO.getDoctorName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Doctor", "name", appointmentDTO.getDoctorName()));
            appointment.setDoctor(doctor);
        }

        if (appointmentDTO.getSlotId() != null) {
            Slot newSlot = slotRepository.findById(appointmentDTO.getSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", appointmentDTO.getSlotId()));
            Slot oldSlot = appointment.getSlot();

            if (oldSlot != null && !oldSlot.getId().equals(newSlot.getId())) {
                slotService.releaseSlot(oldSlot.getId());
            }

            if (!newSlot.isAvailable()) {
                throw new RuntimeException("Selected slot is already booked!");
            }

            slotService.bookSlot(newSlot.getId());
            appointment.setSlot(newSlot);
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        AppointmentDTO updatedDTO = convertToDTO(updatedAppointment);
        logger.info("Appointment updated successfully with ID: {}", appointmentID);
        return updatedDTO;
    }

    public List<AppointmentDTO> getAppointmentsByDrName(String doctorName){
        logger.info("Fetching appointments for doctor: {}", doctorName);
        List<Appointment> appointments = this.appointmentRepository.findAppointmentsByDoctor(doctorName);
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("Total appointments fetched for doctor {}: {}", doctorName, appointmentDTOList.size());
        return appointmentDTOList;
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentDTO.class);
    }

    private Appointment convertToEntities(AppointmentDTO appointmentDTO){
        return modelMapper.map(appointmentDTO, Appointment.class);
    }
}
