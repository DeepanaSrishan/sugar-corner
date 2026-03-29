package com.brownieshop.dao;

import com.brownieshop.model.Customer;
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
public class CustomerDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ── Row Mapper ────────────────────────────────────────────
    private final RowMapper<Customer> customerRowMapper = (rs, rowNum) -> {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setEmail(rs.getString("email"));
        c.setPassword(rs.getString("password"));
        c.setPhone(rs.getString("phone"));
        c.setAddressLine1(rs.getString("address_line1"));
        c.setAddressLine2(rs.getString("address_line2"));
        c.setCity(rs.getString("city"));
        c.setPostalCode(rs.getString("postal_code"));
        c.setCountry(rs.getString("country"));

        Date dob = rs.getDate("date_of_birth");
        if (dob != null) c.setDateOfBirth(dob.toLocalDate());

        c.setProfilePhotoPath(rs.getString("profile_photo_path"));
        c.setStatus(rs.getString("status"));
        c.setRole(rs.getString("role"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) c.setCreatedAt(created.toLocalDateTime());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());

        return c;
    };

    // ── CREATE ────────────────────────────────────────────────
    public long save(Customer c) {
        String sql = """
            INSERT INTO customers
              (first_name, last_name, email, password, phone,
               address_line1, address_line2, city, postal_code,
               country, date_of_birth, profile_photo_path, status, role)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,  c.getFirstName());
            ps.setString(2,  c.getLastName());
            ps.setString(3,  c.getEmail());
            ps.setString(4,  c.getPassword());
            ps.setString(5,  c.getPhone());
            ps.setString(6,  c.getAddressLine1());
            ps.setString(7,  c.getAddressLine2());
            ps.setString(8,  c.getCity());
            ps.setString(9,  c.getPostalCode());
            ps.setString(10, c.getCountry());
            ps.setObject(11, c.getDateOfBirth());
            ps.setString(12, c.getProfilePhotoPath());
            ps.setString(13, c.getStatus() != null ? c.getStatus() : "ACTIVE");
            ps.setString(14, c.getRole()   != null ? c.getRole()   : "CUSTOMER");
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    // ── READ ──────────────────────────────────────────────────
    public Optional<Customer> findById(Long id) {
        List<Customer> list = jdbc.query(
                "SELECT * FROM customers WHERE id = ?", customerRowMapper, id);
        return list.stream().findFirst();
    }

    public Optional<Customer> findByEmail(String email) {
        List<Customer> list = jdbc.query(
                "SELECT * FROM customers WHERE email = ?", customerRowMapper, email);
        return list.stream().findFirst();
    }

    public List<Customer> findAllCustomers() {
        return jdbc.query(
                "SELECT * FROM customers WHERE role = 'CUSTOMER' ORDER BY created_at DESC",
                customerRowMapper);
    }

    public boolean emailExists(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    // ── UPDATE ────────────────────────────────────────────────
    public void updateProfile(Customer c) {
        jdbc.update("""
            UPDATE customers SET
              first_name=?, last_name=?, phone=?,
              address_line1=?, address_line2=?, city=?,
              postal_code=?, country=?, date_of_birth=?,
              updated_at=NOW()
            WHERE id=?
            """,
                c.getFirstName(), c.getLastName(), c.getPhone(),
                c.getAddressLine1(), c.getAddressLine2(), c.getCity(),
                c.getPostalCode(), c.getCountry(), c.getDateOfBirth(),
                c.getId());
    }

    public void updateProfilePhoto(Long id, String path) {
        jdbc.update(
                "UPDATE customers SET profile_photo_path=?, updated_at=NOW() WHERE id=?",
                path, id);
    }

    public void updatePassword(Long id, String newPassword) {
        jdbc.update(
                "UPDATE customers SET password=?, updated_at=NOW() WHERE id=?",
                newPassword, id);
    }

    public void updateStatus(Long id, String status) {
        jdbc.update(
                "UPDATE customers SET status=?, updated_at=NOW() WHERE id=?",
                status, id);
    }

    // ── DELETE ────────────────────────────────────────────────
    public void deleteById(Long id) {
        jdbc.update("DELETE FROM customers WHERE id=?", id);
    }
}