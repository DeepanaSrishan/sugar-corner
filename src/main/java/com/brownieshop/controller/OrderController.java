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

import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private ProductService productService;

    private Customer getLoggedIn(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customerUser");
        if (c != null) return c;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && !fallback.isAdmin()) return fallback;
        return null;
    }

    // ── My Orders ────────────────────────────────────────────
    @GetMapping
    public String myOrders(HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";
        model.addAttribute("orders", orderService.getOrdersByCustomer(c.getId()));
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

    // ── Checkout page ─────────────────────────────────────────
    @GetMapping("/checkout/{productId}")
    public String checkoutPage(@PathVariable Long productId,
                               @RequestParam(defaultValue = "1") int qty,
                               HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Product> opt = productService.getById(productId);
        if (opt.isEmpty()) return "redirect:/products";

        Product p = opt.get();
        // Read live stock from DB — never use cached value
        int liveStock = productService.getStock(p.getId());

        // Block checkout if product is out of stock
        if (liveStock <= 0) return "redirect:/products?error=outofstock";

        // Cap the requested qty to what is actually available
        int safeQty = Math.min(Math.max(1, qty), liveStock);

        BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(safeQty));
        BigDecimal discountPercent = orderService.getDiscountPercentByCustomer(c.getId());
        BigDecimal discountAmount = orderService.calculateDiscountAmount(subtotal, discountPercent);
        BigDecimal finalTotal = orderService.calculateFinalTotal(subtotal, discountPercent);
        OrderService.LoyaltyProgress loyalty = orderService.getLoyaltyProgress(c.getId());

        model.addAttribute("product",  p);
        model.addAttribute("qty",      safeQty);
        model.addAttribute("maxStock", liveStock);   // always live from DB
        model.addAttribute("customer", c);
        model.addAttribute("loyaltyTierLabel", loyalty.getTierLabel());
        model.addAttribute("loyaltyDiscountPercent", discountPercent);
        model.addAttribute("previewSubtotal", subtotal);
        model.addAttribute("previewDiscountAmount", discountAmount);
        model.addAttribute("previewFinalTotal", finalTotal);
        return "orders/checkout";
    }

    // ── Place Order ──────────────────────────────────────────
    @PostMapping("/place")
    public String placeOrder(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false, defaultValue = "") String customization,
            @RequestParam(required = false, defaultValue = "DELIVERY") String deliveryType,
            @RequestParam(required = false, defaultValue = "") String deliveryAddress,
            @RequestParam(required = false, defaultValue = "") String deliveryCity,
            @RequestParam(required = false, defaultValue = "") String deliveryPostalCode,
            @RequestParam(required = false, defaultValue = "") String customerNote,
            HttpSession session) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        System.out.println("[ORDER] Placing order — productId=" + productId
                + " qty=" + quantity + " type=" + deliveryType + " city=" + deliveryCity);

        Order order = new Order();
        order.setCustomerId(c.getId());
        order.setDeliveryType(deliveryType.isBlank() ? "DELIVERY" : deliveryType);
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryCity(deliveryCity);
        order.setDeliveryPostalCode(deliveryPostalCode);
        order.setCustomerNote(customerNote);

        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(Math.max(1, quantity));
        item.setCustomization(customization);

        long orderId = orderService.placeOrder(order, List.of(item));

        System.out.println("[ORDER] Result orderId=" + orderId);

        if (orderId == -2L) return "redirect:/products?error=outofstock";
        if (orderId == -1L) return "redirect:/products?error=order";
        return "redirect:/orders/" + orderId + "?success=placed";
    }

    // ── Cancel ───────────────────────────────────────────────
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