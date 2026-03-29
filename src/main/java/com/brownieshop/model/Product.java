package com.brownieshop.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product entity – maps to the `products` table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;

    private String name;
    private String description;
    private String category;          // e.g. Classic, Fudge, Nutty, Vegan, Special

    private BigDecimal price;
    private Integer stockQuantity;    // 0 = out of stock

    private String imagePath;         // relative path inside /uploads/products/
    private boolean featured;         // show on featured / popular section
    private boolean available;        // admin can mark as unavailable without deleting

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Convenience helpers ──────────────────────────────────
    public boolean isInStock() {
        return available && stockQuantity != null && stockQuantity > 0;
    }

    public String getStockLabel() {
        if (!available)                        return "Unavailable";
        if (stockQuantity == null || stockQuantity <= 0) return "Out of Stock";
        if (stockQuantity <= 5)                return "Low Stock";
        return "In Stock";
    }
}