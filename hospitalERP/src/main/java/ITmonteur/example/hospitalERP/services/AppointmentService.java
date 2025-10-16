package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.PtInfo;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    public List<AppointmentDTO> getAllAppointments(){
        logger.info("Fetching all appointments");
        List<Appointment> appointments = this.appointmentRepository.findAll();
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("Total appointments fetched: {}", appointmentDTOList.size());
        return appointmentDTOList;
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
        logger.info("Creating new appointment for patient: {}", appointmentDTO.getPatientName());
        Appointment appointment = convertToEntities(appointmentDTO);
        PtInfo ptInfo = ptInfoRepository.findById(appointmentDTO.getPtInfoId())
                .orElseThrow(() -> {
                    logger.warn("Patient not found with ID: {}", appointmentDTO.getPtInfoId());
                    return new ResourceNotFoundException("Patient", "id", appointmentDTO.getPtInfoId());
                });
        appointment.setPtInfo(ptInfo);
        Doctor doctor = doctorRepository.findByName(appointmentDTO.getDoctorName())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "name", appointmentDTO.getDoctorName()));
        appointment.setDoctor(doctor);
        appointmentRepository.save(appointment);
        appointmentRepository.save(appointment);
        logger.info("Appointment created successfully for patient: {}", appointmentDTO.getPatientName());
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
                });

        // Update basic fields
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
