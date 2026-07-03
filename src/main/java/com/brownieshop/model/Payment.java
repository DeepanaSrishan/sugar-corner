package com.brownieshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity — linked to orders.
 */
public class Payment {

    private Long id;
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;   // CASH_ON_DELIVERY, CARD, BANK_TRANSFER
    private String status;          // PENDING, COMPLETED, FAILED, REFUNDED
    private String transactionRef;  // e.g. card last4 or bank ref
    private String notes;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined fields
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;

    public Payment() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()                  { return id; }
    public Long getOrderId()             { return orderId; }
    public Long getCustomerId()          { return customerId; }
    public BigDecimal getAmount()        { return amount; }
    public String getPaymentMethod()     { return paymentMethod; }
    public String getStatus()            { return status; }
    public String getTransactionRef()    { return transactionRef; }
    public String getNotes()             { return notes; }
    public LocalDateTime getPaidAt()     { return paidAt; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }
    public String getCustomerFirstName() { return customerFirstName; }
    public String getCustomerLastName()  { return customerLastName; }
    public String getCustomerEmail()     { return customerEmail; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                          { this.id = id; }
    public void setOrderId(Long orderId)                { this.orderId = orderId; }
    public void setCustomerId(Long customerId)          { this.customerId = customerId; }
    public void setAmount(BigDecimal amount)            { this.amount = amount; }
    public void setPaymentMethod(String paymentMethod)  { this.paymentMethod = paymentMethod; }
    public void setStatus(String status)                { this.status = status; }
    public void setTransactionRef(String ref)           { this.transactionRef = ref; }
    public void setNotes(String notes)                  { this.notes = notes; }
    public void setPaidAt(LocalDateTime paidAt)         { this.paidAt = paidAt; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }
    public void setCustomerFirstName(String s)          { this.customerFirstName = s; }
    public void setCustomerLastName(String s)           { this.customerLastName = s; }
    public void setCustomerEmail(String s)              { this.customerEmail = s; }

    // ── Business helpers ─────────────────────────────────────
    public String getCustomerFullName() {
        String f = customerFirstName != null ? customerFirstName : "";
        String l = customerLastName  != null ? customerLastName  : "";
        return (f + " " + l).trim();
    }

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        switch (status) {
            case "PENDING":   return "⏳ Pending";
            case "COMPLETED": return "✅ Completed";
            case "FAILED":    return "❌ Failed";
            case "REFUNDED":  return "↩️ Refunded";
            default:          return status;
        }
    }

    public String getStatusColor() {
        if (status == null) return "grey";
        switch (status) {
            case "PENDING":   return "orange";
            case "COMPLETED": return "green";
            case "FAILED":    return "red";
            case "REFUNDED":  return "blue";
            default:          return "grey";
        }
    }

    public String getMethodLabel() {
        if (paymentMethod == null) return "—";
        switch (paymentMethod) {
            case "CASH_ON_DELIVERY": return "💵 Cash on Delivery";
            case "CARD":             return "💳 Card";
            case "BANK_TRANSFER":    return "🏦 Bank Transfer";
            default:                 return paymentMethod;
        }
    }
}