package com.brownieshop.service;

import com.brownieshop.dao.OrderDAO;
import com.brownieshop.dao.PaymentDAO;
import com.brownieshop.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired private PaymentDAO paymentDAO;
    @Autowired private OrderDAO   orderDAO;

    // ── CREATE PAYMENT (COD) ──────────────────────────────────
    public long createPayment(Long orderId, Long customerId,
                              BigDecimal amount, String paymentMethod) {

        // Prevent double-payment
        if (paymentDAO.existsByOrderId(orderId)) {
            return paymentDAO.findByOrderId(orderId)
                    .map(Payment::getId).orElse(-1L);
        }

        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setCustomerId(customerId);
        p.setAmount(amount);
        p.setPaymentMethod(paymentMethod);
        p.setTransactionRef(generateRef(paymentMethod));

        // ✅ FIXED HERE
        if ("CASH_ON_DELIVERY".equals(paymentMethod)) {
            p.setStatus("PENDING");
        } else {
            p.setStatus("COMPLETED");
            p.setPaidAt(LocalDateTime.now());
        }

        return paymentDAO.save(p);
    }

    // ── CREATE PAYMENT WITH CUSTOM REF ───────────────────────
    public long createPaymentWithRef(Long orderId, Long customerId,
                                     BigDecimal amount, String paymentMethod,
                                     String transactionRef) {

        if (paymentDAO.existsByOrderId(orderId)) {
            return paymentDAO.findByOrderId(orderId)
                    .map(Payment::getId).orElse(-1L);
        }

        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setCustomerId(customerId);
        p.setAmount(amount);
        p.setPaymentMethod(paymentMethod);
        p.setTransactionRef(transactionRef != null ? transactionRef : generateRef(paymentMethod));

        // ✅ FIXED HERE
        p.setStatus("COMPLETED");
        p.setPaidAt(LocalDateTime.now());

        return paymentDAO.save(p);
    }

    // ── PROCESS (aliases) ────────────────────────────────────
    public long processPaymentWithRef(Long orderId, Long customerId,
                                      BigDecimal amount, String method, String txRef) {
        return createPaymentWithRef(orderId, customerId, amount, method, txRef);
    }

    public long processPayment(Long orderId, Long customerId,
                               String method, BigDecimal amount) {
        return createPayment(orderId, customerId, amount, method);
    }

    // ── READ ─────────────────────────────────────────────────
    public Optional<Payment> getById(Long id)           { return paymentDAO.findById(id); }
    public Optional<Payment> getByOrderId(Long orderId) { return paymentDAO.findByOrderId(orderId); }
    public List<Payment> getByCustomer(Long customerId) { return paymentDAO.findByCustomerId(customerId); }
    public List<Payment> getAllPayments()               { return paymentDAO.findAll(); }
    public List<Payment> getAll()                       { return paymentDAO.findAll(); }

    public List<Payment> getByStatus(String status)     { return paymentDAO.findByStatus(status); }

    // ── SEARCH ───────────────────────────────────────────────
    public List<Payment> search(String paymentId, String date, String status) {

        if (paymentId != null && !paymentId.isBlank()) {
            try {
                return paymentDAO.findById(Long.parseLong(paymentId.trim()))
                        .map(List::of).orElse(List.of());
            } catch (NumberFormatException e) {
                return List.of();
            }
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return paymentDAO.findByStatus(status);
        }

        if (date != null && !date.isBlank()) {
            return paymentDAO.findByDate(date);
        }

        return paymentDAO.findAll();
    }

    // ── ADMIN ACTIONS ─────────────────────────────────────────
    public void markCompleted(Long paymentId) {
        paymentDAO.markCompleted(paymentId);
    }

    public boolean processRefund(Long paymentId, String reason) {
        Optional<Payment> opt = paymentDAO.findById(paymentId);
        if (opt.isEmpty()) return false;

        // ✅ FIXED HERE
        if (!"COMPLETED".equals(opt.get().getStatus())) return false;

        paymentDAO.markRefunded(paymentId, reason);
        return true;
    }

    // ── STATS ────────────────────────────────────────────────
    public int getPendingPaymentCount()   { return paymentDAO.countPendingPayments(); }
    public int getPendingCount()          { return paymentDAO.countPendingPayments(); }
    public int getCompletedPaymentCount() { return paymentDAO.countByStatus("COMPLETED"); }
    public BigDecimal getTotalRevenue()   { return paymentDAO.sumCompleted(); }
    public BigDecimal getTodayRevenue()   { return paymentDAO.sumCompletedToday(); }

    // ── HELPER ───────────────────────────────────────────────
    private String generateRef(String method) {
        String prefix = "CASH_ON_DELIVERY".equals(method) ? "COD"
                : "CARD".equals(method) ? "CRD"
                : "BNK";

        return prefix + "-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
    }
}