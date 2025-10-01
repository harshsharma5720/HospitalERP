package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    @OneToMany(mappedBy = "ptInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    private Role role = Role.PATIENT;
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public PtInfo() {
    }

    public PtInfo(long patientId, String patientName, String patientAddress, long patientAadharNo, long contactNo, Date dob, List<Appointment> appointments, User user) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientAddress = patientAddress;
        this.patientAadharNo = patientAadharNo;
        this.contactNo = contactNo;
        this.dob = dob;
        this.appointments = appointments;
        this.user = user;
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

    public List<Appointment> getAppointment() {
        return appointments;
    }

    public void setAppointment(List<Appointment> appointment) {
        this.appointments = appointment;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
