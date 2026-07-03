package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Order;
import com.brownieshop.service.CustomerService;
import com.brownieshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired private CustomerService customerService;
    @Autowired private OrderService    orderService;   // ← NEW: needed for dashboard stats

    private Customer getLoggedInCustomer(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customerUser");
        if (c != null) return c;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && !fallback.isAdmin()) return fallback;
        return null;
    }

    // ── Dashboard ──────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Customer c = getLoggedInCustomer(session);
        if (c == null) return "redirect:/login";

        // Load all orders for this customer to calculate stats
        List<Order> orders = orderService.getOrdersByCustomer(c.getId());

        // Count orders by status for the dashboard stat cards
        long totalOrders   = orders.size();
        long delivered     = orders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        long inProgress    = orders.stream().filter(o ->
                "CONFIRMED".equals(o.getStatus()) ||
                        "IN_PREPARATION".equals(o.getStatus()) ||
                        "READY".equals(o.getStatus())).count();
        long pending       = orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        OrderService.LoyaltyProgress loyalty = orderService.getLoyaltyProgress(c.getId());

        model.addAttribute("customer",    c);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("delivered",   delivered);
        model.addAttribute("inProgress",  inProgress);
        model.addAttribute("pending",     pending);
        model.addAttribute("loyaltyTier", loyalty.getTier());
        model.addAttribute("loyaltyTierLabel", loyalty.getTierLabel());
        model.addAttribute("loyaltyDiscountPercent", loyalty.getDiscountPercent());
        model.addAttribute("loyaltyCompletedOrders", loyalty.getCompletedOrders());
        model.addAttribute("loyaltyOrdersToNextTier", loyalty.getOrdersToNextTier());
        model.addAttribute("loyaltyNextTierLabel", loyalty.getNextTierLabel());
        model.addAttribute("loyaltyProgressPercent", loyalty.getProgressPercent());

        return "customer/dashboard";
    }

    // ── View Profile ──────────────────────────────────────────
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        Customer c = getLoggedInCustomer(session);
        if (c == null) return "redirect:/login";

        // Refresh from DB in case admin updated status
        Optional<Customer> fresh = customerService.getById(c.getId());
        fresh.ifPresent(customer -> {
            session.setAttribute("customerUser", customer);
            session.setAttribute("customer", customer);
            model.addAttribute("customer", customer);
        });

        Customer activeCustomer = getLoggedInCustomer(session);
        model.addAttribute("customer", activeCustomer);
        OrderService.LoyaltyProgress loyalty = orderService.getLoyaltyProgress(activeCustomer.getId());
        model.addAttribute("loyaltyTier", loyalty.getTier());
        model.addAttribute("loyaltyTierLabel", loyalty.getTierLabel());
        model.addAttribute("loyaltyDiscountPercent", loyalty.getDiscountPercent());
        model.addAttribute("loyaltyCompletedOrders", loyalty.getCompletedOrders());
        model.addAttribute("loyaltyOrdersToNextTier", loyalty.getOrdersToNextTier());
        model.addAttribute("loyaltyNextTierLabel", loyalty.getNextTierLabel());
        model.addAttribute("loyaltyProgressPercent", loyalty.getProgressPercent());

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

        Customer c = getLoggedInCustomer(session);
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
        session.setAttribute("customerUser", c);
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

        Customer c = getLoggedInCustomer(session);
        if (c == null) return "redirect:/login";

        String path = customerService.uploadProfilePhoto(c.getId(), photo);
        if (path != null) {
            c.setProfilePhotoPath(path);
            session.setAttribute("customerUser", c);
            session.setAttribute("customer", c);
            model.addAttribute("success", "Profile photo updated!");
        } else {
            model.addAttribute("error", "Photo upload failed. Please try again.");
        }

        model.addAttribute("customer", c);
        return "customer/profile";
    }
}