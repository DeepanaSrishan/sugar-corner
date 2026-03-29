package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // ── Dashboard ──────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Customer c = (Customer) session.getAttribute("customer");
        if (c == null) return "redirect:/login";
        model.addAttribute("customer", c);
        return "customer/dashboard";
    }

    // ── View Profile ──────────────────────────────────────────
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        Customer c = (Customer) session.getAttribute("customer");
        if (c == null) return "redirect:/login";

        // Refresh from DB in case admin updated status
        Optional<Customer> fresh = customerService.getById(c.getId());
        fresh.ifPresent(customer -> {
            session.setAttribute("customer", customer);
            model.addAttribute("customer", customer);
        });
        return "customer/profile";
    }

    // ── Update Profile ────────────────────────────────────────
    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String addressLine1,
            @RequestParam(required = false) String addressLine2,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String country,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            HttpSession session,
            Model model) {

        Customer c = (Customer) session.getAttribute("customer");
        if (c == null) return "redirect:/login";

        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setPhone(phone);
        c.setAddressLine1(addressLine1);
        c.setAddressLine2(addressLine2);
        c.setCity(city);
        c.setPostalCode(postalCode);
        c.setCountry(country);
        c.setDateOfBirth(dateOfBirth);

        customerService.updateProfile(c);
        session.setAttribute("customer", c);

        model.addAttribute("customer", c);
        model.addAttribute("success", "Profile updated successfully!");
        return "customer/profile";
    }

    // ── Upload Profile Photo ───────────────────────────────────
    @PostMapping("/profile/photo")
    public String uploadPhoto(
            @RequestParam("photo") MultipartFile photo,
            HttpSession session,
            Model model) {

        Customer c = (Customer) session.getAttribute("customer");
        if (c == null) return "redirect:/login";

        String path = customerService.uploadProfilePhoto(c.getId(), photo);
        if (path != null) {
            c.setProfilePhotoPath(path);
            session.setAttribute("customer", c);
            model.addAttribute("success", "Profile photo updated!");
        } else {
            model.addAttribute("error", "Photo upload failed. Please try again.");
        }

        model.addAttribute("customer", c);
        return "customer/profile";
    }
}