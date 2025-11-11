package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "receptionist")
public class Receptionist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    private String userName;
    private String profileImage;
    @Enumerated(EnumType.STRING)
    private Role role = Role.RECEPTIONIST;
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id") // FK column in Doctor table
    private User user;

    private int age;

//    // 🧾 Employment Details
//    private String shiftTiming;   // Example: "Morning", "Evening"
//    private String department;    // Example: "Front Desk", "Billing"
//
//    // 🔐 Authentication Info
//    private String username;
//    private String password;
//
//    // 📈 Activity / Status
//    private String status = "ACTIVE"; // ACTIVE / INACTIVE / ON_LEAVE
//    private String joiningDate;

    public Receptionist() {
    }

    public Receptionist(Long id, String name, String email, String phone, Gender gender, String userName
            ,String profileImage ,Role role, User user, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.userName=userName;
        this.profileImage=profileImage;
        this.role = role;
        this.user = user;
        this.age = age;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
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

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
