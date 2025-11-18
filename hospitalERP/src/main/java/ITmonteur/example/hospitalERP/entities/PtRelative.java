package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "patient_relative")
public class PtRelative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private LocalDate dob;
    @Enumerated(EnumType.STRING)
    private RelationShip relationship;
    @Column(nullable = true)
    private Long patientAadharNo;
    @Enumerated(EnumType.STRING)
    private Role role = Role.PATIENT;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PtInfo ptInfo;

    public PtRelative() {}

    public PtRelative(Long id, String name, Gender gender, LocalDate dob,
                      RelationShip relationship, Long patientAadharNo,Role role ,PtInfo ptInfo) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.relationship = relationship;
        this.patientAadharNo = patientAadharNo;
        this.role=role;
        this.ptInfo = ptInfo;
    }

    // Getters and Setters

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public PtInfo getPtInfo() {
        return ptInfo;
    }

    public void setPtInfo(PtInfo ptInfo) {
        this.ptInfo = ptInfo;
    }
}
