package ITmonteur.example.hospitalERP.dto;

public class RegisterRequestDTO {

    private String username;
    private String password;
    private String role; // should match Role enum (ADMIN, DOCTOR, PATIENT, RECEPTIONIST)

    // getters & setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
