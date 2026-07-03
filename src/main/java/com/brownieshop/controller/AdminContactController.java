package com.brownieshop.controller;

import com.brownieshop.model.ChatMessage;
import com.brownieshop.model.Customer;
import com.brownieshop.model.Inquiry;
import com.brownieshop.service.ContactService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/contact")
public class AdminContactController {

    @Autowired private ContactService contactService;

    private Customer getAdmin(HttpSession session) {
        Customer admin = (Customer) session.getAttribute("adminUser");
        if (admin != null && admin.isAdmin()) return admin;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && fallback.isAdmin()) return fallback;
        return null;
    }

    private boolean isAdmin(HttpSession session) {
        Customer c = getAdmin(session);
        return c != null && c.isAdmin();
    }

    // ── Inbox — all inquiries ─────────────────────────────────
    @GetMapping("/inquiries")
    public String allInquiries(
            @RequestParam(required = false) String status,
            HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        List<Inquiry> inquiries = (status != null && !status.isBlank())
                ? contactService.getInquiriesByStatus(status)
                : contactService.getAllInquiries();

        model.addAttribute("admin",        getAdmin(session));
        model.addAttribute("inquiries",    inquiries);
        model.addAttribute("filterStatus", status);
        model.addAttribute("newCount",     contactService.getUnreadInquiryCount());
        model.addAttribute("notifCount",   contactService.getUnreadInquiryCount());
        return "admin/inquiries";
    }

    // ── Admin Chat page ───────────────────────────────────────
    @GetMapping("/chat/{inquiryId}")
    public String adminChat(@PathVariable Long inquiryId,
                            HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        Optional<Inquiry> opt = contactService.getInquiryById(inquiryId);
        if (opt.isEmpty()) return "redirect:/admin/contact/inquiries";

        model.addAttribute("inquiry",    opt.get());
        model.addAttribute("messages",   contactService.getChatMessages(inquiryId));
        model.addAttribute("admin",      getAdmin(session));
        model.addAttribute("newCount",   contactService.getUnreadInquiryCount());
        model.addAttribute("notifCount", contactService.getUnreadInquiryCount());
        return "admin/chat";
    }

    // ── Admin sends a chat message (AJAX) ─────────────────────
    @PostMapping("/chat/{inquiryId}/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adminSend(
            @PathVariable Long inquiryId,
            @RequestParam String message,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        if (!isAdmin(session)) { resp.put("error", "not admin"); return ResponseEntity.status(403).body(resp); }

        Customer admin = getAdmin(session);
        long msgId = contactService.sendChatMessage(
                inquiryId, admin.getId(), "ADMIN",
                "Support Team", message.trim());

        resp.put("success", true);
        resp.put("messageId", msgId);
        return ResponseEntity.ok(resp);
    }

    // ── Poll new messages (AJAX) ──────────────────────────────
    @GetMapping("/chat/{inquiryId}/poll")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> adminPoll(
            @PathVariable Long inquiryId,
            @RequestParam(defaultValue = "0") Long lastId,
            HttpSession session) {

        if (!isAdmin(session)) return ResponseEntity.status(403).build();

        List<ChatMessage> newMsgs = contactService.getNewMessages(inquiryId, lastId);
        List<Map<String, Object>> result = newMsgs.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",         m.getId());
            map.put("message",    m.getMessage());
            map.put("senderType", m.getSenderType());
            map.put("senderName", m.getSenderName() != null ? m.getSenderName() : "Customer");
            map.put("sentAt",     m.getSentAt() != null ? m.getSentAt().toString() : "");
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── Resolve inquiry ───────────────────────────────────────
    @PostMapping("/inquiries/{id}/resolve")
    public String resolve(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        contactService.resolveInquiry(id);
        return "redirect:/admin/contact/inquiries?success=resolved";
    }

    // ── All Feedback ──────────────────────────────────────────
    @GetMapping("/feedback")
    public String allFeedback(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin",        getAdmin(session));
        model.addAttribute("feedbackList", contactService.getAllFeedback());
        model.addAttribute("trends",       contactService.getRatingTrends());
        model.addAttribute("distribution", contactService.getRatingDistribution());
        model.addAttribute("pendingCount", contactService.getPendingFeedbackCount());
        model.addAttribute("newCount",     contactService.getUnreadInquiryCount());
        model.addAttribute("notifCount",   contactService.getUnreadInquiryCount());
        return "admin/feedback";
    }

    // ── Mark feedback reviewed ────────────────────────────────
    @PostMapping("/feedback/{id}/review")
    public String reviewFeedback(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        contactService.markFeedbackReviewed(id);
        return "redirect:/admin/contact/feedback?success=reviewed";
    }
}