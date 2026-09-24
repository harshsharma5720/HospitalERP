package ITmonteur.example.hospitalERP.dto;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequestDTO {

    @NotBlank(message = "Username or email is required")
    private String identifier;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
}
