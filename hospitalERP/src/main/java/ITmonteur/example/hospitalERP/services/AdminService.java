package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.*;
import ITmonteur.example.hospitalERP.entities.*;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private LeaveRequestService leaveRequestService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAccountService userAccountService;

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    // -------------------- Create any user --------------------
    @Transactional
    public UserDTO createUser(RegisterRequestDTO registerRequestDTO) {
        Role role = AuthService.parseRole(registerRequestDTO.getRole());
        User user = authService.createUser(registerRequestDTO, role);
        return convertToDto(user);
    }

    // -------------------- Create Patient --------------------
    @Transactional
    public PtInfoDTO createPatient(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.PATIENT);
        PtInfo patient = ptInfoRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", user.getId()));
        return EntityMapper.toPtInfoDTO(patient);
    }

    // -------------------- Create Doctor --------------------
    @Transactional
    public DoctorDTO createDoctor(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.DOCTOR);
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", user.getId()));
        return EntityMapper.toDoctorDTO(doctor);
    }

    // -------------------- Create Receptionist --------------------
    @Transactional
    public ReceptionistDTO createReceptionist(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.RECEPTIONIST);
        Receptionist receptionist = receptionistRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "userId", user.getId()));
        return modelMapper.map(receptionist, ReceptionistDTO.class);
    }

    // -------------------- Leave decisions --------------------
    public LeaveRequestDTO approveLeave(Long leaveId) {
        return leaveRequestService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED);
    }

    public LeaveRequestDTO rejectLeave(Long leaveId) {
        return leaveRequestService.updateLeaveStatus(leaveId, LeaveStatus.REJECTED);
    }

    // -------------------- Users --------------------
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    public UserDTO getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    public boolean deleteUserById(Long userId) {
        userAccountService.deleteUser(userId);
        logger.info("User deleted by admin: id={}", userId);
        return true;
    }

    private UserDTO convertToDto(User user) {
        return modelMapper.map(user, UserDTO.class);
    }
}
