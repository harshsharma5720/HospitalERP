package com.itmonteur.hospitalerp;

import ITmonteur.example.hospitalERP.entities.*;
import com.itmonteur.hospitalerp.identity.UserRegisteredEvent;
import ITmonteur.example.hospitalERP.repositories.DoctorRepository;
import ITmonteur.example.hospitalERP.repositories.PtInfoRepository;
import ITmonteur.example.hospitalERP.repositories.ReceptionistRepository;
import ITmonteur.example.hospitalERP.services.PatientProfileCreator;
import ITmonteur.example.hospitalERP.services.StaffProfileCreator;
import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The listeners that create role profiles when a user is registered (step 1.2). */
class ProfileCreatorsTest {

    private final PtInfoRepository ptInfoRepository = mock(PtInfoRepository.class);
    private final DoctorRepository doctorRepository = mock(DoctorRepository.class);
    private final ReceptionistRepository receptionistRepository = mock(ReceptionistRepository.class);
    private final PatientProfileCreator patientCreator = new PatientProfileCreator(ptInfoRepository);
    private final StaffProfileCreator staffCreator = new StaffProfileCreator(doctorRepository, receptionistRepository);

    private static UserRegisteredEvent registered(Role role) {
        User user = new User();
        user.setId(7L);
        user.setUsername("asha");
        user.setEmail("asha@example.com");
        user.setPhoneNumber("+919999999999");
        user.setRole(role);
        return new UserRegisteredEvent(user);
    }

    private void publish(UserRegisteredEvent event) {
        patientCreator.onUserRegistered(event);
        staffCreator.onUserRegistered(event);
    }

    @Test
    void patientGetsOnlyAPatientProfileWithDefaults() {
        UserRegisteredEvent event = registered(Role.PATIENT);
        publish(event);

        ArgumentCaptor<PtInfo> saved = ArgumentCaptor.forClass(PtInfo.class);
        verify(ptInfoRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(event.user());
        assertThat(saved.getValue().getPatientName()).isEqualTo("asha");
        assertThat(saved.getValue().getContactNo()).isEqualTo("+919999999999");
        assertThat(saved.getValue().getGender()).isEqualTo(Gender.OTHER);
        assertThat(saved.getValue().getDob()).isNull();
        verifyNoInteractions(doctorRepository, receptionistRepository);
    }

    @Test
    void doctorGetsOnlyADoctorProfile() {
        UserRegisteredEvent event = registered(Role.DOCTOR);
        publish(event);

        ArgumentCaptor<Doctor> saved = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(event.user());
        assertThat(saved.getValue().getSpecialist()).isEqualTo(Specialist.NOT_ASSIGNED);
        assertThat(saved.getValue().getPhoneNumber()).isEqualTo("+919999999999");
        verifyNoInteractions(ptInfoRepository, receptionistRepository);
    }

    @Test
    void receptionistGetsOnlyAReceptionistProfile() {
        publish(registered(Role.RECEPTIONIST));

        verify(receptionistRepository).save(any(Receptionist.class));
        verifyNoInteractions(ptInfoRepository, doctorRepository);
    }

    @Test
    void adminGetsNoProfile() {
        publish(registered(Role.ADMIN));

        verifyNoInteractions(ptInfoRepository, doctorRepository, receptionistRepository);
    }
}
