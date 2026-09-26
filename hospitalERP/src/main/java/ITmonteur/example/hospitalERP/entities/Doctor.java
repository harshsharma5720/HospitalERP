package ITmonteur.example.hospitalERP.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.itmonteur.hospitalerp.identity.User;

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Specialist specialist; // CARDIOLOGY, DENTISTRY, etc.
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String phoneNumber;
    private String userName;
    private String profileImage;

    // No cascade: the login account belongs to the identity module and is deleted explicitly
    // (UserAccountService). @OnDelete only affects the generated FK (ON DELETE CASCADE).
    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", referencedColumnName = "id") // FK column in Doctor table
    private User user;

    public Doctor() {
    }

    public Doctor(Long id, String name, Specialist specialist, String email, String phoneNumber,
                  String profileImage, String userName, User user) {
        this.id = id;
        this.name = name;
        this.specialist = specialist;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userName = userName;
        this.profileImage = profileImage;
        this.user = user;
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

    public Specialist getSpecialist() {
        return specialist;
    }

    public void setSpecialist(Specialist specialist) {
        this.specialist = specialist;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }



    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }



    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }



    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
