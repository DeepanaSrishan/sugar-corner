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

    // ── Place Order ──────────────────────────────────────────
    /**
     * cartItems: list of maps with productId and quantity (and optional customization).
     * We snapshot product name+price at order time so history stays accurate even if
     * the product is later edited.
     */
    public long placeOrder(Order order, List<OrderItem> cartItems) {
        // Calculate total from current product prices
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> resolvedItems = new ArrayList<>();

        for (OrderItem item : cartItems) {
            Optional<Product> optP = productDAO.findById(item.getProductId());
            if (optP.isEmpty() || !optP.get().isAvailable()) continue;

            Product p = optP.get();
            item.setProductName(p.getName());
            item.setProductImage(p.getImagePath());
            item.setUnitPrice(p.getPrice());
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            resolvedItems.add(item);
        }

        if (resolvedItems.isEmpty()) return -1L;

        order.setTotalAmount(total);
        order.setStatus("PENDING");

        long orderId = orderDAO.saveOrder(order);
        for (OrderItem item : resolvedItems) {
            item.setOrderId(orderId);
            orderDAO.saveOrderItem(item);
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
        // security: customer can only cancel their own order
        if (!o.getCustomerId().equals(customerId)) return false;
        if (!o.isCancellable()) return false;
        orderDAO.cancelOrder(orderId);
        return true;
    }

    // ── Admin actions ─────────────────────────────────────────
    public void updateStatus(Long orderId, String status) {
        orderDAO.updateStatus(orderId, status);
    }

    public void saveAdminNote(Long orderId, String note) {
        orderDAO.updateAdminNote(orderId, note);
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
        newOrder.setDeliveryType(deliveryType);
        newOrder.setDeliveryAddress(address);
        newOrder.setDeliveryCity(city);
        newOrder.setDeliveryPostalCode(postalCode);

        List<OrderItem> items = new ArrayList<>();
        for (OrderItem item : orig.getItems()) {
            OrderItem ni = new OrderItem();
            ni.setProductId(item.getProductId());
            ni.setQuantity(item.getQuantity());
            ni.setCustomization(item.getCustomization());
            items.add(ni);
        }
        return placeOrder(newOrder, items);
    }
}