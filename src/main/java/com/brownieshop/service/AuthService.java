package com.brownieshop.service;

import com.brownieshop.dao.CustomerDAO;
import com.brownieshop.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private CustomerDAO customerDAO;

    // ── Register ──────────────────────────────────────────────
    /**
     * Registers a new customer.
     * Returns the new customer's ID, or -1 if email already exists.
     */
    public long register(Customer customer) {
        if (customerDAO.emailExists(customer.getEmail())) {
            return -1L;   // email taken
        }
        // Password stored as-is (plain text) per project requirement.
        // BCrypt hashing will be added in a later sprint.
        customer.setStatus("ACTIVE");
        customer.setRole("CUSTOMER");
        return customerDAO.save(customer);
    }

    // ── Login ─────────────────────────────────────────────────
    /**
     * Authenticates a user by email + password.
     * Returns the Customer if credentials match and account is ACTIVE, else empty.
     */
    public Optional<Customer> login(String email, String password) {
        Optional<Customer> opt = customerDAO.findByEmail(email);
        if (opt.isEmpty()) return Optional.empty();

        Customer c = opt.get();

        // Block check
        if (!"ACTIVE".equalsIgnoreCase(c.getStatus())) {
            return Optional.empty();
        }

        // Plain-text password comparison (hashing added later)
        if (!password.equals(c.getPassword())) {
            return Optional.empty();
        }

        return Optional.of(c);
    }

    // ── Reset Password ────────────────────────────────────────
    /**
     * Resets password for the given email.
     * Returns true if the account exists and password was updated.
     */
    public boolean resetPassword(String email, String newPassword) {
        Optional<Customer> opt = customerDAO.findByEmail(email);
        if (opt.isEmpty()) return false;

        customerDAO.updatePassword(opt.get().getId(), newPassword);
        return true;
    }
}