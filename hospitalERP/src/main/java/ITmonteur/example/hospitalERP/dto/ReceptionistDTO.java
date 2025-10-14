package ITmonteur.example.hospitalERP.dto;

import ITmonteur.example.hospitalERP.entities.Gender;

public class ReceptionistDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String userName;
    private String gender;
    private int age;

    public ReceptionistDTO() {
    }

    public ReceptionistDTO(Long id, String name, String email, String phone,String userName ,String gender, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.userName=userName;
        this.gender = gender;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
