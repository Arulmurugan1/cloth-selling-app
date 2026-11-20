package com.clothapp.gateway.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

public final class SecurityDtos {

    private SecurityDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Pattern(regexp = "^[0-9]{10,15}$", message = "mobile must be 10-15 digits") String mobile,
            @NotBlank @Pattern(regexp = "^.{4,12}$", message = "password must be 4 to 12 characters") String password,
            @NotNull LocalDate dob
    ) {
    }

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank @Pattern(regexp = "^.{4,12}$", message = "password must be 4 to 12 characters") String password
    ) {
    }

    public record AuthResponse(String token, long expiresInSeconds, UserDto user) {
    }

    public record UserDto(String id, String fullName, String email, String mobile, String role, boolean active)
            implements Serializable {
        static UserDto from(AppUser u) {
            return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getRole().name(), u.isActive());
        }
    }

    public record UserRoleUpdate(String role) {
    }

    public record UserActiveUpdate(Boolean active) {
    }
}
