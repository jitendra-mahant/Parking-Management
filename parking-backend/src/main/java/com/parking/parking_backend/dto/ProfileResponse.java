package com.parking.parking_backend.dto;

public class ProfileResponse {
    private String name;
    private String email;
    private String phone;
    private String role;

    public ProfileResponse(String name, String email, String phone, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
}