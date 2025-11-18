package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Gender;
import ITmonteur.example.hospitalERP.entities.RelationShip;

import java.time.LocalDate;

public class PtRelativeDTO {

    private Long id;
    private String name;
    private Gender gender;
    private LocalDate dob;
    private RelationShip relationship;
    private Long patientAadharNo;

    // ID of the patient to which this relative belongs
    private Long patientId;

    public PtRelativeDTO() {}

    public PtRelativeDTO(Long id, String name, Gender gender, LocalDate dob,
                         RelationShip relationship, Long patientAadharNo, Long patientId) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.relationship = relationship;
        this.patientAadharNo = patientAadharNo;
        this.patientId = patientId;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public RelationShip getRelationship() {
        return relationship;
    }

    public void setRelationship(RelationShip relationship) {
        this.relationship = relationship;
    }

    public Long getPatientAadharNo() {
        return patientAadharNo;
    }

    public void setPatientAadharNo(Long patientAadharNo) {
        this.patientAadharNo = patientAadharNo;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
}
