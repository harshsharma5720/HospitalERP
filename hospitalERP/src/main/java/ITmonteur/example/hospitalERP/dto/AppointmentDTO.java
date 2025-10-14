package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Gender;
import ITmonteur.example.hospitalERP.entities.PtInfo;

public class AppointmentDTO {
    private long appointmentID;
    private String patientName;
    private String email;
    private  long phoneNo;
    private Gender gender;
    private int age;
    private String doctor;
    private Long ptInfoId;

    public AppointmentDTO(long appointmentID, String patientName, String email, long phoneNo,
                          Gender gender, int age, String doctor , Long ptInfoId) {
        this.appointmentID = appointmentID;
        this.patientName = patientName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.gender = gender;
        this.age = age;
        this.doctor = doctor;
        this.ptInfoId=ptInfoId;
    }

    public AppointmentDTO() {
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

    public Long getPtInfoId() {
        return ptInfoId;
    }

    public void setPtInfoId(Long ptInfoId) {
        this.ptInfoId = ptInfoId;
    }
}
