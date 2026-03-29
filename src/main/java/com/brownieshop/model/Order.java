package com.brownieshop.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;
    private Long customerId;

    // PENDING | CONFIRMED | IN_PREPARATION | READY | DELIVERED | CANCELLED
    private String status;

    // DELIVERY | PICKUP
    private String deliveryType;

    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPostalCode;

    private BigDecimal totalAmount;

    private String customerNote;   // customer special requests
    private String adminNote;      // internal admin note

    private LocalDateTime requestedTime;  // when customer wants delivery
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // joined fields (not in DB columns – populated by JOIN)
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;

    // order items (loaded separately)
    private List<OrderItem> items;

    // ── Helpers ──────────────────────────────────────────────
    public String getCustomerFullName() {
        return (customerFirstName != null ? customerFirstName : "")
                + " " + (customerLastName != null ? customerLastName : "");
    }

    public boolean isCancellable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        return switch (status) {
            case "PENDING"        -> "⏳ Pending";
            case "CONFIRMED"      -> "✅ Confirmed";
            case "IN_PREPARATION" -> "👩‍🍳 In Preparation";
            case "READY"          -> "📦 Ready";
            case "DELIVERED"      -> "🚚 Delivered";
            case "CANCELLED"      -> "❌ Cancelled";
            default               -> status;
        };
    }

    public String getStatusColor() {
        if (status == null) return "grey";
        return switch (status) {
            case "PENDING"        -> "orange";
            case "CONFIRMED"      -> "blue";
            case "IN_PREPARATION" -> "purple";
            case "READY"          -> "teal";
            case "DELIVERED"      -> "green";
            case "CANCELLED"      -> "red";
            default               -> "grey";
        };
    }
}