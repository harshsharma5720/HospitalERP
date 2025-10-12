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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceptionistService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private ReceptionistRepository receptionistRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ModelMapper modelMapper;

    private ReceptionistDTO registerReceptionist(ReceptionistDTO receptionistDTO){
        Receptionist receptionist = this.convertToEntity(receptionistDTO);
        this.receptionistRepository.save(receptionist);
        return receptionistDTO;
    }

    private ReceptionistDTO getReceptionistByID(Long receptionistId){
        Receptionist receptionist= this.receptionistRepository.findById(receptionistId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", receptionistId));
        ReceptionistDTO receptionistDTO = this.convertToDTO(receptionist);
        return receptionistDTO;
    }

    private List<ReceptionistDTO> getAllReceptionist(){
        List<Receptionist> receptionists = this.receptionistRepository.findAll();
        List<ReceptionistDTO> receptionistDTOList = receptionists.stream()
                .map(receptionist -> this.modelMapper.map(receptionist , ReceptionistDTO.class))
                .collect(Collectors.toList());
        return receptionistDTOList;
    }

    public List<AppointmentDTO> getAllAppointments(){
        List<Appointment> appointments = this.appointmentRepository.findAll();
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return appointmentDTOList;
    }

    public List<AppointmentDTO> getAllAppointmentsByDoctorName(String name){
        Doctor doctor = this.doctorRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Doctor not found with name: " + name));
        List<Appointment> appointments = this.appointmentRepository.findAppointmentsByDoctor(name);
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return appointmentDTOList;
    }

    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO){
        Appointment appointment = this.convertToEntities(appointmentDTO);
        this.appointmentRepository.save(appointment);
        return appointmentDTO;
    }

    public boolean deleteReceptionist(Long receptionistID){
        try {
            Receptionist receptionist = receptionistRepository.findById(receptionistID)
                    .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistID));
            receptionistRepository.delete(receptionist);
            return true;
        } catch (ResourceNotFoundException e) {
            System.err.println("Error: Receptionist not found with ID " + receptionistID);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAppointment(Long appointmentID){
        try {
            Appointment appointment = appointmentRepository.findById(appointmentID)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentID));
            appointmentRepository.delete(appointment);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAllReceptionist(){
        long count = receptionistRepository.count();
        if (count == 0) {return false;  // no doctors to delete
        }
        this.receptionistRepository.deleteAll();
        return true;
    }

    public ReceptionistDTO updateReceptionist(Long receptionistID , ReceptionistDTO receptionistDTO){
        Receptionist receptionist = this.receptionistRepository.findById(receptionistID)
                .orElseThrow(() -> new ResourceNotFoundException("Receptionist", "id", receptionistID));
        receptionist.setName(receptionistDTO.getName());
        receptionist.setEmail(receptionistDTO.getEmail());
        receptionist.setPhone(receptionistDTO.getPhone());
        receptionist.setGender(receptionistDTO.getGender());
        receptionist.setAge(receptionistDTO.getAge());
        Receptionist updatedReceptionist = this.receptionistRepository.save(receptionist);
        ReceptionistDTO receptionistDTO1 = this.convertToDTO(updatedReceptionist);
        return receptionistDTO1;
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
    private Appointment convertToEntities(AppointmentDTO appointmentDTO){
        return modelMapper.map(appointmentDTO,Appointment.class);
    }


}
