package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Doctor;
import com.itmonteur.hospitalerp.common.Gender;
import ITmonteur.example.hospitalERP.entities.Receptionist;
import ITmonteur.example.hospitalERP.entities.Specialist;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.identity.UserRegisteredEvent;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.ReceptionistRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the doctor / receptionist profile when such an account is created.
 * Synchronous listener: runs in the registration transaction. Belongs to the staff module.
 */
@Component
public class StaffProfileCreator {

    private final DoctorRepository doctorRepository;
    private final ReceptionistRepository receptionistRepository;

    public StaffProfileCreator(DoctorRepository doctorRepository, ReceptionistRepository receptionistRepository) {
        this.doctorRepository = doctorRepository;
        this.receptionistRepository = receptionistRepository;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.user();
        switch (user.getRole()) {
            case DOCTOR -> {
                Doctor doctor = new Doctor();
                doctor.setEmail(user.getEmail());
                doctor.setUserName(user.getUsername()); // foreign key (username)
                doctor.setName(user.getUsername());
                doctor.setSpecialist(Specialist.NOT_ASSIGNED);
                doctor.setPhoneNumber(user.getPhoneNumber());
                doctor.setUser(user);
                doctorRepository.save(doctor);
            }
            case RECEPTIONIST -> {
                Receptionist receptionist = new Receptionist();
                receptionist.setEmail(user.getEmail());
                receptionist.setUserName(user.getUsername());
                receptionist.setName(user.getUsername());
                receptionist.setPhone(user.getPhoneNumber());
                receptionist.setUser(user);
                receptionist.setGender(Gender.OTHER);
                receptionistRepository.save(receptionist);
            }
            default -> {
                // PATIENT is handled by the patients module, ADMIN has no profile
            }
        }
    }
}
