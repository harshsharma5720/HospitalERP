package com.itmonteur.hospitalerp.administration.internal;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final AuthService authService;
    private final PtInfoService ptInfoService;
    private final DoctorService doctorService;
    private final ReceptionistService receptionistService;
    private final ModelMapper modelMapper;
    private final LeaveRequestService leaveRequestService;
    private final UserService userService;
    private final UserAccountService userAccountService;

    public AdminService(AuthService authService, PtInfoService ptInfoService, DoctorService doctorService,
                        ReceptionistService receptionistService, ModelMapper modelMapper,
                        LeaveRequestService leaveRequestService, UserService userService,
                        UserAccountService userAccountService) {
        this.authService = authService;
        this.ptInfoService = ptInfoService;
        this.doctorService = doctorService;
        this.receptionistService = receptionistService;
        this.modelMapper = modelMapper;
        this.leaveRequestService = leaveRequestService;
        this.userService = userService;
        this.userAccountService = userAccountService;
    }

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    // -------------------- Create any user --------------------
    @Transactional
    public UserDTO createUser(RegisterRequestDTO registerRequestDTO) {
        Role role = AuthService.parseRole(registerRequestDTO.getRole());
        User user = authService.createUser(registerRequestDTO, role);
        userAccountService.recordCreated(user);
        return convertToDto(user);
    }

    // -------------------- Create Patient --------------------
    @Transactional
    public PtInfoDTO createPatient(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.PATIENT);
        userAccountService.recordCreated(user);
        PtInfo patient = ptInfoService.findPatientEntityByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", user.getId()));
        return PatientMapper.toDTO(patient);
    }

    // -------------------- Create Doctor --------------------
    @Transactional
    public DoctorDTO createDoctor(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.DOCTOR);
        userAccountService.recordCreated(user);
        Doctor doctor = doctorService.findDoctorEntityByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", user.getId()));
        return DoctorMapper.toDTO(doctor);
    }

    // -------------------- Create Receptionist --------------------
    @Transactional
    public ReceptionistDTO createReceptionist(RegisterRequestDTO registerRequestDTO) {
        User user = authService.createUser(registerRequestDTO, Role.RECEPTIONIST);
        userAccountService.recordCreated(user);
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

    // ---------- account status (docs/ACCOUNT_DEACTIVATION_PLAN.md) ----------

    public UserDTO deactivateUser(Long userId) {
        return convertToDto(userAccountService.deactivate(userId));
    }

    public UserDTO reactivateUser(Long userId) {
        return convertToDto(userAccountService.reactivate(userId));
    }

    /** Only for accounts without appointments; otherwise 409 "deactivate instead". */
    public void deleteUserPermanently(Long userId) {
        userAccountService.deletePermanently(userId);
    }

    /** The old DELETE /api/admin/{id}: deactivates now (history is kept). */
    public boolean deleteUserById(Long userId) {
        userAccountService.deactivate(userId);
        logger.info("User deleted by admin: id={}", userId);
        return true;
    }

    private UserDTO convertToDto(User user) {
        return modelMapper.map(user, UserDTO.class);
    }
}
