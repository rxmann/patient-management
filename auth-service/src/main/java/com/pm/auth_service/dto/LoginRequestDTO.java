package com.pm.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {
    @NotBlank(message = "Email is required.")
    @Email(message = "Email should be a valid email.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    private String password;

    public @NotBlank(message = "Email is required.") @Email(message = "Email should be a valid email.") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required.") @Email(message = "Email should be a valid email.") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Password is required.") @Size(min = 8, message = "Password must be at least 8 characters long.") String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "LoginRequestDTO{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public void setPassword(@NotBlank(message = "Password is required.") @Size(min = 8, message = "Password must be at least 8 characters long.") String password) {
        this.password = password;
    }
}