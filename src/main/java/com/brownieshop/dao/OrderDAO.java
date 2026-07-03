package com.brownieshop.dao;

import com.brownieshop.model.Order;
import com.brownieshop.model.OrderItem;
import jakarta.annotation.PostConstruct;
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
public class OrderDAO {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void ensureDiscountColumns() {
        ensureColumn("orders", "subtotal_before_discount", "DECIMAL(10,2) NOT NULL DEFAULT 0");
        ensureColumn("orders", "discount_percent", "DECIMAL(5,2) NOT NULL DEFAULT 0");
        ensureColumn("orders", "discount_amount", "DECIMAL(10,2) NOT NULL DEFAULT 0");
        ensureColumn("orders", "applied_tier", "VARCHAR(30) NOT NULL DEFAULT 'NEW'");
    }

    private void ensureColumn(String tableName, String columnName, String columnDefinition) {
        try {
            boolean exists = false;
            try (Connection connection = jdbc.getDataSource().getConnection()) {
                DatabaseMetaData meta = connection.getMetaData();
                try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
                    exists = rs.next();
                }
            }

            if (!exists) {
                jdbc.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to ensure column " + tableName + "." + columnName, ex);
        }
    }

    // ── Row Mappers ───────────────────────────────────────────
    private final RowMapper<Order> orderRowMapper = (rs, rn) -> {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setCustomerId(rs.getLong("customer_id"));
        o.setStatus(rs.getString("status"));
        o.setDeliveryType(rs.getString("delivery_type"));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setDeliveryCity(rs.getString("delivery_city"));
        o.setDeliveryPostalCode(rs.getString("delivery_postal_code"));
        try { o.setSubtotalBeforeDiscount(rs.getBigDecimal("subtotal_before_discount")); } catch (SQLException ignored) {}
        try { o.setDiscountPercent(rs.getBigDecimal("discount_percent")); } catch (SQLException ignored) {}
        try { o.setDiscountAmount(rs.getBigDecimal("discount_amount")); } catch (SQLException ignored) {}
        try { o.setAppliedTier(rs.getString("applied_tier")); } catch (SQLException ignored) {}
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setCustomerNote(rs.getString("customer_note"));
        o.setAdminNote(rs.getString("admin_note"));
        Timestamp req = rs.getTimestamp("requested_time");
        if (req != null) o.setRequestedTime(req.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) o.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) o.setUpdatedAt(updated.toLocalDateTime());
        // joined customer fields
        try { o.setCustomerFirstName(rs.getString("first_name")); } catch (SQLException ignored) {}
        try { o.setCustomerLastName(rs.getString("last_name")); }   catch (SQLException ignored) {}
        try { o.setCustomerEmail(rs.getString("email")); }          catch (SQLException ignored) {}
        try { o.setCustomerPhone(rs.getString("phone")); }          catch (SQLException ignored) {}
        // customer registered address (for admin view when delivery address is empty)
        try { o.setCustomerAddress(rs.getString("address_line1")); }     catch (SQLException ignored) {}
        try { o.setCustomerCity(rs.getString("cust_city")); }            catch (SQLException ignored) {}
        try { o.setCustomerPostalCode(rs.getString("cust_postal")); }    catch (SQLException ignored) {}
        return o;
    };

    private final RowMapper<OrderItem> itemRowMapper = (rs, rn) -> {
        OrderItem i = new OrderItem();
        i.setId(rs.getLong("id"));
        i.setOrderId(rs.getLong("order_id"));
        i.setProductId(rs.getLong("product_id"));
        i.setProductName(rs.getString("product_name"));
        i.setProductImage(rs.getString("product_image"));
        i.setUnitPrice(rs.getBigDecimal("unit_price"));
        i.setQuantity(rs.getInt("quantity"));
        i.setCustomization(rs.getString("customization"));
        return i;
    };

    // ── CREATE ORDER ─────────────────────────────────────────
    public long saveOrder(Order o) {
        String sql = """
            INSERT INTO orders
              (customer_id, status, delivery_type, delivery_address,
                             delivery_city, delivery_postal_code, subtotal_before_discount,
                             discount_percent, discount_amount, applied_tier, total_amount,
                             customer_note, requested_time)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, o.getCustomerId());
            ps.setString(2, o.getStatus() != null ? o.getStatus() : "PENDING");
            ps.setString(3, o.getDeliveryType());
            ps.setString(4, o.getDeliveryAddress());
            ps.setString(5, o.getDeliveryCity());
            ps.setString(6, o.getDeliveryPostalCode());
            ps.setBigDecimal(7, o.getSubtotalBeforeDiscountSafe());
            ps.setBigDecimal(8, o.getDiscountPercentSafe());
            ps.setBigDecimal(9, o.getDiscountAmountSafe());
            ps.setString(10, o.getAppliedTier() != null ? o.getAppliedTier() : "NEW");
            ps.setBigDecimal(11, o.getTotalAmount());
            ps.setString(12, o.getCustomerNote());
            ps.setObject(13, o.getRequestedTime());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : -1L;
    }

    // ── CREATE ORDER ITEMS ───────────────────────────────────
    public void saveOrderItem(OrderItem item) {
        jdbc.update("""
            INSERT INTO order_items
              (order_id, product_id, product_name, product_image,
               unit_price, quantity, customization)
            VALUES (?,?,?,?,?,?,?)
            """,
                item.getOrderId(), item.getProductId(),
                item.getProductName(), item.getProductImage(),
                item.getUnitPrice(), item.getQuantity(),
                item.getCustomization());
    }

    // ── READ ─────────────────────────────────────────────────
    public Optional<Order> findById(Long id) {
        List<Order> list = jdbc.query(
                "SELECT o.*, c.first_name, c.last_name, c.email, c.phone, " +
                        "c.address_line1, c.city AS cust_city, c.postal_code AS cust_postal " +
                        "FROM orders o JOIN customers c ON o.customer_id = c.id WHERE o.id = ?",
                orderRowMapper, id);
        return list.stream().findFirst();
    }

    public List<OrderItem> findItemsByOrderId(Long orderId) {
        return jdbc.query(
                "SELECT * FROM order_items WHERE order_id = ? ORDER BY id",
                itemRowMapper, orderId);
    }

    public List<Order> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT o.*, c.first_name, c.last_name, c.email, c.phone, " +
                        "c.address_line1, c.city AS cust_city, c.postal_code AS cust_postal " +
                        "FROM orders o JOIN customers c ON o.customer_id = c.id " +
                        "WHERE o.customer_id = ? ORDER BY o.created_at DESC",
                orderRowMapper, customerId);
    }

    /** Admin: all orders newest first */
    public List<Order> findAll() {
        return jdbc.query(
                "SELECT o.*, c.first_name, c.last_name, c.email, c.phone, " +
                        "c.address_line1, c.city AS cust_city, c.postal_code AS cust_postal " +
                        "FROM orders o JOIN customers c ON o.customer_id = c.id " +
                        "ORDER BY o.created_at DESC",
                orderRowMapper);
    }

    /** Admin: search by order ID */
    public List<Order> findByOrderId(Long orderId) {
        return jdbc.query(
                "SELECT o.*, c.first_name, c.last_name, c.email, c.phone, " +
                        "c.address_line1, c.city AS cust_city, c.postal_code AS cust_postal " +
                        "FROM orders o JOIN customers c ON o.customer_id = c.id WHERE o.id = ?",
                orderRowMapper, orderId);
    }

    /** Admin: search by date (yyyy-MM-dd) */
    public List<Order> findByDate(String date) {
        return jdbc.query(
                "SELECT o.*, c.first_name, c.last_name, c.email, c.phone, " +
                        "c.address_line1, c.city AS cust_city, c.postal_code AS cust_postal " +
                        "FROM orders o JOIN customers c ON o.customer_id = c.id " +
                        "WHERE DATE(o.created_at) = ? ORDER BY o.created_at DESC",
                orderRowMapper, date);
    }

    /** Count of PENDING orders (for notification badge) */
    public int countPendingOrders() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'PENDING'", Integer.class);
        return count != null ? count : 0;
    }

    /** Count of new orders placed in last 24 hours */
    public int countNewOrdersToday() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE created_at >= NOW() - INTERVAL 1 DAY",
                Integer.class);
        return count != null ? count : 0;
    }

    // ── UPDATE ───────────────────────────────────────────────
    public void updateStatus(Long orderId, String status) {
        jdbc.update(
                "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?",
                status, orderId);
    }

    public void updateAdminNote(Long orderId, String note) {
        jdbc.update(
                "UPDATE orders SET admin_note = ?, updated_at = NOW() WHERE id = ?",
                note, orderId);
    }

    public void cancelOrder(Long orderId) {
        jdbc.update(
                "UPDATE orders SET status = 'CANCELLED', updated_at = NOW() WHERE id = ?",
                orderId);
    }

    // ── LOYALTY / MEMBERSHIP ─────────────────────────────────

    /**
     * Counts completed orders for a customer (only real completed orders).
     * Used to calculate their membership tier:
     *   1-2  → NEW
     *   3-5  → REGULAR
     *   6-10 → LOYAL
     *   10+  → VIP
     *
     * Only counts DELIVERED orders — not pending/cancelled.
     */
    public int countCompletedByCustomerId(Long customerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders " +
                        "WHERE customer_id = ? AND status = 'DELIVERED'",
                Integer.class, customerId);
        return count != null ? count : 0;
    }

    /**
     * Returns a map of customerId → completed order count for ALL customers.
     * More efficient than calling countCompletedByCustomerId() per customer
     * when loading the full customer list.
     */
    public java.util.Map<Long, Integer> countCompletedOrdersAllCustomers() {
        java.util.Map<Long, Integer> map = new java.util.HashMap<>();
        jdbc.query(
                "SELECT customer_id, COUNT(*) as cnt FROM orders " +
                        "WHERE status = 'DELIVERED' GROUP BY customer_id",
                rs -> {
                    map.put(rs.getLong("customer_id"), rs.getInt("cnt"));
                }
        );
        return map;
    }

}