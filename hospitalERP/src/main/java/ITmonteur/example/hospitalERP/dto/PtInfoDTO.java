package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Appointment;
import ITmonteur.example.hospitalERP.entities.Gender;
import jakarta.persistence.OneToMany;

import java.util.Date;

public class PtInfoDTO {
    private long patientId;
    private String patientName;
    private String email;
    private String patientAddress;
    private long patientAadharNo;
    private long contactNo;
    private Date dob;
    private Gender gender;
    private Appointment appointment;

    public PtInfoDTO() {
    }

    public PtInfoDTO(long patientId, String patientName,String email, String patientAddress, long patientAadharNo, long contactNo, Date dob,Gender gender, Appointment appointment) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.email=email;
        this.patientAddress = patientAddress;
        this.patientAadharNo = patientAadharNo;
        this.contactNo = contactNo;
        this.dob = dob;
        this.gender = gender;
        this.appointment = appointment;
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

    public long getContactNo() {
        return contactNo;
    }

    public void setContactNo(long contactNo) {
        this.contactNo = contactNo;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}
