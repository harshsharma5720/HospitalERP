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
    private String gender;

    private int age;

    // 🧾 Employment Details
    private String shiftTiming;   // Example: "Morning", "Evening"
    private String department;    // Example: "Front Desk", "Billing"

    // 🔐 Authentication Info
    private String username;
    private String password;

    // 📈 Activity / Status
    private String status = "ACTIVE"; // ACTIVE / INACTIVE / ON_LEAVE
    private String joiningDate;

}
