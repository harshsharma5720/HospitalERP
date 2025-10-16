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
    private AuthService authService;
    @Autowired
    private PtInfoRepository ptInfoRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ReceptionistRepository receptionistRepository;
    @Autowired
    private ModelMapper modelMapper;

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
}
