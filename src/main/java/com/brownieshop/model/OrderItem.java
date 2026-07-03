package com.brownieshop.model;

import java.math.BigDecimal;

/**
 * OrderItem entity — no Lombok to avoid any potential conflicts.
 */
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String customization;

    public OrderItem() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()               { return id; }
    public Long getOrderId()          { return orderId; }
    public Long getProductId()        { return productId; }
    public String getProductName()    { return productName; }
    public String getProductImage()   { return productImage; }
    public BigDecimal getUnitPrice()  { return unitPrice; }
    public Integer getQuantity()      { return quantity; }
    public String getCustomization()  { return customization; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                        { this.id = id; }
    public void setOrderId(Long orderId)              { this.orderId = orderId; }
    public void setProductId(Long productId)          { this.productId = productId; }
    public void setProductName(String productName)    { this.productName = productName; }
    public void setProductImage(String productImage)  { this.productImage = productImage; }
    public void setUnitPrice(BigDecimal unitPrice)    { this.unitPrice = unitPrice; }
    public void setQuantity(Integer quantity)         { this.quantity = quantity; }
    public void setCustomization(String customization){ this.customization = customization; }

    // ── Helpers ──────────────────────────────────────────────
    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}