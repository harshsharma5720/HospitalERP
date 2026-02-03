package ITmonteur.example.hospitalERP.dto;

public class DoctorDTO {

    private Long id;
    private Long userId;
    private String name;
    private String specialist;
    private String email;
//    private String password; // encrypted
    private String phoneNumber;
    private String userName;
    private String profileImage;

    public DoctorDTO() {
    }

    public DoctorDTO(Long id,Long userId ,String name, String specialist, String email, String phoneNumber
            , String userName, String profileImage) {
        this.id = id;
        this.userId=userId;
        this.name = name;
        this.specialist = specialist;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userName=userName;
        this.profileImage=profileImage;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialist() {
        return specialist;
    }

    public void setSpecialist(String specialist) {
        this.specialist = specialist;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
