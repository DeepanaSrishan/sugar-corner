package com.brownieshop.model;

import java.time.LocalDateTime;

/**
 * Feedback — customer review/feedback about products or service.
 * Rating: 1–5 stars.
 */
public class Feedback {

    private Long id;
    private Long customerId;
    private Long productId;      // optional — null means general feedback

    private String category;    // PRODUCT | SERVICE | DELIVERY | GENERAL
    private Integer rating;     // 1 to 5
    private String comment;

    // PENDING | REVIEWED
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined fields
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String productName;

    public Feedback() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()                  { return id; }
    public Long getCustomerId()          { return customerId; }
    public Long getProductId()           { return productId; }
    public String getCategory()          { return category; }
    public Integer getRating()           { return rating; }
    public String getComment()           { return comment; }
    public String getStatus()            { return status; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }
    public String getCustomerFirstName() { return customerFirstName; }
    public String getCustomerLastName()  { return customerLastName; }
    public String getCustomerEmail()     { return customerEmail; }
    public String getProductName()       { return productName; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                           { this.id = id; }
    public void setCustomerId(Long customerId)           { this.customerId = customerId; }
    public void setProductId(Long productId)             { this.productId = productId; }
    public void setCategory(String category)             { this.category = category; }
    public void setRating(Integer rating)                { this.rating = rating; }
    public void setComment(String comment)               { this.comment = comment; }
    public void setStatus(String status)                 { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt)    { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)    { this.updatedAt = updatedAt; }
    public void setCustomerFirstName(String s)           { this.customerFirstName = s; }
    public void setCustomerLastName(String s)            { this.customerLastName = s; }
    public void setCustomerEmail(String s)               { this.customerEmail = s; }
    public void setProductName(String s)                 { this.productName = s; }

    // ── Helpers ──────────────────────────────────────────────
    public String getCustomerFullName() {
        String f = customerFirstName != null ? customerFirstName : "";
        String l = customerLastName  != null ? customerLastName  : "";
        return (f + " " + l).trim();
    }

    public String getStars() {
        if (rating == null) return "–";
        return "⭐".repeat(rating) + "☆".repeat(Math.max(0, 5 - rating));
    }

    public String getCategoryLabel() {
        if (category == null) return "General";
        switch (category) {
            case "PRODUCT":  return "🍫 Product";
            case "SERVICE":  return "🤝 Service";
            case "DELIVERY": return "🚚 Delivery";
            default:         return "💬 General";
        }
    }
}