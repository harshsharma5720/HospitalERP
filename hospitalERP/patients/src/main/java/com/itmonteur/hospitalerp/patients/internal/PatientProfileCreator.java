package com.itmonteur.hospitalerp.patients.internal;

import com.itmonteur.hospitalerp.common.Gender;
import com.itmonteur.hospitalerp.patients.PtInfo;
import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import com.itmonteur.hospitalerp.identity.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the patient profile when a PATIENT account is registered.
 * Synchronous listener: runs in the registration transaction. Belongs to the patients module.
 */
@Component
public class PatientProfileCreator {

    private final PtInfoRepository ptInfoRepository;

    public PatientProfileCreator(PtInfoRepository ptInfoRepository) {
        this.ptInfoRepository = ptInfoRepository;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.user();
        if (user.getRole() != Role.PATIENT) {
            return;
        }
        PtInfo patient = new PtInfo();
        patient.setEmail(user.getEmail());
        patient.setUserName(user.getUsername());
        patient.setUser(user);
        patient.setPatientName(user.getUsername());
        patient.setPatientAddress("Not provided");
        patient.setContactNo(user.getPhoneNumber());
        patient.setPatientAadharNo(null);
        patient.setGender(Gender.OTHER);
        patient.setDob(null); // asked for on the profile page instead of a fake date
        patient.setCreatedAt(user.getCreatedAt());
        ptInfoRepository.save(patient);
    }
}
