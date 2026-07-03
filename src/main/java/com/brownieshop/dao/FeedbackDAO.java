package com.brownieshop.dao;

import com.brownieshop.model.Feedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Map;

@Repository
public class FeedbackDAO {

    @Autowired
    private JdbcTemplate jdbc;

    private final RowMapper<Feedback> feedbackMapper = (rs, rn) -> {
        Feedback f = new Feedback();
        f.setId(rs.getLong("id"));
        f.setCustomerId(rs.getLong("customer_id"));
        long pid = rs.getLong("product_id");
        if (!rs.wasNull()) f.setProductId(pid);
        f.setCategory(rs.getString("category"));
        f.setRating(rs.getInt("rating"));
        f.setComment(rs.getString("comment"));
        f.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) f.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) f.setUpdatedAt(updated.toLocalDateTime());
        try { f.setCustomerFirstName(rs.getString("first_name")); } catch (Exception ignored) {}
        try { f.setCustomerLastName(rs.getString("last_name"));   } catch (Exception ignored) {}
        try { f.setCustomerEmail(rs.getString("email"));          } catch (Exception ignored) {}
        try { f.setProductName(rs.getString("product_name"));     } catch (Exception ignored) {}
        return f;
    };

    public long save(Feedback f) {
        String sql = "INSERT INTO feedback (customer_id, product_id, category, rating, comment, status) VALUES (?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, f.getCustomerId());
            if (f.getProductId() != null) ps.setLong(2, f.getProductId());
            else ps.setNull(2, Types.BIGINT);
            ps.setString(3, f.getCategory() != null ? f.getCategory() : "GENERAL");
            ps.setInt(4, f.getRating() != null ? f.getRating() : 5);
            ps.setString(5, f.getComment());
            ps.setString(6, "PENDING");
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public List<Feedback> findAll() {
        return jdbc.query(
                "SELECT f.*, c.first_name, c.last_name, c.email, " +
                        "p.name AS product_name " +
                        "FROM feedback f " +
                        "JOIN customers c ON f.customer_id = c.id " +
                        "LEFT JOIN products p ON f.product_id = p.id " +
                        "ORDER BY f.created_at DESC",
                feedbackMapper);
    }

    public List<Feedback> findByCustomer(Long customerId) {
        return jdbc.query(
                "SELECT f.*, c.first_name, c.last_name, c.email, " +
                        "p.name AS product_name " +
                        "FROM feedback f " +
                        "JOIN customers c ON f.customer_id = c.id " +
                        "LEFT JOIN products p ON f.product_id = p.id " +
                        "WHERE f.customer_id = ? ORDER BY f.created_at DESC",
                feedbackMapper, customerId);
    }

    public void markReviewed(Long id) {
        jdbc.update("UPDATE feedback SET status='REVIEWED', updated_at=NOW() WHERE id=?", id);
    }

    public int countByStatus(String status) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM feedback WHERE status=?", Integer.class, status);
        return c != null ? c : 0;
    }

    /** Trend analysis: average rating per category */
    public List<Map<String, Object>> getRatingTrends() {
        return jdbc.queryForList(
                "SELECT category, COUNT(*) as total, AVG(rating) as avg_rating, " +
                        "SUM(CASE WHEN rating >= 4 THEN 1 ELSE 0 END) as positive " +
                        "FROM feedback GROUP BY category ORDER BY avg_rating DESC");
    }

    /** Rating distribution: how many 1★, 2★, ... 5★ */
    public List<Map<String, Object>> getRatingDistribution() {
        return jdbc.queryForList(
                "SELECT rating, COUNT(*) as count FROM feedback GROUP BY rating ORDER BY rating DESC");
    }
}