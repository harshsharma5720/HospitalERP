package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient")
public class PtInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;
    private String patientName;
    @Column(unique = true , nullable = false)
    private String email;
    private String patientAddress;
    @Column(unique = false, nullable = true)
    private Long patientAadharNo;
    @Column(nullable = true ,length = 15)
    private String contactNo;
    private LocalDate dob;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private String userName;
    private String profileImage;
    @OneToMany(mappedBy = "ptInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PtRelative> relatives = new ArrayList<>();
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public PtInfo() {
    }

    public PtInfo(Long patientId, String patientName, String email, String patientAddress,
                  Long patientAadharNo, String contactNo, LocalDate dob, Gender gender,
                  String userName, String profileImage, List<PtRelative> relatives, User user) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.email = email;
        this.patientAddress = patientAddress;
        this.patientAadharNo = patientAadharNo;
        this.contactNo = contactNo;
        this.dob = dob;
        this.gender = gender;
        this.userName = userName;
        this.profileImage = profileImage;
        this.relatives = relatives;
        this.user = user;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
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

    public Long getPatientAadharNo() {
        return patientAadharNo;
    }

    public void setPatientAadharNo(Long patientAadharNo) {
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



    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }



    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public List<PtRelative> getRelatives() {
        return relatives;
    }

    public void setRelatives(List<PtRelative> relatives) {
        this.relatives = relatives;
    }
}
