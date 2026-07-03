package com.brownieshop.service;

import com.brownieshop.dao.OrderDAO;
import com.brownieshop.dao.ProductDAO;
import com.brownieshop.model.Order;
import com.brownieshop.model.OrderItem;
import com.brownieshop.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired private OrderDAO orderDAO;
    @Autowired private ProductDAO productDAO;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // ── Place Order ──────────────────────────────────────────
    public long placeOrder(Order order, List<OrderItem> cartItems) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> resolvedItems = new ArrayList<>();

        for (OrderItem item : cartItems) {
            if (item.getProductId() == null) {
                System.out.println("[ORDER] Skipping item - productId is null");
                continue;
            }

            Optional<Product> optP = productDAO.findById(item.getProductId());
            if (optP.isEmpty()) {
                System.out.println("[ORDER] Skipping item - product not found: " + item.getProductId());
                continue;
            }

            Product p = optP.get();
            Integer rawQty = item.getQuantity();
            int qty = (rawQty == null || rawQty < 1) ? 1 : rawQty;

            // ── STOCK VALIDATION ─────────────────────────────
            // Check current stock in DB (re-read to be accurate)
            int currentStock = productDAO.getStock(item.getProductId());
            if (currentStock <= 0) {
                System.out.println("[ORDER] Rejected - out of stock: " + p.getName());
                return -2L; // special code: out of stock
            }
            if (qty > currentStock) {
                System.out.println("[ORDER] Capping qty from " + qty + " to " + currentStock + " for: " + p.getName());
                qty = currentStock; // cap at available stock
            }

            item.setProductName(p.getName());
            item.setProductImage(p.getImagePath());
            item.setUnitPrice(p.getPrice());
            item.setQuantity(qty);

            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
            resolvedItems.add(item);
            System.out.println("[ORDER] Added item: " + p.getName() + " x" + qty);
        }

        if (resolvedItems.isEmpty()) {
            System.out.println("[ORDER] No valid items - order rejected");
            return -1L;
        }

        if (order.getDeliveryType() == null || order.getDeliveryType().isBlank()) {
            order.setDeliveryType("DELIVERY");
        }

        int completedOrders = orderDAO.countCompletedByCustomerId(order.getCustomerId());
        String tier = getMembershipTier(completedOrders);
        BigDecimal discountPercent = getDiscountPercentForTier(tier);
        BigDecimal discountAmount = calculateDiscountAmount(total, discountPercent);
        BigDecimal finalTotal = total.subtract(discountAmount).max(BigDecimal.ZERO);

        order.setSubtotalBeforeDiscount(total);
        order.setDiscountPercent(discountPercent);
        order.setDiscountAmount(discountAmount);
        order.setAppliedTier(tier);
        order.setTotalAmount(finalTotal);
        order.setStatus("PENDING");

        long orderId = orderDAO.saveOrder(order);
        System.out.println("[ORDER] Order saved with ID: " + orderId);

        for (OrderItem item : resolvedItems) {
            item.setOrderId(orderId);
            orderDAO.saveOrderItem(item);
            // ── DECREASE STOCK ────────────────────────────────
            productDAO.decreaseStock(item.getProductId(), item.getQuantity());
            System.out.println("[ORDER] Stock decreased for product " + item.getProductId() + " by " + item.getQuantity());
        }

        return orderId;
    }

    // ── Read ─────────────────────────────────────────────────
    public Optional<Order> getById(Long id) {
        Optional<Order> opt = orderDAO.findById(id);
        opt.ifPresent(o -> o.setItems(orderDAO.findItemsByOrderId(id)));
        return opt;
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        List<Order> orders = orderDAO.findByCustomerId(customerId);
        orders.forEach(o -> o.setItems(orderDAO.findItemsByOrderId(o.getId())));
        return orders;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = orderDAO.findAll();
        orders.forEach(o -> o.setItems(orderDAO.findItemsByOrderId(o.getId())));
        return orders;
    }

    public List<Order> searchOrders(String orderId, String date) {
        if (orderId != null && !orderId.isBlank()) {
            try {
                List<Order> result = orderDAO.findByOrderId(Long.parseLong(orderId.trim()));
                result.forEach(o -> o.setItems(orderDAO.findItemsByOrderId(o.getId())));
                return result;
            } catch (NumberFormatException e) {
                return List.of();
            }
        }
        if (date != null && !date.isBlank()) {
            List<Order> result = orderDAO.findByDate(date);
            result.forEach(o -> o.setItems(orderDAO.findItemsByOrderId(o.getId())));
            return result;
        }
        return getAllOrders();
    }

    // ── Notification counts ─────────────────────────────────
    public int getPendingOrderCount() {
        return orderDAO.countPendingOrders();
    }

    public int getNewOrdersTodayCount() {
        return orderDAO.countNewOrdersToday();
    }

    // ── Customer Cancel ──────────────────────────────────────
    public boolean cancelOrder(Long orderId, Long customerId) {
        Optional<Order> opt = orderDAO.findById(orderId);
        if (opt.isEmpty()) return false;
        Order o = opt.get();
        if (!o.getCustomerId().equals(customerId)) return false;
        if (!o.isCancellable()) return false;
        orderDAO.cancelOrder(orderId);
        return true;
    }

    // ── Admin actions ─────────────────────────────────────────
    public void updateStatus(Long orderId, String status) {
        orderDAO.updateStatus(orderId, normalizeStatus(status));
    }

    public void saveAdminNote(Long orderId, String note) {
        orderDAO.updateAdminNote(orderId, note);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        return status.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }

    // ── Reorder ──────────────────────────────────────────────
    public long reorder(Long originalOrderId, Long customerId,
                        String deliveryType, String address,
                        String city, String postalCode) {
        Optional<Order> opt = getById(originalOrderId);
        if (opt.isEmpty()) return -1L;
        Order orig = opt.get();
        if (!orig.getCustomerId().equals(customerId)) return -1L;

        Order newOrder = new Order();
        newOrder.setCustomerId(customerId);
        newOrder.setDeliveryType(deliveryType != null ? deliveryType : "DELIVERY");
        newOrder.setDeliveryAddress(address);
        newOrder.setDeliveryCity(city);
        newOrder.setDeliveryPostalCode(postalCode);

        List<OrderItem> items = new ArrayList<>();
        for (OrderItem item : orig.getItems()) {
            OrderItem ni = new OrderItem();
            ni.setProductId(item.getProductId());
            Integer qty = item.getQuantity();
            ni.setQuantity(qty != null ? qty : 1);
            ni.setCustomization(item.getCustomization());
            items.add(ni);
        }
        return placeOrder(newOrder, items);
    }

    // ── LOYALTY / MEMBERSHIP ─────────────────────────────────

    /**
     * Returns completed order count for one customer.
     * Used on customer detail / dashboard pages.
     */
    public int getCompletedOrderCount(Long customerId) {
        return orderDAO.countCompletedByCustomerId(customerId);
    }

    /**
     * Returns completed order counts for ALL customers as a map.
     * Used by AdminController when loading the full customer list —
     * one SQL query instead of N queries.
     */
    public java.util.Map<Long, Integer> getAllCustomerOrderCounts() {
        return orderDAO.countCompletedOrdersAllCustomers();
    }

    /**
     * Calculates the membership tier string based on completed order count.
     *
     * Tiers (matching the image):
     *   0-2     → NEW
     *   3-5     → REGULAR
     *   6-10    → LOYAL
     *   11+     → VIP
     */
    public static String getMembershipTier(int completedOrders) {
        if (completedOrders <= 2)       return "NEW";
        if (completedOrders <= 5)       return "REGULAR";
        if (completedOrders <= 10)      return "LOYAL";
        return "VIP";
    }

    /** Human-readable label for a tier */
    public static String getTierLabel(String tier) {
        switch (tier) {
            case "NEW":      return "🌱 New Customer";
            case "REGULAR":  return "⭐ Regular Customer";
            case "LOYAL":    return "💎 Loyalty Customer";
            case "VIP":      return "👑 VIP Customer";
            default:           return "🌱 New Customer";
        }
    }

    public static BigDecimal getDiscountPercentForTier(String tier) {
        switch (tier) {
            case "REGULAR": return new BigDecimal("5");
            case "LOYAL":   return new BigDecimal("8");
            case "VIP":     return new BigDecimal("10");
            default:          return BigDecimal.ZERO;
        }
    }

    public BigDecimal getDiscountPercentByCustomer(Long customerId) {
        int completed = getCompletedOrderCount(customerId);
        return getDiscountPercentForTier(getMembershipTier(completed));
    }

    public BigDecimal calculateDiscountAmount(BigDecimal subtotal, BigDecimal discountPercent) {
        if (subtotal == null || discountPercent == null || subtotal.signum() <= 0 || discountPercent.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(discountPercent)
                .divide(HUNDRED, 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFinalTotal(BigDecimal subtotal, BigDecimal discountPercent) {
        if (subtotal == null) return BigDecimal.ZERO;
        return subtotal.subtract(calculateDiscountAmount(subtotal, discountPercent)).max(BigDecimal.ZERO);
    }

    public LoyaltyProgress getLoyaltyProgress(Long customerId) {
        int completed = getCompletedOrderCount(customerId);
        String tier = getMembershipTier(completed);
        int ordersToNextTier;
        int progressPercent;
        String nextTier;

        if (completed <= 2) {
            ordersToNextTier = 3 - completed;
            progressPercent = (int) Math.min(100, Math.round((completed / 3.0) * 100));
            nextTier = "REGULAR";
        } else if (completed <= 5) {
            ordersToNextTier = 6 - completed;
            progressPercent = (int) Math.min(100, Math.round(((completed - 3) / 3.0) * 100));
            nextTier = "LOYAL";
        } else if (completed <= 10) {
            ordersToNextTier = 11 - completed;
            progressPercent = (int) Math.min(100, Math.round(((completed - 6) / 5.0) * 100));
            nextTier = "VIP";
        } else {
            ordersToNextTier = 0;
            progressPercent = 100;
            nextTier = "VIP";
        }

        return new LoyaltyProgress(
                completed,
                tier,
                getTierLabel(tier),
                getDiscountPercentForTier(tier),
                Math.max(0, ordersToNextTier),
                nextTier,
                getTierLabel(nextTier),
                Math.max(0, progressPercent)
        );
    }

    public static class LoyaltyProgress {
        private final int completedOrders;
        private final String tier;
        private final String tierLabel;
        private final BigDecimal discountPercent;
        private final int ordersToNextTier;
        private final String nextTier;
        private final String nextTierLabel;
        private final int progressPercent;

        public LoyaltyProgress(int completedOrders, String tier, String tierLabel,
                               BigDecimal discountPercent, int ordersToNextTier,
                               String nextTier, String nextTierLabel, int progressPercent) {
            this.completedOrders = completedOrders;
            this.tier = tier;
            this.tierLabel = tierLabel;
            this.discountPercent = discountPercent;
            this.ordersToNextTier = ordersToNextTier;
            this.nextTier = nextTier;
            this.nextTierLabel = nextTierLabel;
            this.progressPercent = progressPercent;
        }

        public int getCompletedOrders() { return completedOrders; }
        public String getTier() { return tier; }
        public String getTierLabel() { return tierLabel; }
        public BigDecimal getDiscountPercent() { return discountPercent; }
        public int getOrdersToNextTier() { return ordersToNextTier; }
        public String getNextTier() { return nextTier; }
        public String getNextTierLabel() { return nextTierLabel; }
        public int getProgressPercent() { return progressPercent; }
    }

}