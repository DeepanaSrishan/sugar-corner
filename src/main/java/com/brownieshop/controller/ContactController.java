package com.brownieshop.controller;

import com.brownieshop.model.ChatMessage;
import com.brownieshop.model.Customer;
import com.brownieshop.model.Inquiry;
import com.brownieshop.service.ContactService;
import com.brownieshop.service.ProductService;
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
public class ContactController {

    @Autowired private ContactService contactService;
    @Autowired private ProductService  productService;

    private Customer loggedIn(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customerUser");
        if (c != null) return c;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && !fallback.isAdmin()) return fallback;
        return null;
    }

    // ── Contact & Inquiry form ─────────────────────────────────
    @GetMapping("/contact")
    public String contactPage(HttpSession session, Model model) {
        Customer c = loggedIn(session);
        if (c == null) return "redirect:/login";
        model.addAttribute("customer", c);
        model.addAttribute("inquiries", contactService.getInquiriesByCustomer(c.getId()));
        model.addAttribute("myFeedback", contactService.getFeedbackByCustomer(c.getId()));
        model.addAttribute("products", productService.getAllProducts());
        return "contact/contact";
    }

    // ── Submit Inquiry ─────────────────────────────────────────
    @PostMapping("/contact/inquiry")
    public String submitInquiry(
            @RequestParam String subject,
            @RequestParam String message,
            HttpSession session) {
        Customer c = loggedIn(session);
        if (c == null) return "redirect:/login";
        long id = contactService.submitInquiry(c.getId(), subject, message);
        return "redirect:/contact/chat/" + id + "?sent=true";
    }

    // ── Live Chat page ─────────────────────────────────────────
    @GetMapping("/contact/chat/{inquiryId}")
    public String chatPage(@PathVariable Long inquiryId,
                           HttpSession session, Model model) {
        Customer c = loggedIn(session);
        if (c == null) return "redirect:/login";

        Optional<Inquiry> opt = contactService.getInquiryById(inquiryId);
        if (opt.isEmpty() || !opt.get().getCustomerId().equals(c.getId()))
            return "redirect:/contact";

        model.addAttribute("inquiry", opt.get());
        model.addAttribute("messages", contactService.getChatMessages(inquiryId));
        model.addAttribute("customer", c);
        return "contact/chat";
    }

    // ── Send Chat Message (AJAX) ──────────────────────────────
    @PostMapping("/contact/chat/{inquiryId}/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long inquiryId,
            @RequestParam String message,
            HttpSession session) {

        Customer c = loggedIn(session);
        Map<String, Object> resp = new HashMap<>();
        if (c == null) { resp.put("error", "not logged in"); return ResponseEntity.status(401).body(resp); }

        Optional<Inquiry> opt = contactService.getInquiryById(inquiryId);
        if (opt.isEmpty() || !opt.get().getCustomerId().equals(c.getId())) {
            resp.put("error", "not found"); return ResponseEntity.status(403).body(resp);
        }

        long msgId = contactService.sendChatMessage(
                inquiryId, c.getId(), "CUSTOMER", c.getFullName(), message.trim());

        resp.put("success", true);
        resp.put("messageId", msgId);
        return ResponseEntity.ok(resp);
    }

    // ── Poll for new messages (AJAX) ──────────────────────────
    @GetMapping("/contact/chat/{inquiryId}/poll")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> pollMessages(
            @PathVariable Long inquiryId,
            @RequestParam(defaultValue = "0") Long lastId,
            HttpSession session) {

        Customer c = loggedIn(session);
        if (c == null) return ResponseEntity.status(401).build();

        List<ChatMessage> newMsgs = contactService.getNewMessages(inquiryId, lastId);
        List<Map<String, Object>> result = newMsgs.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",         m.getId());
            map.put("message",    m.getMessage());
            map.put("senderType", m.getSenderType());
            map.put("senderName", m.getSenderName() != null ? m.getSenderName() : "Support");
            map.put("sentAt",     m.getSentAt() != null ? m.getSentAt().toString() : "");
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── Submit Feedback ────────────────────────────────────────
    @PostMapping("/contact/feedback")
    public String submitFeedback(
            @RequestParam String category,
            @RequestParam Integer rating,
            @RequestParam String comment,
            @RequestParam(required = false) Long productId,
            HttpSession session) {

        Customer c = loggedIn(session);
        if (c == null) return "redirect:/login";
        contactService.submitFeedback(c.getId(), productId, category, rating, comment);
        return "redirect:/contact?feedbackSent=true";
    }
}