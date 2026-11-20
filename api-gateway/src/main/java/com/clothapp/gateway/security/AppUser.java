package com.clothapp.gateway.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String mobile;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "dob", nullable = false)
    private Instant dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;

    protected AppUser() {
    }

    public AppUser(String email, String mobile, String fullName, String passwordHash, Instant dob) {
        this.email = email;
        this.mobile = mobile;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.dob = dob;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getDob() { return dob; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setRole(Role role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
}
