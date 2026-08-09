package com.parking.parking_backend.dto;

public class ProfileResponse {
    private String name;
    private String email;
    private String phone;
    private String role;
    private String address;
    private String photoBase64;

    public ProfileResponse(String name, String email, String phone, String role, String address, String photoBase64) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.address = address;
        this.photoBase64 = photoBase64;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getAddress() { return address; }
    public String getPhotoBase64() { return photoBase64; }
}