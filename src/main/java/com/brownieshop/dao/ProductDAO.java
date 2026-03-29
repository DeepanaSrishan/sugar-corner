package com.brownieshop.dao;

import com.brownieshop.model.Product;
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
public class ProductDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ── Row Mapper ────────────────────────────────────────────
    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setCategory(rs.getString("category"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setImagePath(rs.getString("image_path"));
        p.setFeatured(rs.getBoolean("featured"));
        p.setAvailable(rs.getBoolean("available"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());

        return p;
    };

    // ── CREATE ────────────────────────────────────────────────
    public long save(Product p) {
        String sql = """
            INSERT INTO products
              (name, description, category, price, stock_quantity, image_path, featured, available)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,  p.getName());
            ps.setString(2,  p.getDescription());
            ps.setString(3,  p.getCategory());
            ps.setBigDecimal(4, p.getPrice());
            ps.setInt(5,     p.getStockQuantity() != null ? p.getStockQuantity() : 0);
            ps.setString(6,  p.getImagePath());
            ps.setBoolean(7, p.isFeatured());
            ps.setBoolean(8, p.isAvailable());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    // ── READ ──────────────────────────────────────────────────
    public Optional<Product> findById(Long id) {
        List<Product> list = jdbc.query(
                "SELECT * FROM products WHERE id = ?", productRowMapper, id);
        return list.stream().findFirst();
    }

    /** All products (admin view – includes unavailable) */
    public List<Product> findAll() {
        return jdbc.query(
                "SELECT * FROM products ORDER BY created_at DESC",
                productRowMapper);
    }

    /** All AVAILABLE products (customer-facing) */
    public List<Product> findAllAvailable() {
        return jdbc.query(
                "SELECT * FROM products WHERE available = true ORDER BY featured DESC, created_at DESC",
                productRowMapper);
    }

    /** Search by name (customer-facing, available only) */
    public List<Product> searchByName(String keyword) {
        return jdbc.query(
                "SELECT * FROM products WHERE available = true AND name LIKE ? ORDER BY featured DESC",
                productRowMapper, "%" + keyword + "%");
    }

    /** Filter by category (customer-facing, available only) */
    public List<Product> findByCategory(String category) {
        return jdbc.query(
                "SELECT * FROM products WHERE available = true AND category = ? ORDER BY featured DESC",
                productRowMapper, category);
    }

    /** Filter by max price (customer-facing, available only) */
    public List<Product> findByMaxPrice(double maxPrice) {
        return jdbc.query(
                "SELECT * FROM products WHERE available = true AND price <= ? ORDER BY price ASC",
                productRowMapper, maxPrice);
    }

    /** Featured / popular brownies */
    public List<Product> findFeatured() {
        return jdbc.query(
                "SELECT * FROM products WHERE available = true AND featured = true ORDER BY created_at DESC",
                productRowMapper);
    }

    /** All distinct categories */
    public List<String> findAllCategories() {
        return jdbc.queryForList(
                "SELECT DISTINCT category FROM products WHERE available = true ORDER BY category",
                String.class);
    }

    // ── UPDATE ────────────────────────────────────────────────
    public void update(Product p) {
        jdbc.update("""
            UPDATE products SET
              name=?, description=?, category=?, price=?,
              stock_quantity=?, featured=?, available=?,
              updated_at=NOW()
            WHERE id=?
            """,
                p.getName(), p.getDescription(), p.getCategory(), p.getPrice(),
                p.getStockQuantity(), p.isFeatured(), p.isAvailable(),
                p.getId());
    }

    public void updateImage(Long id, String path) {
        jdbc.update(
                "UPDATE products SET image_path=?, updated_at=NOW() WHERE id=?",
                path, id);
    }

    public void setAvailability(Long id, boolean available) {
        jdbc.update(
                "UPDATE products SET available=?, updated_at=NOW() WHERE id=?",
                available, id);
    }

    // ── DELETE ────────────────────────────────────────────────
    public void deleteById(Long id) {
        jdbc.update("DELETE FROM products WHERE id=?", id);
    }
}