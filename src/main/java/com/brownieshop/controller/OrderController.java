package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Order;
import com.brownieshop.model.OrderItem;
import com.brownieshop.model.Product;
import com.brownieshop.service.OrderService;
import com.brownieshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private ProductService productService;

    private Customer getLoggedIn(HttpSession session) {
        return (Customer) session.getAttribute("customer");
    }

    // ── My Orders list ───────────────────────────────────────
    @GetMapping
    public String myOrders(HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        List<Order> orders = orderService.getOrdersByCustomer(c.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("customer", c);
        return "orders/my-orders";
    }

    // ── Order detail ─────────────────────────────────────────
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Order> opt = orderService.getById(id);
        if (opt.isEmpty() || !opt.get().getCustomerId().equals(c.getId()))
            return "redirect:/orders";

        model.addAttribute("order", opt.get());
        model.addAttribute("customer", c);
        return "orders/order-detail";
    }

    // ── Checkout page (GET) ──────────────────────────────────
    @GetMapping("/checkout/{productId}")
    public String checkoutPage(@PathVariable Long productId,
                               @RequestParam(defaultValue = "1") int qty,
                               HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Product> opt = productService.getById(productId);
        if (opt.isEmpty() || !opt.get().isAvailable()) return "redirect:/products";

        model.addAttribute("product", opt.get());
        model.addAttribute("qty", qty);
        model.addAttribute("customer", c);
        return "orders/checkout";
    }

    // ── Place Order (POST) ───────────────────────────────────
    @PostMapping("/place")
    public String placeOrder(
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestParam(required = false, defaultValue = "") String customization,
            @RequestParam String deliveryType,
            @RequestParam(required = false, defaultValue = "") String deliveryAddress,
            @RequestParam(required = false, defaultValue = "") String deliveryCity,
            @RequestParam(required = false, defaultValue = "") String deliveryPostalCode,
            @RequestParam(required = false, defaultValue = "") String customerNote,
            HttpSession session, Model model) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Order order = new Order();
        order.setCustomerId(c.getId());
        order.setDeliveryType(deliveryType);
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryCity(deliveryCity);
        order.setDeliveryPostalCode(deliveryPostalCode);
        order.setCustomerNote(customerNote);

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(Math.max(1, quantity));
        item.setCustomization(customization);

        long orderId = orderService.placeOrder(order, List.of(item));
        if (orderId == -1L) return "redirect:/products?error=order";

        return "redirect:/orders/" + orderId + "?success=placed";
    }

    // ── Cancel Order ─────────────────────────────────────────
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, HttpSession session) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";
        orderService.cancelOrder(id, c.getId());
        return "redirect:/orders/" + id + "?cancelled=true";
    }

    // ── Reorder ──────────────────────────────────────────────
    @PostMapping("/{id}/reorder")
    public String reorder(@PathVariable Long id, HttpSession session) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        // Use same address from original order
        Optional<Order> orig = orderService.getById(id);
        if (orig.isEmpty() || !orig.get().getCustomerId().equals(c.getId()))
            return "redirect:/orders";

        Order o = orig.get();
        long newId = orderService.reorder(id, c.getId(),
                o.getDeliveryType(), o.getDeliveryAddress(),
                o.getDeliveryCity(), o.getDeliveryPostalCode());

        if (newId == -1L) return "redirect:/orders?error=reorder";
        return "redirect:/orders/" + newId + "?success=reordered";
    }
}