package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Order;
import com.brownieshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired private OrderService orderService;

    private boolean isAdmin(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customer");
        return c != null && c.isAdmin();
    }

    // ── Order list with search ────────────────────────────────
    @GetMapping
    public String listOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String date,
            HttpSession session, Model model) {

        if (!isAdmin(session)) return "redirect:/admin/login";

        List<Order> orders = orderService.searchOrders(orderId, date);
        model.addAttribute("orders", orders);
        model.addAttribute("searchOrderId", orderId);
        model.addAttribute("searchDate", date);
        model.addAttribute("pendingCount", orderService.getPendingOrderCount());
        model.addAttribute("newToday", orderService.getNewOrdersTodayCount());
        return "admin/orders";
    }

    // ── Order detail ─────────────────────────────────────────
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        Optional<Order> opt = orderService.getById(id);
        if (opt.isEmpty()) return "redirect:/admin/orders";

        model.addAttribute("order", opt.get());
        model.addAttribute("pendingCount", orderService.getPendingOrderCount());
        return "admin/order-detail";
    }

    // ── Update status ─────────────────────────────────────────
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders/" + id + "?success=status";
    }

    // ── Save admin note ───────────────────────────────────────
    @PostMapping("/{id}/note")
    public String saveNote(
            @PathVariable Long id,
            @RequestParam String adminNote,
            HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        orderService.saveAdminNote(id, adminNote);
        return "redirect:/admin/orders/" + id + "?success=note";
    }
}