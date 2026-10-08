package com.itmonteur.hospitalerp.patients;

import com.itmonteur.hospitalerp.common.Gender;

import java.time.LocalDate;

public class PtInfoDTO {
    private long patientId;
    private String patientName;
    private String email;
    private String patientAddress;
    private Long patientAadharNo;  // boxed so a missing Aadhaar stays null instead of 0
    private String contactNo;
    private LocalDate dob;
    private Gender gender;
    private String userName;
    private String profileImage;
    // false for a walk-in record registered at the front desk (no login account)
    private boolean hasLogin;

    public PtInfoDTO() {
    }

    public PtInfoDTO(long patientId, String patientName,String email, String patientAddress,
                     Long patientAadharNo,String contactNo, LocalDate dob,Gender gender,
                     String userName, String profileImage) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.email=email;
        this.patientAddress = patientAddress;
        this.patientAadharNo = patientAadharNo;
        this.contactNo = contactNo;
        this.dob = dob;
        this.gender = gender;
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



    public String getUserName() { return userName;  }

    public void setUserName(String userName) { this.userName = userName; }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isHasLogin() {
        return hasLogin;
    }

    public void setHasLogin(boolean hasLogin) {
        this.hasLogin = hasLogin;
    }
}
