package com.brownieshop.model;

import java.time.LocalDateTime;

/**
 * Inquiry — a message sent from customer to admin.
 * Status flow: NEW → IN_PROGRESS → RESOLVED
 */
public class Inquiry {

    private Long id;
    private Long customerId;

    private String subject;
    private String message;

    // NEW | IN_PROGRESS | RESOLVED
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined from customers table
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;

    // Admin's reply (latest reply text shown inline)
    private String adminReply;
    private LocalDateTime repliedAt;

    public Inquiry() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()                     { return id; }
    public Long getCustomerId()             { return customerId; }
    public String getSubject()              { return subject; }
    public String getMessage()              { return message; }
    public String getStatus()               { return status; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getUpdatedAt()     { return updatedAt; }
    public String getCustomerFirstName()    { return customerFirstName; }
    public String getCustomerLastName()     { return customerLastName; }
    public String getCustomerEmail()        { return customerEmail; }
    public String getCustomerPhone()        { return customerPhone; }
    public String getAdminReply()           { return adminReply; }
    public LocalDateTime getRepliedAt()     { return repliedAt; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                             { this.id = id; }
    public void setCustomerId(Long customerId)             { this.customerId = customerId; }
    public void setSubject(String subject)                 { this.subject = subject; }
    public void setMessage(String message)                 { this.message = message; }
    public void setStatus(String status)                   { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt = updatedAt; }
    public void setCustomerFirstName(String s)             { this.customerFirstName = s; }
    public void setCustomerLastName(String s)              { this.customerLastName = s; }
    public void setCustomerEmail(String s)                 { this.customerEmail = s; }
    public void setCustomerPhone(String s)                 { this.customerPhone = s; }
    public void setAdminReply(String adminReply)           { this.adminReply = adminReply; }
    public void setRepliedAt(LocalDateTime repliedAt)      { this.repliedAt = repliedAt; }

    // ── Helpers ──────────────────────────────────────────────
    public String getCustomerFullName() {
        String f = customerFirstName != null ? customerFirstName : "";
        String l = customerLastName  != null ? customerLastName  : "";
        return (f + " " + l).trim();
    }

    public String getStatusLabel() {
        if (status == null) return "New";
        switch (status) {
            case "NEW":         return "🆕 New";
            case "IN_PROGRESS": return "💬 In Progress";
            case "RESOLVED":    return "✅ Resolved";
            default:            return status;
        }
    }

    public String getStatusColor() {
        if (status == null) return "orange";
        switch (status) {
            case "NEW":         return "orange";
            case "IN_PROGRESS": return "blue";
            case "RESOLVED":    return "green";
            default:            return "grey";
        }
    }
}