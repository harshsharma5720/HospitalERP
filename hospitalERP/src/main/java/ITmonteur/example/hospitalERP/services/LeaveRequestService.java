package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.LeaveRequestDTO;
import ITmonteur.example.hospitalERP.entities.LeaveRequest;
import ITmonteur.example.hospitalERP.entities.LeaveStatus;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.LeaveRequestRepository;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestService.class);

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Convert DTO → Entity
    private LeaveRequest convertToEntity(LeaveRequestDTO dto) {
        logger.info("Converting LeaveRequestDTO to Entity for user {}", dto.getUserId());
        return modelMapper.map(dto, LeaveRequest.class);
    }

    // Convert Entity → DTO
    private LeaveRequestDTO convertToDto(LeaveRequest leaveRequest) {
        logger.info("Converting LeaveRequest Entity to DTO for ID {}", leaveRequest.getId());
        return modelMapper.map(leaveRequest, LeaveRequestDTO.class);
    }

    public LeaveRequestDTO createLeaveRequest(LeaveRequestDTO dto) {
        logger.info("Creating leave request for user {}", dto.getUserId());
        LeaveRequest leaveRequest = new LeaveRequest();
        boolean exists = leaveRequestRepository
                .existsByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        dto.getUserId(), dto.getEndDate(), dto.getStartDate());
        if (exists) {
            throw new RuntimeException("Leave Request already exists for these dates! Choose different dates.");
        }
        User user = this.userRepository.findById(dto.getUserId()).orElseThrow(() ->
                new ResourceNotFoundException("User", "id", dto.getUserId()));
        logger.info("User found: {}", user.getUsername());
        leaveRequest.setUser(user);
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setRole(dto.getRole());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        logger.info("Leave request successfully created with ID {}", savedRequest.getId());
        return convertToDto(savedRequest);
    }

    public List<LeaveRequestDTO> getAllLeaveRequests() {
        logger.info("Fetching all leave requests");
        return leaveRequestRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public LeaveRequestDTO getLeaveRequestById(Long id) {
        logger.info("Fetching leave request by ID {}", id);
        return leaveRequestRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
    }

    public List<LeaveRequestDTO> getLeavesByUser(Long userId) {
        logger.info("Fetching leave requests for user {}", userId);
        return leaveRequestRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    public List<LeaveRequestDTO> getLeavesByUserAndStatus(Long userId, LeaveStatus status) {
        logger.info("Fetching leave requests for user {}", userId);
        List<LeaveRequest> leaves = leaveRequestRepository.findByUserIdAndStatus(userId, status);
        return leaves.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    public List<LeaveRequestDTO> getAllLeavesByStatus(LeaveStatus status) {
        logger.info("Fetching all leave requests with status {}", status);
        return this.leaveRequestRepository.findAllByStatus(status)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public LeaveRequestDTO updateLeaveRequest(Long id, LeaveRequestDTO dto) {
        logger.info("Updating leave request ID {}", id);

        LeaveRequest existing = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setReason(dto.getReason());
        existing.setRole(dto.getRole());

        LeaveRequest saved = leaveRequestRepository.save(existing);

        logger.info("Leave request updated successfully ID {}", id);
        return convertToDto(saved);
    }

    public LeaveRequestDTO updateLeaveStatus(Long id, LeaveStatus status) {
        logger.info("Updating status for Leave Request ID {}", id);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        leaveRequest.setStatus(status);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        logger.info("Leave status updated to {} for Leave Request ID {}", status, id);
        return convertToDto(saved);
    }

    public boolean deleteLeaveRequest(Long id) {
        logger.info("Deleting leave request ID {}", id);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        leaveRequestRepository.delete(leaveRequest);
        logger.info("Leave request deleted successfully ID {}", id);
        return true;
    }
}
