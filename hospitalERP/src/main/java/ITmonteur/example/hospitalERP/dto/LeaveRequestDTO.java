package ITmonteur.example.hospitalERP.dto;

import java.time.LocalDate;

public class LeaveRequestDTO {

    private Long id;
    private Long userId;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    public LeaveRequestDTO() {
    }

    public LeaveRequestDTO(Long id ,Long userId, String role, LocalDate startDate, LocalDate endDate,
                           String reason) {
        this.id=id;
        this.userId = userId;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
