package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.dto.ReceptionistDTO;
import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Receptionist;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.ReceptionistRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceptionistService {

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ReceptionistRepository receptionistRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ModelMapper modelMapper;

    public ReceptionistDTO registerReceptionist(ReceptionistDTO receptionistDTO) {
        logger.info("Attempting to register a new receptionist: {}", receptionistDTO.getName());
        Receptionist receptionist = this.convertToEntity(receptionistDTO);
        receptionistRepository.save(receptionist);
        logger.info("Receptionist registered successfully with ID: {}", receptionist.getId());
        return receptionistDTO;
    }

    public ReceptionistDTO getReceptionistByID(Long receptionistId) {
        logger.info("Fetching receptionist by ID: {}", receptionistId);
        Receptionist receptionist = receptionistRepository.findById(receptionistId)
                .orElseThrow(() -> {
                    logger.warn("Receptionist not found with ID: {}", receptionistId);
                    return new ResourceNotFoundException("Receptionist", "id", receptionistId);
                });

        logger.info("Receptionist found: {}", receptionist.getName());
        return this.convertToDTO(receptionist);
    }

    public List<ReceptionistDTO> getAllReceptionist() {
        logger.info("Fetching all receptionists from database");
        List<Receptionist> receptionists = receptionistRepository.findAll();
        logger.info("Total receptionists found: {}", receptionists.size());
        return receptionists.stream()
                .map(receptionist -> modelMapper.map(receptionist, ReceptionistDTO.class))
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getAllAppointments() {
        logger.info("Fetching all appointments");
        List<Appointment> appointments = appointmentRepository.findAll();
        logger.info("Total appointments found: {}", appointments.size());
        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getAllAppointmentsByDoctorName(String name) {
        logger.info("Fetching appointments for doctor: {}", name);
        Doctor doctor = doctorRepository.findByName(name)
                .orElseThrow(() -> {
                    logger.warn("Doctor not found with name: {}", name);
                    return new RuntimeException("Doctor not found with name: " + name);
                });

        List<Appointment> appointments = appointmentRepository.findAppointmentsByDoctor(name);
        logger.info("Appointments found for doctor {}: {}", name, appointments.size());
        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean createAppointment(AppointmentDTO appointmentDTO) {
        logger.info("Creating new appointment for patient: {}", appointmentDTO.getPatientName());
        try {
            Appointment appointment = this.convertToEntities(appointmentDTO);
            appointmentRepository.save(appointment);
            logger.info("Appointment created successfully with ID: {}", appointment.getAppointmentID());
            return true;
        } catch (Exception e) {
            logger.error("Error while creating appointment: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteReceptionist(Long receptionistID) {
        logger.info("Attempting to delete receptionist with ID: {}", receptionistID);
        try {
            Receptionist receptionist = receptionistRepository.findById(receptionistID)
                    .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistID));

            receptionistRepository.delete(receptionist);
            logger.info("Receptionist deleted successfully with ID: {}", receptionistID);
            return true;
        } catch (ResourceNotFoundException e) {
            logger.warn("Receptionist not found with ID: {}", receptionistID);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while deleting receptionist with ID {}: {}", receptionistID, e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteAppointment(Long appointmentID) {
        logger.info("Attempting to delete appointment with ID: {}", appointmentID);
        try {
            Appointment appointment = appointmentRepository.findById(appointmentID)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentID));

            appointmentRepository.delete(appointment);
            logger.info("Appointment deleted successfully with ID: {}", appointmentID);
            return true;
        } catch (ResourceNotFoundException e) {
            logger.warn("Appointment not found with ID: {}", appointmentID);
            return false;
        } catch (Exception e) {
            logger.error("Error deleting appointment ID {}: {}", appointmentID, e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteAllAppointments() {
        long count = appointmentRepository.count();
        if (count == 0) {
            logger.warn("No appointments found to delete");
            return false;
        }
        appointmentRepository.deleteAll();
        logger.info("All appointments deleted successfully ({} records)", count);
        return true;
    }

    public boolean deleteAllReceptionist() {
        try {
            long count = receptionistRepository.count();
            if (count == 0) {
                logger.warn("No receptionists found to delete");
                return false;
            }
            receptionistRepository.deleteAll();
            logger.info("All receptionists deleted successfully ({} records)", count);
            return true;
        } catch (Exception e) {
            logger.error("Error while deleting all receptionists: {}", e.getMessage(), e);
            return false;
        }
    }

    public ReceptionistDTO updateReceptionist(Long receptionistID, ReceptionistDTO receptionistDTO) {
        logger.info("Updating receptionist with ID: {}", receptionistID);
        try {
            Receptionist receptionist = receptionistRepository.findById(receptionistID)
                    .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistID));

            if (receptionistDTO.getName() != null) receptionist.setName(receptionistDTO.getName());
            if (receptionistDTO.getEmail() != null) receptionist.setEmail(receptionistDTO.getEmail());
            if (receptionistDTO.getPhone() != null) receptionist.setPhone(receptionistDTO.getPhone());
            if (receptionistDTO.getGender() != null) receptionist.setGender(receptionistDTO.getGender());
            if (receptionistDTO.getAge() != 0) receptionist.setAge(receptionistDTO.getAge());

            Receptionist updatedReceptionist = receptionistRepository.save(receptionist);
            logger.info("Receptionist updated successfully with ID: {}", receptionistID);

            return this.convertToDTO(updatedReceptionist);

        } catch (ResourceNotFoundException e) {
            logger.warn("Receptionist not found with ID: {}", receptionistID);
            throw e;
        } catch (Exception e) {
            logger.error("Error updating receptionist with ID {}: {}", receptionistID, e.getMessage(), e);
            return null;
        }
    }

    private ReceptionistDTO convertToDTO(Receptionist receptionist) {
        return modelMapper.map(receptionist, ReceptionistDTO.class);
    }

    private Receptionist convertToEntity(ReceptionistDTO receptionistDTO) {
        return modelMapper.map(receptionistDTO, Receptionist.class);
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentDTO.class);
    }

    private Appointment convertToEntities(AppointmentDTO appointmentDTO) {
        return modelMapper.map(appointmentDTO, Appointment.class);
    }
}
