package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.dto.DoctorDTO;
import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Doctor;
import ITmonteur.example.hospitalERP.exception.ResourceNotFoundException;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ModelMapper modelMapper;

    public DoctorDTO registerDoctor(DoctorDTO request) {
//        Doctor doctor = new Doctor();
//        doctor.setName(request.getName());
//        doctor.setSpecialization(request.getSpecialization());
//        doctor.setEmail(request.getEmail());
//        doctor.setPassword(request.getPassword()); // TODO: encode later with BCrypt
//        doctor.setPhoneNumber(request.getPhoneNumber());
        Doctor doctor = this.convertToEntity(request);
        this.doctorRepository.save(doctor);
        DoctorDTO doctorDTO = this.convertToDTO(doctor);
        return doctorDTO;
    }
    public List<DoctorDTO> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return doctors.stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .collect(Collectors.toList());
    }

    public DoctorDTO getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        DoctorDTO doctorDTO = this.convertToDTO(doctor);
        return doctorDTO;
    }
    public DoctorDTO updateDoctor(Long doctorId, DoctorDTO doctorDTO) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        doctor.setName(doctorDTO.getName());
        doctor.setEmail(doctorDTO.getEmail());
        doctor.setSpecialization(doctorDTO.getSpecialization());

        Doctor updatedDoctor = doctorRepository.save(doctor);
        DoctorDTO doctorDTO1 = this.convertToDTO(updatedDoctor);
        return doctorDTO1;
    }
    public boolean deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        this.doctorRepository.delete(doctor);
        return true;
    }
    public boolean deleteAllDoctors(){
        long count = doctorRepository.count();  // check how many doctors exist
        if (count == 0) {return false;  // no doctors to delete
        }
        this.doctorRepository.deleteAll();
        return true;
    }

    private DoctorDTO convertToDTO(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDTO.class);
    }
    private Doctor convertToEntity(DoctorDTO doctorDTO) {
        return modelMapper.map(doctorDTO, Doctor.class);
    }
}
