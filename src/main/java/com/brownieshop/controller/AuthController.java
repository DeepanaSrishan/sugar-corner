package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    // ── Home / Landing ─────────────────────────────────────────
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // ── Register ───────────────────────────────────────────────
    @GetMapping("/register")
    public String showRegister(HttpSession session) {
        if (session.getAttribute("customer") != null) return "redirect:/customer/dashboard";
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(required = false) String phone,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        Customer c = new Customer();
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setEmail(email);
        c.setPassword(password);
        c.setPhone(phone);

        long newId = authService.register(c);
        if (newId == -1L) {
            model.addAttribute("error", "This email address is already registered.");
            return "register";
        }

        model.addAttribute("success", "Account created successfully! You can now log in.");
        return "login";
    }

    // ── Login ──────────────────────────────────────────────────
    @GetMapping("/login")
    public String showLogin(HttpSession session) {
        if (session.getAttribute("customer") != null) {
            Customer c = (Customer) session.getAttribute("customer");
            return c.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/customer/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Optional<Customer> opt = authService.login(email, password);

        if (opt.isEmpty()) {
            model.addAttribute("error", "Invalid email / password, or account is blocked.");
            return "login";
        }

        Customer c = opt.get();
        session.setAttribute("customer", c);
        session.setAttribute("customerId", c.getId());

        if (c.isAdmin()) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/customer/dashboard";
    }

    // ── Admin Login (separate page) ────────────────────────────
    @GetMapping("/admin/login")
    public String showAdminLogin(HttpSession session) {
        if (session.getAttribute("customer") != null) {
            Customer c = (Customer) session.getAttribute("customer");
            if (c.isAdmin()) return "redirect:/admin/dashboard";
        }
        return "admin/admin-login";
    }

    @PostMapping("/admin/login")
    public String adminLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Optional<Customer> opt = authService.login(email, password);

        if (opt.isEmpty() || !opt.get().isAdmin()) {
            model.addAttribute("error", "Invalid admin credentials.");
            return "admin/admin-login";
        }

        session.setAttribute("customer", opt.get());
        return "redirect:/admin/dashboard";
    }

    // ── Logout ─────────────────────────────────────────────────
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    // ── Reset Password ─────────────────────────────────────────
    @GetMapping("/reset-password")
    public String showReset() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        boolean ok = authService.resetPassword(email, newPassword);
        if (!ok) {
            model.addAttribute("error", "No account found with that email.");
            return "reset-password";
        }

        model.addAttribute("success", "Password reset successfully. Please log in.");
        return "login";
    }
}