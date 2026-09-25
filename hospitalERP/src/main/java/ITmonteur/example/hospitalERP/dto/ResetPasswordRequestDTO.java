package ITmonteur.example.hospitalERP.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequestDTO {

    @NotBlank(message = "Username or email is required")
    private String identifier;
    @NotBlank(message = "Code is required")
    private String code;
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String newPassword;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
