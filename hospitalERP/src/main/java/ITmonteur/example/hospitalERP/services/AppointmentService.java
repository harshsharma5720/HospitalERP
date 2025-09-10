package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.repositories.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private ModelMapper modelMapper;

    public List<AppointmentDTO> getAllAppointments(){
        List<Appointment> appointments = this.appointmentRepository.findAll();
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return appointmentDTOList;

    }
    public AppointmentDTO getAppointmentByID(long appointmentID ){
        Appointment appointment = this.appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentID));
        AppointmentDTO appointmentDTO = this.convertToDTO(appointment);
        return appointmentDTO;
    }
    public boolean createAppointment(AppointmentDTO appointmentDTO){
        Appointment appointment = this.convertToEntities(appointmentDTO);
        this.appointmentRepository.save(appointment);
        return true;
    }
    public boolean deleteAppointmentByID(long appointmentID){
        this.appointmentRepository.deleteById(appointmentID);
        return true;
    }
    public boolean deleteAllAppointments(){
        this.appointmentRepository.deleteAll();
        return true;
    }
    public AppointmentDTO updateAppointmentById(long appointmentID,AppointmentDTO appointmentDTO){
        Appointment appointment = this.appointmentRepository.findById(appointmentID)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentID));
        appointment.setAge(appointmentDTO.getAge());
        appointment.setGender(appointmentDTO.getGender());
        appointment.setPatientName(appointmentDTO.getPatientName());
        appointment.setEmail(appointmentDTO.getEmail());
        appointment.setDoctor(appointmentDTO.getDoctor());
        appointment.setPhoneNo(appointmentDTO.getPhoneNo());
        return convertToDTO(appointmentRepository.save(appointment));

    }
//    public AppointmentDTO getByDrName(String drName){
//        AppointmentDTO appointmentDTO = this.appointmentRepository.findByDrName()
//    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentDTO.class);
    }
    private Appointment convertToEntities(AppointmentDTO appointmentDTO){
        return modelMapper.map(appointmentDTO,Appointment.class);
    }
}
