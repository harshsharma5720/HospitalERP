package ITmonteur.example.hospitalERP.dto;

public class AppointmentDTO {
    private long appointmentID;
    private String patientName;
    private String email;
    private  long phoneNo;
    private String gender;
    private int age;
    private String doctor;

    public AppointmentDTO(long appointmentID, String patientName, String email, long phoneNo, String gender, int age, String doctor) {
        this.appointmentID = appointmentID;
        this.patientName = patientName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.gender = gender;
        this.age = age;
        this.doctor = doctor;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
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
}
