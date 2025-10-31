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

//    public List<AppointmentDTO> getAppointmentsByPatientId(Long patientId) {
//        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
//        return appointments.stream()
//                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
//                .collect(Collectors.toList());
//    }

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

//    public boolean createAppointment(AppointmentDTO appointmentDTO) {
//        logger.info("Creating new appointment for patient: {}", appointmentDTO.getPatientName());
//        Appointment appointment = convertToEntities(appointmentDTO);
//        PtInfo ptInfo = ptInfoRepository.findById(appointmentDTO.getPtInfoId())
//                .orElseThrow(() -> {
//                    logger.warn("Patient not found with ID: {}", appointmentDTO.getPtInfoId());
//                    return new ResourceNotFoundException("Patient", "id", appointmentDTO.getPtInfoId());
//                });
//        appointment.setPtInfo(ptInfo);
//        Doctor doctor = doctorRepository.findByName(appointmentDTO.getDoctorName())
//                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "name", appointmentDTO.getDoctorName()));
//        appointment.setDoctor(doctor);
//        appointmentRepository.save(appointment);
//        logger.info("Appointment created successfully for patient: {}", appointmentDTO.getPatientName());
//        return true;
//    }

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
        return true;
    }


    public boolean deleteAppointmentByID(long appointmentID){
        logger.info("Deleting appointment with ID: {}", appointmentID);
        Appointment appointment = this.appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> {
                    logger.warn("Appointment not found with ID: {}", appointmentID);
                    return new ResourceNotFoundException("Appointment", "id", appointmentID);
                });
        this.appointmentRepository.deleteById(appointmentID);
        logger.info("Appointment deleted successfully with ID: {}", appointmentID);
        return true;
    }

    public boolean deleteAllAppointments(){
        long count = appointmentRepository.count();  // check how many appointments exist
        if (count == 0) {
            logger.warn("No appointments to delete");
            return false;  // no appointments to delete
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
                });  // Fetch existing appointment
        appointment.setPatientName(appointmentDTO.getPatientName());  // Update patient details
        appointment.setAge(appointmentDTO.getAge());
        appointment.setGender(appointmentDTO.getGender());
        appointment.setShift(appointmentDTO.getShift());
        appointment.setDate(appointmentDTO.getDate());
        appointment.setMessage(appointmentDTO.getMessage());
        if (appointmentDTO.getDoctorName() != null && !appointmentDTO.getDoctorName().isEmpty()) {// Update doctor (if provided)
            Doctor doctor = doctorRepository.findByName(appointmentDTO.getDoctorName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Doctor", "name", appointmentDTO.getDoctorName()));
            appointment.setDoctor(doctor);
        }
        if (appointmentDTO.getSlotId() != null) {  // Update slot (if provided)
            Slot newSlot = slotRepository.findById(appointmentDTO.getSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", appointmentDTO.getSlotId()));
            Slot oldSlot = appointment.getSlot();// Release previous slot if changed
            if (oldSlot != null && !oldSlot.getId().equals(newSlot.getId())) {
                slotService.releaseSlot(oldSlot.getId());
            }
            if (!newSlot.isAvailable()) {  // Check slot availability
                throw new RuntimeException("Selected slot is already booked!");
            }
            slotService.bookSlot(newSlot.getId());  // Book the new slot
            appointment.setSlot(newSlot);
        }
        Appointment updatedAppointment = appointmentRepository.save(appointment);  // Save and map updated appointment
        AppointmentDTO updatedDTO = convertToDTO(updatedAppointment);
        logger.info("Appointment updated successfully with ID: {}", appointmentID);
        return updatedDTO;
    }

//    public AppointmentDTO updateAppointmentById(long appointmentID, AppointmentDTO appointmentDTO) {
//        logger.info("Updating appointment with ID: {}", appointmentID);
//
//        Appointment appointment = this.appointmentRepository.findById(appointmentID)
//                .orElseThrow(() -> {
//                    logger.warn("Appointment not found with ID: {}", appointmentID);
//                    return new ResourceNotFoundException("Appointment", "appointmentID", appointmentID);
//                });
//
//        // Update basic fields
//        appointment.setPatientName(appointmentDTO.getPatientName());
//        appointment.setAge(appointmentDTO.getAge());
//        appointment.setGender(appointmentDTO.getGender());
//        appointment.setShift(appointmentDTO.getShift());
//        appointment.setDate(appointmentDTO.getDate());
//        appointment.setMessage(appointmentDTO.getMessage());
//        if (appointmentDTO.getDoctorName() != null && !appointmentDTO.getDoctorName().isEmpty()) {
//            Doctor doctor = doctorRepository.findByName(appointmentDTO.getDoctorName())
//                    .orElseThrow(() -> new ResourceNotFoundException(
//                            "Doctor", "name", appointmentDTO.getDoctorName()));
//            appointment.setDoctor(doctor);
//        }
//        Appointment updatedAppointment = appointmentRepository.save(appointment);
//        AppointmentDTO updatedDTO = convertToDTO(updatedAppointment);
//
//        logger.info("Appointment updated successfully with ID: {}", appointmentID);
//        return updatedDTO;
//    }

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
