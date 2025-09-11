package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class PtInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long patientId;
    private String patientName;
    private String patientAddress;
    private long patientAadharNo;
    private long contactNo;
    private Date dob;
    @OneToMany
    private Appointment appointment;

    public PtInfo() {
    }

    public PtInfo(long patientId, String patientName, String patientAddress, long patientAadharNo, long contactNo, Date dob, Appointment appointment) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientAddress = patientAddress;
        this.patientAadharNo = patientAadharNo;
        this.contactNo = contactNo;
        this.dob = dob;
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

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}
