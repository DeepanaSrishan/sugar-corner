package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Order;
import com.brownieshop.model.Payment;
import com.brownieshop.service.OrderService;
import com.brownieshop.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    private Customer getLoggedIn(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customerUser");
        if (c != null) return c;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && !fallback.isAdmin()) return fallback;
        return null;
    }

    // ── My Payments list ─────────────────────────────────────
    @GetMapping
    public String myPayments(HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        List<Payment> payments = paymentService.getByCustomer(c.getId());

        // ✅ FIXED: use getStatus() (NOT getPaymentStatus())
        long completedCount = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .count();

        long pendingCount = payments.stream()
                .filter(p -> "PENDING".equals(p.getStatus()))
                .count();

        model.addAttribute("payments", payments);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("customer", c);

        return "payments/my-payments";
    }

    // ── Card / Bank details form ──────────────────────────────
    @GetMapping("/card-details")
    public String cardDetailsPage(@RequestParam Long orderId,
                                  @RequestParam String method,
                                  HttpSession session,
                                  Model model) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Order> opt = orderService.getById(orderId);

        if (opt.isEmpty() ||
                !opt.get().getCustomerId().equals(c.getId())) {
            return "redirect:/orders";
        }

        if (paymentService.getByOrderId(orderId).isPresent()) {
            return "redirect:/orders/" + orderId;
        }

        model.addAttribute("order", opt.get());
        model.addAttribute("method", method);
        model.addAttribute("customer", c);

        return "payments/card-details";
    }

    // ── Payment detail page ──────────────────────────────────
    @GetMapping("/{id}")
    public String paymentDetail(@PathVariable Long id,
                                HttpSession session,
                                Model model) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Payment> opt = paymentService.getById(id);

        if (opt.isEmpty()) return "redirect:/payments";

        if (!opt.get().getCustomerId().equals(c.getId())) {
            return "redirect:/payments";
        }

        Optional<Order> order = orderService.getById(opt.get().getOrderId());

        model.addAttribute("payment", opt.get());
        model.addAttribute("customer", c);
        order.ifPresent(o -> model.addAttribute("order", o));

        return "payments/payment-detail";
    }

    // ── Choose payment method ────────────────────────────────
    @GetMapping("/pay/{orderId}")
    public String paymentPage(@PathVariable Long orderId,
                              HttpSession session,
                              Model model) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Order> opt = orderService.getById(orderId);

        if (opt.isEmpty() ||
                !opt.get().getCustomerId().equals(c.getId())) {
            return "redirect:/orders";
        }

        Optional<Payment> existing = paymentService.getByOrderId(orderId);

        if (existing.isPresent()) {
            return "redirect:/payments/" + existing.get().getId();
        }

        model.addAttribute("order", opt.get());
        model.addAttribute("customer", c);

        return "payments/pay";
    }

    // ── Submit payment method ────────────────────────────────
    @PostMapping("/submit")
    public String submitPayment(@RequestParam Long orderId,
                                @RequestParam String paymentMethod,
                                HttpSession session) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Order> opt = orderService.getById(orderId);

        if (opt.isEmpty() ||
                !opt.get().getCustomerId().equals(c.getId())) {
            return "redirect:/orders";
        }

        if (paymentService.getByOrderId(orderId).isPresent()) {
            return "redirect:/orders/" + orderId;
        }

        // Card or Bank → go to details page
        if ("CARD".equals(paymentMethod) || "BANK_TRANSFER".equals(paymentMethod)) {
            return "redirect:/payments/card-details?orderId=" + orderId + "&method=" + paymentMethod;
        }

        // COD → create immediately
        long paymentId = paymentService.createPayment(
                orderId,
                c.getId(),
                opt.get().getTotalAmount(),
                paymentMethod
        );

        return "redirect:/payments/" + paymentId + "?confirmed=true";
    }

    // ── Submit card / bank details ───────────────────────────
    @PostMapping("/card-submit")
    public String submitCardPayment(@RequestParam Long orderId,
                                    @RequestParam String paymentMethod,
                                    @RequestParam(required = false) String cardNumber,
                                    @RequestParam(required = false) String bankRef,
                                    HttpSession session) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Order> opt = orderService.getById(orderId);

        if (opt.isEmpty() ||
                !opt.get().getCustomerId().equals(c.getId())) {
            return "redirect:/orders";
        }

        if (paymentService.getByOrderId(orderId).isPresent()) {
            return "redirect:/orders/" + orderId;
        }

        String txRef = null;

        if ("CARD".equals(paymentMethod) && cardNumber != null && cardNumber.length() >= 4) {
            String digits = cardNumber.replaceAll("\\s", "");
            txRef = "CARD-****-" + digits.substring(digits.length() - 4);
        } else if ("BANK_TRANSFER".equals(paymentMethod) && bankRef != null && !bankRef.isBlank()) {
            txRef = "BANK-" + bankRef.trim().toUpperCase();
        }

        long paymentId = paymentService.createPaymentWithRef(
                orderId,
                c.getId(),
                opt.get().getTotalAmount(),
                paymentMethod,
                txRef
        );

        return "redirect:/payments/" + paymentId + "?confirmed=true";
    }
}