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
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PtInfoRepository ptInfoRepository;
    @Autowired
    private ReceptionistRepository receptionistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    // -------------------- Patient --------------------
    public PtInfoDTO createPtInfo(PtInfoDTO ptInfoDTO) {
        logger.info("Creating new patient record for: {}", ptInfoDTO.getPatientName());

        try {
            PtInfo ptInfo = convertToEntity(ptInfoDTO);

            // Initialize appointments list
            if (ptInfo.getAppointments() == null) {
                ptInfo.setAppointments(new ArrayList<>());
                logger.debug("No appointments provided. Initialized empty list for patient: {}", ptInfo.getPatientName());
            }

            // Create default user credentials
            String defaultUsername = "pt_" + ptInfo.getPatientName().toLowerCase().replace(" ", "_") + "_" + UUID.randomUUID().toString().substring(0, 4);
            String defaultPassword = passwordEncoder.encode("Patient@123");

            User user = new User();
            user.setUsername(defaultUsername);
            user.setPassword(defaultPassword);
            user.setEmail(ptInfo.getEmail());
            user.setRole(Role.PATIENT);
            userRepository.save(user);

            ptInfo.setUser(user);

            PtInfo savedPatient = ptInfoRepository.save(ptInfo);
            PtInfoDTO savedDTO = convertToDto(savedPatient);

            logger.info("Patient record created successfully with username: {}", user.getUsername());
            return savedDTO;

        } catch (Exception e) {
            logger.error("Error creating patient record for {}: {}", ptInfoDTO.getPatientName(), e.getMessage(), e);
            throw e;
        }
    }

    // -------------------- Doctor --------------------
    public DoctorDTO createDoctor(DoctorDTO doctorDTO) {
        logger.info("Creating new doctor profile for: {}", doctorDTO.getName());

        try {
            Doctor doctor = convertToEntity(doctorDTO);

            // Default credentials
            String defaultUsername = "dr_" + doctor.getName().toLowerCase().replace(" ", "_") + "_" + UUID.randomUUID().toString().substring(0, 4);
            String defaultPassword = passwordEncoder.encode("Doctor@123");

            User user = new User();
            user.setUsername(defaultUsername);
            user.setPassword(defaultPassword);
            user.setEmail(doctor.getEmail());
            user.setRole(Role.DOCTOR);
            userRepository.save(user);

            doctor.setUser(user);
            doctor.setRole(Role.DOCTOR);

            Doctor savedDoctor = doctorRepository.save(doctor);
            DoctorDTO savedDTO = convertToDto(savedDoctor);

            logger.info("Doctor created successfully with username: {}", user.getUsername());
            return savedDTO;

        } catch (Exception e) {
            logger.error("Error creating doctor profile for {}: {}", doctorDTO.getName(), e.getMessage(), e);
            throw e;
        }
    }

    // -------------------- Receptionist --------------------
    public ReceptionistDTO createReceptionist(ReceptionistDTO receptionistDTO) {
        logger.info("Creating new receptionist profile for: {}", receptionistDTO.getName());

        try {
            Receptionist receptionist = convertToEntity(receptionistDTO);
            // Default credentials
            String defaultUsername = "rcp_" + receptionist.getName().toLowerCase().replace(" ", "_") + "_" + UUID.randomUUID().toString().substring(0, 4);
            String defaultPassword = passwordEncoder.encode("Reception@123");

            User user = new User();
            user.setUsername(defaultUsername);
            user.setPassword(defaultPassword);
            user.setEmail(receptionist.getEmail());
            user.setRole(Role.RECEPTIONIST);
            userRepository.save(user);

            receptionist.setUser(user);
            receptionist.setRole(Role.RECEPTIONIST);

            Receptionist savedReceptionist = receptionistRepository.save(receptionist);
            ReceptionistDTO savedDTO = convertToDTO(savedReceptionist);

            logger.info("Receptionist created successfully with username: {}", user.getUsername());
            return savedDTO;

        } catch (Exception e) {
            logger.error("Error creating receptionist profile for {}: {}", receptionistDTO.getName(), e.getMessage(), e);
            throw e;
        }
    }

    // -------------------- Appointment --------------------
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        logger.info("Creating new appointment for patient ID: {}", appointmentDTO.getPtInfoId());

        try {
            Appointment appointment = convertToEntities(appointmentDTO);

            Appointment savedAppointment = appointmentRepository.save(appointment);
            AppointmentDTO savedDTO = convertToDTO(savedAppointment);

            logger.info("Appointment created successfully with ID: {}", savedAppointment.getAppointmentID());
            return savedDTO;

        } catch (Exception e) {
            logger.error("Error creating appointment: {}", e.getMessage(), e);
            throw e;
        }
    }

    // -------------------- Conversion Methods --------------------
    private PtInfoDTO convertToDto(PtInfo ptInfo) {
        return modelMapper.map(ptInfo, PtInfoDTO.class);
    }

    private PtInfo convertToEntity(PtInfoDTO ptInfoDTO) {
        return modelMapper.map(ptInfoDTO, PtInfo.class);
    }

    private DoctorDTO convertToDto(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    private Doctor convertToEntity(DoctorDTO doctorDTO) {
        return modelMapper.map(doctorDTO, Doctor.class);
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
