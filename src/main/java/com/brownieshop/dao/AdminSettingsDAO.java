package com.brownieshop.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persists admin settings in the database so they survive logout/login.
 *
 * Uses a simple key-value table: admin_settings(setting_key, setting_value)
 *
 * Run this SQL once in MySQL to create the table:
 * ─────────────────────────────────────────────────────────────────
 * CREATE TABLE IF NOT EXISTS admin_settings (
 *     setting_key   VARCHAR(100) PRIMARY KEY,
 *     setting_value VARCHAR(500) NOT NULL DEFAULT ''
 * );
 * INSERT IGNORE INTO admin_settings (setting_key, setting_value)
 * VALUES ('notif_read_at_id', '0');
 * ─────────────────────────────────────────────────────────────────
 *
 * WHY NOT SESSION:
 *   HttpSession is destroyed on logout. Storing the notification
 *   watermark in session means it resets to 0 every login, making
 *   all old pending orders appear as "new" again.
 *   Storing in DB persists across login/logout correctly.
 */
@Repository
public class AdminSettingsDAO {

    @Autowired
    private JdbcTemplate jdbc;

    private static final String KEY_NOTIF_READ_AT = "notif_read_at_id";

    /**
     * Ensures the admin_settings table and default row exist.
     * Called once at startup by AdminOrderController via @PostConstruct
     * or lazily on first read.
     * Uses CREATE TABLE IF NOT EXISTS so it's safe to call multiple times.
     */
    public void ensureTableExists() {
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS admin_settings (" +
                        "  setting_key   VARCHAR(100) PRIMARY KEY," +
                        "  setting_value VARCHAR(500) NOT NULL DEFAULT ''" +
                        ")"
        );
        jdbc.execute(
                "INSERT IGNORE INTO admin_settings (setting_key, setting_value) " +
                        "VALUES ('" + KEY_NOTIF_READ_AT + "', '0')"
        );
    }

    /**
     * Returns the highest order ID that the admin has already read
     * (i.e. clicked "Mark all as read" at that point).
     * Orders with ID > this value are considered "new / unread".
     */
    public long getNotifReadAtId() {
        try {
            String val = jdbc.queryForObject(
                    "SELECT setting_value FROM admin_settings WHERE setting_key = ?",
                    String.class, KEY_NOTIF_READ_AT);
            return val != null ? Long.parseLong(val.trim()) : 0L;
        } catch (Exception e) {
            // Table may not exist yet on very first run
            return 0L;
        }
    }

    /**
     * Saves the watermark — the max order ID seen at the moment
     * admin clicked "Mark all as read".
     */
    public void setNotifReadAtId(long id) {
        try {
            jdbc.update(
                    "UPDATE admin_settings SET setting_value = ? WHERE setting_key = ?",
                    String.valueOf(id), KEY_NOTIF_READ_AT);
        } catch (Exception e) {
            // Silently fail — notification is cosmetic
        }
    }
}