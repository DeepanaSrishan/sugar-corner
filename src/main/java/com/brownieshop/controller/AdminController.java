package com.brownieshop.controller;

import com.brownieshop.dao.AdminSettingsDAO;
import com.brownieshop.model.Customer;
import com.brownieshop.model.Order;
import com.brownieshop.service.ContactService;
import com.brownieshop.service.CustomerService;
import com.brownieshop.service.OrderService;
import com.brownieshop.service.PaymentService;
import com.brownieshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private CustomerService  customerService;
    @Autowired private ProductService   productService;
    @Autowired private OrderService     orderService;
    @Autowired private PaymentService   paymentService;
    @Autowired private ContactService   contactService;
    @Autowired private AdminSettingsDAO adminSettingsDAO;

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

    /** Same DB-watermark logic as AdminOrderController */
    private void addNotifData(Model model) {
        long readAtId = adminSettingsDAO.getNotifReadAtId();

        List<Order> allPending = orderService.getAllOrders().stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .collect(Collectors.toList());

        List<Order> newOrders = allPending.stream()
                .filter(o -> o.getId() != null && o.getId() > readAtId)
                .collect(Collectors.toList());

        model.addAttribute("notifCount",       newOrders.size());
        model.addAttribute("newOrdersList",    newOrders);
        model.addAttribute("pendingOrdersList", allPending);
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        model.addAttribute("admin", getAdmin(session));
        addNotifData(model);

        List<Customer> customers = customerService.getAllCustomers();
        model.addAttribute("totalCustomers",  customers.size());
        model.addAttribute("activeCustomers",
                customers.stream().filter(Customer::isActive).count());
        model.addAttribute("blockedCustomers",
                customers.stream().filter(c -> "BLOCKED".equalsIgnoreCase(c.getStatus())).count());
        model.addAttribute("totalProducts",   productService.getAllProducts().size());
        model.addAttribute("totalOrders",     orderService.getAllOrders().size());
        model.addAttribute("pendingOrders",   orderService.getPendingOrderCount());
        model.addAttribute("newToday",        orderService.getNewOrdersTodayCount());
        model.addAttribute("totalRevenue",    paymentService.getTotalRevenue());
        model.addAttribute("pendingPayments", paymentService.getPendingPaymentCount());
        model.addAttribute("unreadInquiries", contactService.getUnreadInquiryCount());
        model.addAttribute("pendingFeedback", contactService.getPendingFeedbackCount());
        return "admin/dashboard";
    }

    @GetMapping("/customers")
    public String listCustomers(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin",     getAdmin(session));

        List<Customer> customers = customerService.getAllCustomers();
        model.addAttribute("customers", customers);

        // Loyalty tier data — one DB query for all customers
        Map<Long, Integer> orderCountMap = orderService.getAllCustomerOrderCounts();
        model.addAttribute("orderCountMap", orderCountMap);

        // Pre-compute tier string per customer so Thymeleaf doesn't need Java logic
        Map<Long, String> tierMap = new java.util.HashMap<>();
        Map<Long, String> tierLabelMap = new java.util.HashMap<>();
        for (Customer c : customers) {
            int cnt = orderCountMap.getOrDefault(c.getId(), 0);
            String tier = com.brownieshop.service.OrderService.getMembershipTier(cnt);
            tierMap.put(c.getId(), tier);
            tierLabelMap.put(c.getId(), com.brownieshop.service.OrderService.getTierLabel(tier));
        }
        model.addAttribute("tierMap",      tierMap);
        model.addAttribute("tierLabelMap", tierLabelMap);

        addNotifData(model);
        return "admin/customers";
    }

    @PostMapping("/customers/{id}/block")
    public String blockCustomer(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        customerService.blockCustomer(id);
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/unblock")
    public String unblockCustomer(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        customerService.unblockCustomer(id);
        return "redirect:/admin/customers";
    }
}