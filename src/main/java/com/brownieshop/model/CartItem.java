package com.brownieshop.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * One item stored in the session-based shopping cart.
 * Must be Serializable so it can live in the HTTP session.
 */
public class CartItem implements Serializable {

    private Long   productId;
    private String productName;
    private String productImage;
    private String category;
    private BigDecimal unitPrice;
    private int    quantity;
    private String customization;

    public CartItem() {}

    public CartItem(Product p, int quantity, String customization) {
        this.productId     = p.getId();
        this.productName   = p.getName();
        this.productImage  = p.getImagePath();
        this.category      = p.getCategory();
        this.unitPrice     = p.getPrice();
        this.quantity      = Math.max(1, quantity);
        this.customization = customization;
    }

    // ── Getters ──────────────────────────────────────────────
    public Long       getProductId()     { return productId; }
    public String     getProductName()   { return productName; }
    public String     getProductImage()  { return productImage; }
    public String     getCategory()      { return category; }
    public BigDecimal getUnitPrice()     { return unitPrice; }
    public int        getQuantity()      { return quantity; }
    public String     getCustomization() { return customization; }

    // ── Setters ──────────────────────────────────────────────
    public void setProductId(Long productId)         { this.productId = productId; }
    public void setProductName(String productName)   { this.productName = productName; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public void setCategory(String category)         { this.category = category; }
    public void setUnitPrice(BigDecimal unitPrice)   { this.unitPrice = unitPrice; }
    public void setQuantity(int quantity)            { this.quantity = Math.max(1, quantity); }
    public void setCustomization(String c)           { this.customization = c; }

    // ── Helper ───────────────────────────────────────────────
    public BigDecimal getSubtotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}