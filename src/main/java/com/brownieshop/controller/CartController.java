package com.brownieshop.controller;

import com.brownieshop.model.CartItem;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired private ProductService productService;
    @Autowired private OrderService   orderService;

    // ── Helper: get or create cart in session ─────────────────
    @SuppressWarnings("unchecked")
    private List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private Customer getLoggedIn(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customerUser");
        if (c != null) return c;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && !fallback.isAdmin()) return fallback;
        return null;
    }

    // ── View Cart ─────────────────────────────────────────────
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        List<CartItem> cart = getCart(session);
        BigDecimal total = cart.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountPercent = orderService.getDiscountPercentByCustomer(c.getId());
        BigDecimal discountAmount = orderService.calculateDiscountAmount(total, discountPercent);
        BigDecimal finalTotal = orderService.calculateFinalTotal(total, discountPercent);
        OrderService.LoyaltyProgress loyalty = orderService.getLoyaltyProgress(c.getId());

        // Build productId -> liveStock map so cart.html can cap each qty control
        Map<Long, Integer> stockMap = new HashMap<>();
        for (CartItem item : cart) {
            int live = productService.getStock(item.getProductId());
            stockMap.put(item.getProductId(), live);
        }

        model.addAttribute("cart",     cart);
        model.addAttribute("total",    total);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("loyaltyTierLabel", loyalty.getTierLabel());
        model.addAttribute("stockMap", stockMap);
        model.addAttribute("customer", c);
        return "orders/cart";
    }

    // ── Add item to cart ──────────────────────────────────────
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(required = false, defaultValue = "") String customization,
                            HttpSession session) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Product> opt = productService.getById(productId);
        if (opt.isEmpty() || !opt.get().getInStock()) return "redirect:/products";

        // Always read live stock from DB — never trust cached product object
        int liveStock = productService.getStock(productId);
        if (liveStock <= 0) return "redirect:/products?error=outofstock";

        List<CartItem> cart = getCart(session);
        Product p = opt.get();

        // If same product already in cart, increase quantity but cap at stock
        for (CartItem item : cart) {
            if (item.getProductId().equals(productId)) {
                int newQty = Math.min(item.getQuantity() + Math.max(1, quantity), liveStock);
                item.setQuantity(newQty);
                return "redirect:/cart";
            }
        }

        // New cart item — cap at live stock
        int safeQty = Math.min(Math.max(1, quantity), liveStock);
        cart.add(new CartItem(p, safeQty, customization));
        return "redirect:/cart";
    }

    // ── Update quantity ───────────────────────────────────────
    @PostMapping("/update")
    public String updateCart(@RequestParam Long productId,
                             @RequestParam int  quantity,
                             HttpSession session) {
        List<CartItem> cart = getCart(session);
        if (quantity <= 0) {
            cart.removeIf(i -> i.getProductId().equals(productId));
        } else {
            // Cap at live stock — prevents form tampering
            int liveStock = productService.getStock(productId);
            int safeQty   = Math.min(quantity, liveStock);
            for (CartItem item : cart) {
                if (item.getProductId().equals(productId)) {
                    item.setQuantity(safeQty > 0 ? safeQty : 1);
                    break;
                }
            }
        }
        return "redirect:/cart";
    }

    // ── Remove one item ───────────────────────────────────────
    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session) {
        getCart(session).removeIf(i -> i.getProductId().equals(productId));
        return "redirect:/cart";
    }

    // ── Clear entire cart ─────────────────────────────────────
    @PostMapping("/clear")
    public String clearCart(HttpSession session) {
        session.removeAttribute("cart");
        return "redirect:/cart";
    }

    // ── Cart Checkout page ────────────────────────────────────
    @GetMapping("/checkout")
    public String cartCheckout(HttpSession session, Model model) {
        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        List<CartItem> cart = getCart(session);
        if (cart.isEmpty()) return "redirect:/cart";

        BigDecimal total = cart.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountPercent = orderService.getDiscountPercentByCustomer(c.getId());
        BigDecimal discountAmount = orderService.calculateDiscountAmount(total, discountPercent);
        BigDecimal finalTotal = orderService.calculateFinalTotal(total, discountPercent);
        OrderService.LoyaltyProgress loyalty = orderService.getLoyaltyProgress(c.getId());

        model.addAttribute("cart",     cart);
        model.addAttribute("total",    total);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("loyaltyTierLabel", loyalty.getTierLabel());
        model.addAttribute("customer", c);
        return "orders/cart-checkout";
    }

    // ── Place order from cart ─────────────────────────────────
    @PostMapping("/place")
    public String placeCartOrder(
            @RequestParam(required = false, defaultValue = "DELIVERY") String deliveryType,
            @RequestParam(required = false, defaultValue = "") String deliveryAddress,
            @RequestParam(required = false, defaultValue = "") String deliveryCity,
            @RequestParam(required = false, defaultValue = "") String deliveryPostalCode,
            @RequestParam(required = false, defaultValue = "") String customerNote,
            HttpSession session) {

        Customer c = getLoggedIn(session);
        if (c == null) return "redirect:/login";

        List<CartItem> cart = getCart(session);
        if (cart.isEmpty()) return "redirect:/cart";

        Order order = new Order();
        order.setCustomerId(c.getId());
        order.setDeliveryType(deliveryType.isBlank() ? "DELIVERY" : deliveryType);
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryCity(deliveryCity);
        order.setDeliveryPostalCode(deliveryPostalCode);
        order.setCustomerNote(customerNote);

        List<OrderItem> items = new ArrayList<>();
        for (CartItem ci : cart) {
            OrderItem oi = new OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setQuantity(ci.getQuantity());
            oi.setCustomization(ci.getCustomization());
            items.add(oi);
        }

        long orderId = orderService.placeOrder(order, items);
        if (orderId == -1L) return "redirect:/cart?error=order";

        // Clear cart after successful order
        session.removeAttribute("cart");

        return "redirect:/payments/pay/" + orderId;
    }
}