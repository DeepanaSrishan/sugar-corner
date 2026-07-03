package com.brownieshop.controller;

import com.brownieshop.model.Customer;
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
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private OrderService   orderService;

    private Customer getAdmin(HttpSession session) {
        Customer admin = (Customer) session.getAttribute("adminUser");
        if (admin != null && admin.isAdmin()) return admin;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && fallback.isAdmin()) return fallback;
        return null;
    }

    private boolean isAdmin(HttpSession session) {
        Customer c = getAdmin(session);
        return c != null && c.isAdmin();
    }

    // Adds admin name + notifCount to every model so sidebar shows correctly
    private void addCommon(HttpSession session, Model model) {
        model.addAttribute("admin",      getAdmin(session));
        model.addAttribute("notifCount", orderService.getPendingOrderCount());
    }

    // ── All Payments with search/filter ──────────────────────────
    // FIX: Now reads paymentId, date, status from GET params
    //      and passes todayRevenue which the template requires
    @GetMapping
    public String listPayments(
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            HttpSession session, Model model) {

        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(session, model);

        // Use search() when any filter is active, otherwise return all
        List<Payment> payments = paymentService.search(paymentId, date, status);

        model.addAttribute("payments",       payments);
        model.addAttribute("totalRevenue",   paymentService.getTotalRevenue());
        model.addAttribute("completedCount", paymentService.getCompletedPaymentCount());
        model.addAttribute("pendingCount",   paymentService.getPendingPaymentCount());
        // todayRevenue required by payments.html stat strip
        model.addAttribute("todayRevenue",   paymentService.getTodayRevenue());
        // Keep filter values so the form shows what was searched
        model.addAttribute("searchPaymentId", paymentId);
        model.addAttribute("searchDate",      date);
        model.addAttribute("searchStatus",    status);
        return "admin/payments";
    }

    // ── Payment detail ────────────────────────────────────────────
    @GetMapping("/{id}")
    public String paymentDetail(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(session, model);
        Optional<Payment> opt = paymentService.getById(id);
        if (opt.isEmpty()) return "redirect:/admin/payments";
        model.addAttribute("payment", opt.get());
        return "admin/payment-detail";
    }

    // ── Mark COD / bank payment as completed ──────────────────────
    @PostMapping("/{id}/complete")
    public String markComplete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        paymentService.markCompleted(id);
        return "redirect:/admin/payments?success=completed";
    }

    // ── Process refund ────────────────────────────────────────────
    @PostMapping("/{id}/refund")
    public String refund(@PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "") String reason,
                         HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        paymentService.processRefund(id, reason);
        return "redirect:/admin/payments?success=refunded";
    }
}