package com.brownieshop.dao;

import com.brownieshop.model.ChatMessage;
import com.brownieshop.model.Inquiry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class InquiryDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ── Row Mappers ───────────────────────────────────────────
    private final RowMapper<Inquiry> inquiryMapper = (rs, rn) -> {
        Inquiry i = new Inquiry();
        i.setId(rs.getLong("id"));
        i.setCustomerId(rs.getLong("customer_id"));
        i.setSubject(rs.getString("subject"));
        i.setMessage(rs.getString("message"));
        i.setStatus(rs.getString("status"));
        i.setAdminReply(rs.getString("admin_reply"));
        Timestamp repl = rs.getTimestamp("replied_at");
        if (repl != null) i.setRepliedAt(repl.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) i.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) i.setUpdatedAt(updated.toLocalDateTime());
        try { i.setCustomerFirstName(rs.getString("first_name")); } catch (Exception ignored) {}
        try { i.setCustomerLastName(rs.getString("last_name"));   } catch (Exception ignored) {}
        try { i.setCustomerEmail(rs.getString("email"));          } catch (Exception ignored) {}
        try { i.setCustomerPhone(rs.getString("phone"));          } catch (Exception ignored) {}
        return i;
    };

    private final RowMapper<ChatMessage> chatMapper = (rs, rn) -> {
        ChatMessage m = new ChatMessage();
        m.setId(rs.getLong("id"));
        m.setInquiryId(rs.getLong("inquiry_id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setSenderType(rs.getString("sender_type"));
        m.setSenderName(rs.getString("sender_name"));
        m.setMessage(rs.getString("message"));
        Timestamp sent = rs.getTimestamp("sent_at");
        if (sent != null) m.setSentAt(sent.toLocalDateTime());
        return m;
    };

    // ── INQUIRY CRUD ─────────────────────────────────────────
    public long saveInquiry(Inquiry i) {
        String sql = "INSERT INTO inquiries (customer_id, subject, message, status) VALUES (?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, i.getCustomerId());
            ps.setString(2, i.getSubject());
            ps.setString(3, i.getMessage());
            ps.setString(4, "NEW");
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public Optional<Inquiry> findInquiryById(Long id) {
        List<Inquiry> list = jdbc.query(
                "SELECT i.*, c.first_name, c.last_name, c.email, c.phone " +
                        "FROM inquiries i JOIN customers c ON i.customer_id = c.id WHERE i.id = ?",
                inquiryMapper, id);
        return list.stream().findFirst();
    }

    public List<Inquiry> findAllInquiries() {
        return jdbc.query(
                "SELECT i.*, c.first_name, c.last_name, c.email, c.phone " +
                        "FROM inquiries i JOIN customers c ON i.customer_id = c.id " +
                        "ORDER BY i.created_at DESC",
                inquiryMapper);
    }

    public List<Inquiry> findInquiriesByCustomer(Long customerId) {
        return jdbc.query(
                "SELECT i.*, c.first_name, c.last_name, c.email, c.phone " +
                        "FROM inquiries i JOIN customers c ON i.customer_id = c.id " +
                        "WHERE i.customer_id = ? ORDER BY i.created_at DESC",
                inquiryMapper, customerId);
    }

    public List<Inquiry> findByStatus(String status) {
        return jdbc.query(
                "SELECT i.*, c.first_name, c.last_name, c.email, c.phone " +
                        "FROM inquiries i JOIN customers c ON i.customer_id = c.id " +
                        "WHERE i.status = ? ORDER BY i.created_at DESC",
                inquiryMapper, status);
    }

    public void updateInquiryStatus(Long id, String status) {
        jdbc.update("UPDATE inquiries SET status=?, updated_at=NOW() WHERE id=?", status, id);
    }

    public void saveAdminReply(Long id, String reply) {
        jdbc.update(
                "UPDATE inquiries SET admin_reply=?, replied_at=NOW(), status='IN_PROGRESS', updated_at=NOW() WHERE id=?",
                reply, id);
    }

    public int countByStatus(String status) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inquiries WHERE status=?", Integer.class, status);
        return c != null ? c : 0;
    }

    // ── CHAT MESSAGES ────────────────────────────────────────
    public long saveChatMessage(ChatMessage m) {
        String sql = "INSERT INTO chat_messages (inquiry_id, sender_id, sender_type, sender_name, message) VALUES (?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, m.getInquiryId());
            ps.setLong(2, m.getSenderId());
            ps.setString(3, m.getSenderType());
            ps.setString(4, m.getSenderName());
            ps.setString(5, m.getMessage());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public List<ChatMessage> findMessagesByInquiry(Long inquiryId) {
        return jdbc.query(
                "SELECT * FROM chat_messages WHERE inquiry_id=? ORDER BY sent_at ASC",
                chatMapper, inquiryId);
    }

    /** Returns only messages sent after a given message ID (for polling) */
    public List<ChatMessage> findMessagesAfter(Long inquiryId, Long lastMessageId) {
        return jdbc.query(
                "SELECT * FROM chat_messages WHERE inquiry_id=? AND id>? ORDER BY sent_at ASC",
                chatMapper, inquiryId, lastMessageId);
    }
}