package ITmonteur.example.hospitalERP.controller;

import ITmonteur.example.hospitalERP.dto.AppointmentDTO;
import ITmonteur.example.hospitalERP.services.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/getAll")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments(){
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAllAppointments();
            return ResponseEntity.ok(appointmentDTOList);
    }

    @GetMapping("appointmentId/{myId}")
    public ResponseEntity<AppointmentDTO> getAppointmentByAppointmentID(@PathVariable long myId){
        AppointmentDTO appointmentDTO = this.appointmentService.getAppointmentByID(myId);
        return  ResponseEntity.ok(appointmentDTO);

    }

    @GetMapping("appointmentsByDoctor/{doctorName}")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointmentsByDoctor(@PathVariable String doctorName){
        List<AppointmentDTO> appointmentDTOList = this.appointmentService.getAppointmentsByDrName(doctorName);
        return ResponseEntity.ok(appointmentDTOList);

    }

    @PostMapping("/NewAppointment")
    public ResponseEntity<String> createNewAppointment(@RequestBody AppointmentDTO appointmentDTO){
        if(this.appointmentService.createAppointment(appointmentDTO)){
            return ResponseEntity.ok("Your slot has been booked");
        }
        else {
            return ResponseEntity.ok("Error occured");
        }
    }

    @DeleteMapping("/CancelAppointment/{appointmentId}")
    public ResponseEntity<String> deleteAppointmentByAppointmentId(@PathVariable long appointmentId){
        if (this.appointmentService.deleteAppointmentByID(appointmentId)){
            return ResponseEntity.ok("Your appointment slot has been deleted successfully!!");
        }
        else{
            return ResponseEntity.ok("you entered the wrong credentials..");
        }
    }

    @DeleteMapping("/cancelALlAppointments")
    public ResponseEntity<String> deleteAllAppointment(){
        if(this.appointmentService.deleteAllAppointments()){
            return ResponseEntity.ok("Deleted all appointments successfully!!");
        }
        else{
            return ResponseEntity.ok("Error occured..");
        }
    }

    @PutMapping("/update/{appointmentId}")
    public ResponseEntity<AppointmentDTO> updateAppointment(@PathVariable long appointmentId,
                                                            @RequestBody AppointmentDTO appointmentDTO){
        AppointmentDTO appointmentDTO1 = this.appointmentService.updateAppointmentById(appointmentId,appointmentDTO);
        return ResponseEntity.ok(appointmentDTO1);
    }
}