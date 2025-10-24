package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.entities.Specialist;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Register a new doctor
    public DoctorDTO registerDoctor(DoctorDTO request) {
        logger.info("Registering new doctor: {}", request.getName());
        Doctor doctor = this.convertToEntity(request);
        doctorRepository.save(doctor);
        DoctorDTO doctorDTO = this.convertToDTO(doctor);
        logger.info("Doctor registered successfully with ID: {}", doctorDTO.getId());
        return doctorDTO;
    }

    // Get all doctors
    public List<DoctorDTO> getAllDoctors() {
        logger.info("Fetching all doctors");
        List<Doctor> doctors = doctorRepository.findAll();
        List<DoctorDTO> doctorDTOList = doctors.stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .collect(Collectors.toList());
        logger.info("Total doctors retrieved: {}", doctorDTOList.size());
        return doctorDTOList;
    }

    // Get doctor by ID
    public DoctorDTO getDoctorById(Long doctorId) {
        logger.info("Fetching doctor with ID: {}", doctorId);
        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
            DoctorDTO doctorDTO = convertToDTO(doctor);
            logger.info("Doctor retrieved: {}", doctorDTO.getName());
            return doctorDTO;
        } catch (ResourceNotFoundException e) {
            logger.warn("Doctor not found with ID: {}", doctorId);
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching doctor with ID {}: {}", doctorId, e.getMessage(), e);
            throw e;
        }
    }
    public List<DoctorDTO> findDoctorsBySpecialization(Specialist specialization){
        logger.info("Fetching all doctors ");
        List<Doctor> doctors = this.doctorRepository.findBySpecialist(specialization)
                .orElseThrow(()-> new RuntimeException("Doctors not found with specialization :"+specialization));
        List<DoctorDTO> doctorDTOS = doctors.stream()
                .map(doctor ->{
                    DoctorDTO doctorDTO = convertToDTO(doctor);
                    return doctorDTO;
                })
                .collect(Collectors.toList());
        logger.info("Total doctors retrieved: {}", doctorDTOS.size());
        return doctorDTOS;
    }

    // Update doctor details
    public DoctorDTO updateDoctor(Long doctorId, DoctorDTO doctorDTO) {
        logger.info("Updating doctor with ID: {}", doctorId);
        try {
            Doctor doctor = doctorRepository.findByUserId(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
            doctor.setName(doctorDTO.getName());
            doctor.setUserName(doctorDTO.getUserName());
            doctor.setEmail(doctorDTO.getEmail());
            doctor.setPhoneNumber(doctorDTO.getPhoneNumber());
            doctor.setSpecialist(Specialist.valueOf(doctorDTO.getSpecialization().toUpperCase()));

            Doctor updatedDoctor = doctorRepository.save(doctor);
            DoctorDTO updatedDTO = convertToDTO(updatedDoctor);
            logger.info("Doctor updated successfully with ID: {}", doctorId);
            return updatedDTO;
        } catch (ResourceNotFoundException e) {
            logger.warn("Doctor not found for update with ID: {}", doctorId);
            throw e;
        } catch (Exception e) {
            logger.error("Error updating doctor with ID {}: {}", doctorId, e.getMessage(), e);
            throw e;
        }
    }

    // Delete doctor by ID
    public boolean deleteDoctor(Long doctorId) {
        logger.info("Deleting doctor with ID: {}", doctorId);
        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
            doctorRepository.delete(doctor);
            logger.info("Doctor deleted successfully with ID: {}", doctorId);
            return true;
        } catch (ResourceNotFoundException e) {
            logger.warn("Doctor not found for deletion with ID: {}", doctorId);
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting doctor with ID {}: {}", doctorId, e.getMessage(), e);
            return false;
        }
    }

    // Delete all doctors
    public boolean deleteAllDoctors() {
        logger.info("Deleting all doctors");
        long count = doctorRepository.count();
        if (count == 0) {
            logger.warn("No doctors found to delete");
            return false;
        }
        doctorRepository.deleteAll();
        logger.info("All doctors deleted successfully");
        return true;
    }

    // Convert entity to DTO
    private DoctorDTO convertToDTO(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    // Convert DTO to entity
    private Doctor convertToEntity(DoctorDTO doctorDTO) {
        return modelMapper.map(doctorDTO, Doctor.class);
    }
}
