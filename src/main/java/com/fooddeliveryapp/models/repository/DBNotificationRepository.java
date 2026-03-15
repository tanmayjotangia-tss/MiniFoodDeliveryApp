package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Handles targeted, single-row notification mutations directly against
 * the {@code notifications} table.
 *
 * <h3>Why a separate class instead of using UserRepository.save()?</h3>
 * {@code userRepository.save(user)} persists the entire user AND all
 * notifications via a delete-all + reinsert cycle inside one transaction.
 * If the user upsert step (a different table, different constraint) fails or
 * rolls back for any reason, the notification change is silently lost — which
 * is exactly why deletes were not surviving restart while mark-as-read was.
 *
 * Each method here issues a single, targeted SQL statement.  There is no
 * surrounding user state to go wrong.
 */
public class DBNotificationRepository {

    // ── SQL ──────────────────────────────────────────────────────────────────

    private static final String MARK_READ =
            "UPDATE notifications SET is_read = TRUE WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM notifications WHERE id = ?";

    private static final String DELETE_ALL_FOR_USER =
            "DELETE FROM notifications WHERE user_id = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read in the database.
     *
     * @param notificationId the UUID of the notification row
     */
    public void markAsRead(String notificationId) {
        executeUpdate(MARK_READ, notificationId);
    }

    /**
     * Permanently deletes a single notification row.
     *
     * @param notificationId the UUID of the notification row
     */
    public void delete(String notificationId) {
        executeUpdate(DELETE_BY_ID, notificationId);
    }

    /**
     * Deletes every notification belonging to a user.
     *
     * @param userId the UUID of the user whose notifications to clear
     */
    public void deleteAll(String userId) {
        executeUpdate(DELETE_ALL_FOR_USER, userId);
    }

    // ── private helper ───────────────────────────────────────────────────────

    private void executeUpdate(String sql, String param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Notification DB operation failed: " + e.getMessage(), e);
        }
    }
}