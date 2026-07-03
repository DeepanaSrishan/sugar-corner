package com.brownieshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order entity — no Lombok to avoid conflicts.
 */
public class Order {

    private Long id;
    private Long customerId;
    private String status;
    private String deliveryType;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPostalCode;
    private BigDecimal subtotalBeforeDiscount;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private String appliedTier;
    private BigDecimal totalAmount;
    private String customerNote;
    private String adminNote;
    private LocalDateTime requestedTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined from customers table
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;
    // Customer's registered address (for admin view)
    private String customerAddress;
    private String customerCity;
    private String customerPostalCode;

    // Loaded separately
    private List<OrderItem> items;

    public Order() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()                     { return id; }
    public Long getCustomerId()             { return customerId; }
    public String getStatus()               { return status; }
    public String getDeliveryType()         { return deliveryType; }
    public String getDeliveryAddress()      { return deliveryAddress; }
    public String getDeliveryCity()         { return deliveryCity; }
    public String getDeliveryPostalCode()   { return deliveryPostalCode; }
    public BigDecimal getSubtotalBeforeDiscount() { return subtotalBeforeDiscount; }
    public BigDecimal getDiscountPercent()   { return discountPercent; }
    public BigDecimal getDiscountAmount()    { return discountAmount; }
    public String getAppliedTier()           { return appliedTier; }
    public BigDecimal getTotalAmount()      { return totalAmount; }
    public String getCustomerNote()         { return customerNote; }
    public String getAdminNote()            { return adminNote; }
    public LocalDateTime getRequestedTime() { return requestedTime; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getUpdatedAt()     { return updatedAt; }
    public String getCustomerFirstName()    { return customerFirstName; }
    public String getCustomerLastName()     { return customerLastName; }
    public String getCustomerEmail()        { return customerEmail; }
    public String getCustomerPhone()        { return customerPhone; }
    public String getCustomerAddress()      { return customerAddress; }
    public String getCustomerCity()         { return customerCity; }
    public String getCustomerPostalCode()   { return customerPostalCode; }
    public List<OrderItem> getItems()       { return items; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                             { this.id = id; }
    public void setCustomerId(Long customerId)             { this.customerId = customerId; }
    public void setStatus(String status) {
        if (status == null) {
            this.status = null;
            return;
        }
        this.status = status.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }
    public void setDeliveryType(String deliveryType)       { this.deliveryType = deliveryType; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public void setDeliveryCity(String deliveryCity)       { this.deliveryCity = deliveryCity; }
    public void setDeliveryPostalCode(String p)            { this.deliveryPostalCode = p; }
    public void setSubtotalBeforeDiscount(BigDecimal subtotalBeforeDiscount) { this.subtotalBeforeDiscount = subtotalBeforeDiscount; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setAppliedTier(String appliedTier)         { this.appliedTier = appliedTier; }
    public void setTotalAmount(BigDecimal totalAmount)     { this.totalAmount = totalAmount; }
    public void setCustomerNote(String customerNote)       { this.customerNote = customerNote; }
    public void setAdminNote(String adminNote)             { this.adminNote = adminNote; }
    public void setRequestedTime(LocalDateTime t)          { this.requestedTime = t; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt = updatedAt; }
    public void setCustomerFirstName(String s)             { this.customerFirstName = s; }
    public void setCustomerLastName(String s)              { this.customerLastName = s; }
    public void setCustomerEmail(String s)                 { this.customerEmail = s; }
    public void setCustomerPhone(String s)                 { this.customerPhone = s; }
    public void setCustomerAddress(String s)               { this.customerAddress = s; }
    public void setCustomerCity(String s)                  { this.customerCity = s; }
    public void setCustomerPostalCode(String s)            { this.customerPostalCode = s; }
    public void setItems(List<OrderItem> items)            { this.items = items; }

    // ── Business helpers ─────────────────────────────────────
    public String getCustomerFullName() {
        String first = customerFirstName != null ? customerFirstName : "";
        String last  = customerLastName  != null ? customerLastName  : "";
        return (first + " " + last).trim();
    }

    public boolean isCancellable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        switch (status) {
            case "PENDING":        return "⏳ Pending";
            case "CONFIRMED":      return "✅ Confirmed";
            case "IN_PREPARATION": return "👩‍🍳 In Preparation";
            case "READY":          return "📦 Ready";
            case "DELIVERED":      return "🚚 Delivered";
            case "CANCELLED":      return "❌ Cancelled";
            default:               return status;
        }
    }

    public String getStatusColor() {
        if (status == null) return "grey";
        switch (status) {
            case "PENDING":        return "orange";
            case "CONFIRMED":      return "blue";
            case "IN_PREPARATION": return "purple";
            case "READY":          return "teal";
            case "DELIVERED":      return "green";
            case "CANCELLED":      return "red";
            default:               return "grey";
        }
    }

    public BigDecimal getSubtotalBeforeDiscountSafe() {
        return subtotalBeforeDiscount != null ? subtotalBeforeDiscount : BigDecimal.ZERO;
    }

    public BigDecimal getDiscountPercentSafe() {
        return discountPercent != null ? discountPercent : BigDecimal.ZERO;
    }

    public BigDecimal getDiscountAmountSafe() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }
}