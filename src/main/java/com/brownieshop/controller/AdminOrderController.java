package com.brownieshop.controller;

import com.brownieshop.dao.AdminSettingsDAO;
import com.brownieshop.model.Customer;
import com.brownieshop.model.Order;
import com.brownieshop.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired private OrderService       orderService;
    @Autowired private AdminSettingsDAO   adminSettingsDAO;

    /**
     * Creates the admin_settings table on startup if it doesn't exist yet.
     * This means the table is always ready before any request comes in.
     */
    @PostConstruct
    public void init() {
        adminSettingsDAO.ensureTableExists();
    }

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

    /**
     * Builds notification bell data for every admin page.
     *
     * HOW THE WATERMARK WORKS (DB-based, survives logout):
     *   - notifReadAtId is stored in admin_settings table (persists across sessions)
     *   - notifCount   = PENDING orders with id > notifReadAtId  (truly new)
     *   - newOrdersList = those new orders shown in the bell dropdown
     *
     * When admin clicks "Mark all read":
     *   - POST /admin/orders/notifications/mark-read is called
     *   - Current max order ID is saved to DB as new watermark
     *   - On next page load: notifCount = 0 (nothing above watermark)
     *
     * When a new order arrives after mark-read:
     *   - New order ID > watermark → shows in bell with count = 1
     */
    private void addCommon(HttpSession session, Model model) {
        model.addAttribute("admin", getAdmin(session));
        // Read watermark from DB (not session — survives logout)
        long readAtId = adminSettingsDAO.getNotifReadAtId();

        List<Order> allPending = orderService.getAllOrders().stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .collect(Collectors.toList());

        // New = PENDING orders with id ABOVE the DB watermark
        List<Order> newOrders = allPending.stream()
                .filter(o -> o.getId() != null && o.getId() > readAtId)
                .collect(Collectors.toList());

        model.addAttribute("notifCount",       newOrders.size());
        model.addAttribute("newOrdersList",    newOrders);
        model.addAttribute("pendingOrdersList", allPending);
    }

    // ── Order list ────────────────────────────────────────────
    @GetMapping
    public String listOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String date,
            HttpSession session, Model model) {

        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(session, model);

        List<Order> orders = orderService.searchOrders(orderId, date);
        model.addAttribute("orders",         orders);
        model.addAttribute("searchOrderId",  orderId);
        model.addAttribute("searchDate",     date);
        model.addAttribute("pendingCount",   orderService.getPendingOrderCount());
        model.addAttribute("newToday",       orderService.getNewOrdersTodayCount());
        return "admin/orders";
    }

    // ── Order detail ──────────────────────────────────────────
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(session, model);

        Optional<Order> opt = orderService.getById(id);
        if (opt.isEmpty()) return "redirect:/admin/orders";
        model.addAttribute("order",        opt.get());
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
        String normalizedStatus = status == null
            ? ""
            : status.trim().toUpperCase().replace('-', '_').replace(' ', '_');

        orderService.updateStatus(id, normalizedStatus);
        String success = "DELIVERED".equals(normalizedStatus) ? "delivered" : "status";
        return "redirect:/admin/orders/" + id + "?success=" + success;
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

    /**
     * Mark all as read — saves current max order ID to DB.
     *
     * After this:
     *   - Badge shows 0 across all page loads (even after logout/login)
     *   - Only orders with a HIGHER ID (placed after this moment) will
     *     reappear in the bell
     *
     * Returns "0" as plain text so JS can confirm success.
     */
    @PostMapping("/notifications/mark-read")
    @ResponseBody
    public String markNotificationsRead(HttpSession session) {
        if (!isAdmin(session)) return "error";

        // Find max order ID currently in system
        long maxId = orderService.getAllOrders().stream()
                .mapToLong(o -> o.getId() != null ? o.getId() : 0L)
                .max()
                .orElse(0L);

        // Save to DB — persists across logout/login
        adminSettingsDAO.setNotifReadAtId(maxId);
        return "0";
    }
}