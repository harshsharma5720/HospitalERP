package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Gender;
import jakarta.persistence.OneToMany;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class PtInfoDTO {
    private long patientId;
    private String patientName;
    private String email;
    private String patientAddress;
    private long patientAadharNo;
    private String contactNo;
    private LocalDate dob;
    private Gender gender;
    private List<AppointmentDTO> appointment;
    private String userName;
    private String profileImage;

    public PtInfoDTO() {
    }

    public PtInfoDTO(long patientId, String patientName,String email, String patientAddress,
                     long patientAadharNo,String contactNo, LocalDate dob,Gender gender,
                     List<AppointmentDTO> appointment, String userName, String profileImage) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.email=email;
        this.patientAddress = patientAddress;
        this.patientAadharNo = patientAadharNo;
        this.contactNo = contactNo;
        this.dob = dob;
        this.gender = gender;
        this.appointment = appointment;
        this.userName=userName;
        this.profileImage=profileImage;
    }

    public long getPatientId() {
        return patientId;
    }

    public void setPatientId(long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public long getPatientAadharNo() {
        return patientAadharNo;
    }

    public void setPatientAadharNo(long patientAadharNo) {
        this.patientAadharNo = patientAadharNo;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public List<AppointmentDTO> getAppointment() {
        return appointment;
    }

    public void setAppointment(List<AppointmentDTO> appointment) {
        this.appointment = appointment;
    }

    public String getUserName() { return userName;  }

    public void setUserName(String userName) { this.userName = userName; }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
