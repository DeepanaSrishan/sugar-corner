package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.service.CustomerService;
import com.brownieshop.service.OrderService;
import com.brownieshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private CustomerService customerService;
    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;

    private boolean isAdmin(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customer");
        return c != null && c.isAdmin();
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Customer admin = (Customer) session.getAttribute("customer");
        model.addAttribute("admin", admin);
        List<Customer> customers = customerService.getAllCustomers();
        model.addAttribute("totalCustomers", customers.size());
        model.addAttribute("activeCustomers",
                customers.stream().filter(Customer::isActive).count());
        model.addAttribute("blockedCustomers",
                customers.stream().filter(c -> "BLOCKED".equalsIgnoreCase(c.getStatus())).count());
        model.addAttribute("totalProducts", productService.getAllProducts().size());
        model.addAttribute("totalOrders",   orderService.getAllOrders().size());
        model.addAttribute("pendingOrders", orderService.getPendingOrderCount());
        model.addAttribute("newToday",      orderService.getNewOrdersTodayCount());
        model.addAttribute("notifCount",    orderService.getPendingOrderCount());
        return "admin/dashboard";
    }

    @GetMapping("/customers")
    public String listCustomers(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("notifCount", orderService.getPendingOrderCount());
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