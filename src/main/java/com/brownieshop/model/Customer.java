package com.brownieshop.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer entity – maps to the `customers` table.
 * Password is stored as plain text for now (will be hashed in a later sprint).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String password;          // plain-text for now

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;
    private String country;

    private LocalDate dateOfBirth;
    private String profilePhotoPath;  // relative path inside /uploads/

    // ACTIVE | BLOCKED
    private String status;

    // CUSTOMER | ADMIN
    private String role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Convenience helpers ──────────────────────────────────
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}