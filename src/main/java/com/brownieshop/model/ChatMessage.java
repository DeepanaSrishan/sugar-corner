package com.brownieshop.model;

import java.time.LocalDateTime;

/**
 * ChatMessage — a single message in a chat thread linked to an Inquiry.
 * sender_type: CUSTOMER | ADMIN
 */
public class ChatMessage {

    private Long id;
    private Long inquiryId;
    private Long senderId;
    private String senderType;   // CUSTOMER | ADMIN
    private String senderName;
    private String message;
    private LocalDateTime sentAt;

    public ChatMessage() {}

    // ── Getters ──────────────────────────────────────────────
    public Long getId()                 { return id; }
    public Long getInquiryId()          { return inquiryId; }
    public Long getSenderId()           { return senderId; }
    public String getSenderType()       { return senderType; }
    public String getSenderName()       { return senderName; }
    public String getMessage()          { return message; }
    public LocalDateTime getSentAt()    { return sentAt; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(Long id)                         { this.id = id; }
    public void setInquiryId(Long inquiryId)           { this.inquiryId = inquiryId; }
    public void setSenderId(Long senderId)             { this.senderId = senderId; }
    public void setSenderType(String senderType)       { this.senderType = senderType; }
    public void setSenderName(String senderName)       { this.senderName = senderName; }
    public void setMessage(String message)             { this.message = message; }
    public void setSentAt(LocalDateTime sentAt)        { this.sentAt = sentAt; }

    public boolean isFromAdmin() { return "ADMIN".equals(senderType); }
}