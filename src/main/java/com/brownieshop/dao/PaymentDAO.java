package com.brownieshop.dao;

import com.brownieshop.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ── Row Mapper ───────────────────────────────────────────
    private final RowMapper<Payment> rowMapper = (rs, rn) -> {
        Payment p = new Payment();

        p.setId(rs.getLong("id"));
        p.setOrderId(rs.getLong("order_id"));
        p.setCustomerId(rs.getLong("customer_id"));

        p.setPaymentMethod(rs.getString("payment_method"));
        p.setStatus(rs.getString("status"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setTransactionRef(rs.getString("transaction_ref"));
        p.setNotes(rs.getString("notes"));

        Timestamp paid = rs.getTimestamp("paid_at");
        if (paid != null) p.setPaidAt(paid.toLocalDateTime());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());

        // FIX: Read joined customer fields when present (queries that JOIN customers).
        // Uses try-catch so simple SELECT * queries still work without crashing.
        try { p.setCustomerFirstName(rs.getString("first_name")); } catch (Exception ignored) {}
        try { p.setCustomerLastName(rs.getString("last_name"));   } catch (Exception ignored) {}
        try { p.setCustomerEmail(rs.getString("email"));          } catch (Exception ignored) {}

        return p;
    };

    // ── CREATE ───────────────────────────────────────────────
    public long save(Payment p) {
        String sql = """
            INSERT INTO payments
              (order_id, customer_id, payment_method, status,   -- 🔧 FIX HERE
               amount, transaction_ref, notes, paid_at)
            VALUES (?,?,?,?,?,?,?,?)
            """;

        KeyHolder kh = new GeneratedKeyHolder();

        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, p.getOrderId());
            ps.setLong(2, p.getCustomerId());
            ps.setString(3, p.getPaymentMethod());

            // 🔧 FIX: now using status column
            ps.setString(4, p.getStatus() != null ? p.getStatus() : "PENDING");

            ps.setBigDecimal(5, p.getAmount());
            ps.setString(6, p.getTransactionRef());
            ps.setString(7, p.getNotes());
            ps.setObject(8, p.getPaidAt());

            return ps;
        }, kh);

        return kh.getKey().longValue();
    }

    // ── READ ─────────────────────────────────────────────────
    // FIX: All read queries now JOIN customers so that
    //      customerFirstName, customerLastName, customerEmail
    //      are populated — required by payment-detail.html customer box.
    public Optional<Payment> findById(Long id) {
        List<Payment> list = jdbc.query(
                "SELECT p.*, c.first_name, c.last_name, c.email " +
                        "FROM payments p " +
                        "JOIN customers c ON p.customer_id = c.id " +
                        "WHERE p.id = ?",
                rowMapper, id);
        return list.stream().findFirst();
    }

    public Optional<Payment> findByOrderId(Long orderId) {
        List<Payment> list = jdbc.query(
                "SELECT p.*, c.first_name, c.last_name, c.email " +
                        "FROM payments p " +
                        "JOIN customers c ON p.customer_id = c.id " +
                        "WHERE p.order_id = ?",
                rowMapper, orderId);
        return list.stream().findFirst();
    }

    public List<Payment> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT p.*, c.first_name, c.last_name, c.email " +
                        "FROM payments p " +
                        "JOIN customers c ON p.customer_id = c.id " +
                        "WHERE p.customer_id = ? ORDER BY p.created_at DESC",
                rowMapper, customerId);
    }

    public List<Payment> findAll() {
        return jdbc.query(
                "SELECT p.*, c.first_name, c.last_name, c.email " +
                        "FROM payments p " +
                        "JOIN customers c ON p.customer_id = c.id " +
                        "ORDER BY p.created_at DESC",
                rowMapper);
    }

    public List<Payment> findByStatus(String status) {
        return jdbc.query(
                "SELECT p.*, c.first_name, c.last_name, c.email " +
                        "FROM payments p " +
                        "JOIN customers c ON p.customer_id = c.id " +
                        "WHERE p.status = ? ORDER BY p.created_at DESC",
                rowMapper, status);
    }

    public List<Payment> findByDate(String date) {
        return jdbc.query(
                "SELECT p.*, c.first_name, c.last_name, c.email " +
                        "FROM payments p " +
                        "JOIN customers c ON p.customer_id = c.id " +
                        "WHERE DATE(p.created_at) = ? ORDER BY p.created_at DESC",
                rowMapper, date);
    }

    public boolean existsByOrderId(Long orderId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE order_id = ?",
                Integer.class, orderId);
        return count != null && count > 0;
    }

    // ── UPDATE ───────────────────────────────────────────────
    public void updateStatus(Long id, String status, String notes) {
        jdbc.update(
                "UPDATE payments SET status=?, notes=?, updated_at=NOW() WHERE id=?", // 🔧 FIX
                status, notes, id);
    }

    public void markCompleted(Long id) {
        jdbc.update(
                "UPDATE payments SET status='COMPLETED', paid_at=NOW(), updated_at=NOW() WHERE id=?", // 🔧 FIX
                id);
    }

    public void markRefunded(Long id, String notes) {
        jdbc.update(
                "UPDATE payments SET status='REFUNDED', notes=?, updated_at=NOW() WHERE id=?", // 🔧 FIX
                notes, id);
    }

    // ── STATS ────────────────────────────────────────────────
    public int countByStatus(String status) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE status=?", // 🔧 FIX
                Integer.class, status);
        return c != null ? c : 0;
    }

    public BigDecimal sumCompleted() {
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='COMPLETED'", // 🔧 FIX
                BigDecimal.class);
        return total != null ? total : BigDecimal.ZERO;
    }

    public int countPendingPayments() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE status = 'PENDING'", // 🔧 FIX
                Integer.class);
        return count != null ? count : 0;
    }

    public BigDecimal sumCompletedToday() {
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM payments " +
                        "WHERE status='COMPLETED' AND DATE(paid_at) = CURDATE()", // 🔧 FIX
                BigDecimal.class);
        return sum != null ? sum : BigDecimal.ZERO;
    }

}