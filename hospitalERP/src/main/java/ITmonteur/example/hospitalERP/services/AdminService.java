package ITmonteur.example.hospitalERP.services;

import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.AuthService;
import com.itmonteur.hospitalerp.identity.RegisterRequestDTO;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.identity.UserDTO;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.patients.PatientMapper;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.patients.PtInfoDTO;
import com.itmonteur.hospitalerp.patients.PtInfoService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorDTO;
import com.itmonteur.hospitalerp.staff.DoctorMapper;
import com.itmonteur.hospitalerp.staff.DoctorService;
import com.itmonteur.hospitalerp.staff.LeaveRequestDTO;
import com.itmonteur.hospitalerp.staff.LeaveRequestService;
import com.itmonteur.hospitalerp.staff.LeaveStatus;
import com.itmonteur.hospitalerp.staff.Receptionist;
import com.itmonteur.hospitalerp.staff.ReceptionistDTO;
import com.itmonteur.hospitalerp.staff.ReceptionistService;
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
    private PtInfoService ptInfoService;
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private ReceptionistService receptionistService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private LeaveRequestService leaveRequestService;
    @Autowired
    private UserService userService;
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
        PtInfo patient = ptInfoService.findPatientEntityByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", user.getId()));
        return PatientMapper.toDTO(patient);
    }

    // -------------------- Create Doctor --------------------
    @Transactional
    public DoctorDTO createDoctor(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.DOCTOR);
        Doctor doctor = doctorService.findDoctorEntityByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", user.getId()));
        return DoctorMapper.toDTO(doctor);
    }

    // -------------------- Create Receptionist --------------------
    @Transactional
    public ReceptionistDTO createReceptionist(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.RECEPTIONIST);
        Receptionist receptionist = receptionistService.findReceptionistEntityByUserId(user.getId())
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
        return userService.getAllUsers()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    public UserDTO getUserById(Long userId) {
        return userService.findUser(userId)
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
