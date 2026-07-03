package com.brownieshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product entity.
 * NOTE: No Lombok @Data here — we write getters/setters manually
 * to avoid Lombok conflicts with boolean isAvailable() / isInStock().
 */
public class Product {

    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imagePath;
    private boolean featured;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────
    public Product() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public String getCategory()          { return category; }
    public BigDecimal getPrice()         { return price; }
    public Integer getStockQuantity()    { return stockQuantity; }
    public String getImagePath()         { return imagePath; }
    public boolean isFeatured()          { return featured; }
    public boolean isAvailable()         { return available; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                          { this.id = id; }
    public void setName(String name)                    { this.name = name; }
    public void setDescription(String description)      { this.description = description; }
    public void setCategory(String category)            { this.category = category; }
    public void setPrice(BigDecimal price)              { this.price = price; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setImagePath(String imagePath)          { this.imagePath = imagePath; }
    public void setFeatured(boolean featured)           { this.featured = featured; }
    public void setAvailable(boolean available)         { this.available = available; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    // ── Business logic ───────────────────────────────────────
    /**
     * TRUE if the product can be ordered right now.
     * Used by Thymeleaf: ${product.inStock}  → calls getInStock()
     */
    public boolean getInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    /**
     * Label for the stock badge on product cards.
     */
    public String getStockLabel() {
        if (stockQuantity == null || stockQuantity <= 0) return "Out of Stock";
        if (stockQuantity <= 5) return "Low Stock";
        return "In Stock";
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', available=" + available
                + ", stock=" + stockQuantity + "}";
    }
}