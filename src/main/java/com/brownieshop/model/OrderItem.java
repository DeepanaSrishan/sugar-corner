package com.brownieshop.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;

    private String productName;    // snapshot at time of order
    private String productImage;   // snapshot
    private BigDecimal unitPrice;  // snapshot at time of order
    private Integer quantity;
    private String customization;  // e.g. "No nuts", "Extra frosting"

    // ── Helpers ──────────────────────────────────────────────
    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}