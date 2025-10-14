package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long appointmentID;
    private String patientName;
    private String email;
    private  long phoneNo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    private int age;
    private String doctor;
    @ManyToOne
    @JoinColumn(name = "patient_id", referencedColumnName = "patientId") // foreign key in Appointment table
    private PtInfo ptInfo;

    public Appointment(long appointmentID, String patientName, String email, long phoneNo, Gender gender, int age, String doctor, PtInfo ptInfo) {
        this.appointmentID = appointmentID;
        this.patientName = patientName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.gender = gender;
        this.age = age;
        this.doctor = doctor;
        this.ptInfo = ptInfo;
    }

    public Appointment() {
    }

    public long getAppointmentID() {
        return appointmentID;
    }

    public void setAppointmentID(long appointmentID) {
        this.appointmentID = appointmentID;
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

    public long getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(long phoneNo) {
        this.phoneNo = phoneNo;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }
    public PtInfo getPtInfo() {
        return ptInfo;
    }

    public void setPtInfo(PtInfo ptInfo) {
        this.ptInfo = ptInfo;
    }
}
