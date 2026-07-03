package com.brownieshop.service;

import com.brownieshop.dao.FeedbackDAO;
import com.brownieshop.dao.InquiryDAO;
import com.brownieshop.model.ChatMessage;
import com.brownieshop.model.Feedback;
import com.brownieshop.model.Inquiry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ContactService {

    @Autowired private InquiryDAO  inquiryDAO;
    @Autowired private FeedbackDAO feedbackDAO;

    // ── INQUIRY ───────────────────────────────────────────────
    public long submitInquiry(Long customerId, String subject, String message) {
        Inquiry i = new Inquiry();
        i.setCustomerId(customerId);
        i.setSubject(subject);
        i.setMessage(message);
        long id = inquiryDAO.saveInquiry(i);
        // First chat message = the inquiry itself
        ChatMessage cm = new ChatMessage();
        cm.setInquiryId(id);
        cm.setSenderId(customerId);
        cm.setSenderType("CUSTOMER");
        cm.setMessage(message);
        inquiryDAO.saveChatMessage(cm);
        return id;
    }

    public Optional<Inquiry> getInquiryById(Long id) {
        return inquiryDAO.findInquiryById(id);
    }

    public List<Inquiry> getInquiriesByCustomer(Long customerId) {
        return inquiryDAO.findInquiriesByCustomer(customerId);
    }

    public List<Inquiry> getAllInquiries() {
        return inquiryDAO.findAllInquiries();
    }

    public List<Inquiry> getInquiriesByStatus(String status) {
        return inquiryDAO.findByStatus(status);
    }

    public void resolveInquiry(Long id) {
        inquiryDAO.updateInquiryStatus(id, "RESOLVED");
    }

    // ── CHAT ─────────────────────────────────────────────────
    public long sendChatMessage(Long inquiryId, Long senderId,
                                String senderType, String senderName, String message) {
        ChatMessage cm = new ChatMessage();
        cm.setInquiryId(inquiryId);
        cm.setSenderId(senderId);
        cm.setSenderType(senderType);
        cm.setSenderName(senderName);
        cm.setMessage(message);
        long msgId = inquiryDAO.saveChatMessage(cm);

        // Update inquiry status when admin replies
        if ("ADMIN".equals(senderType)) {
            inquiryDAO.saveAdminReply(inquiryId, message);
        }
        return msgId;
    }

    public List<ChatMessage> getChatMessages(Long inquiryId) {
        return inquiryDAO.findMessagesByInquiry(inquiryId);
    }

    /** Returns new messages after lastMessageId — used for polling */
    public List<ChatMessage> getNewMessages(Long inquiryId, Long lastMessageId) {
        return inquiryDAO.findMessagesAfter(inquiryId, lastMessageId);
    }

    // ── FEEDBACK ──────────────────────────────────────────────
    public long submitFeedback(Long customerId, Long productId,
                               String category, Integer rating, String comment) {
        Feedback f = new Feedback();
        f.setCustomerId(customerId);
        f.setProductId(productId);
        f.setCategory(category);
        f.setRating(rating);
        f.setComment(comment);
        return feedbackDAO.save(f);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackDAO.findAll();
    }

    public List<Feedback> getFeedbackByCustomer(Long customerId) {
        return feedbackDAO.findByCustomer(customerId);
    }

    public void markFeedbackReviewed(Long id) {
        feedbackDAO.markReviewed(id);
    }

    // ── STATS ─────────────────────────────────────────────────
    public int getUnreadInquiryCount()     { return inquiryDAO.countByStatus("NEW"); }
    public int getPendingFeedbackCount()   { return feedbackDAO.countByStatus("PENDING"); }
    public List<Map<String, Object>> getRatingTrends() { return feedbackDAO.getRatingTrends(); }
    public List<Map<String, Object>> getRatingDistribution() { return feedbackDAO.getRatingDistribution(); }
}