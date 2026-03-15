package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.models.notification.AppNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DBNotificationRepository {

    private static final String INSERT =
            "INSERT INTO notifications (id,user_id,message,created_at,is_read)" +
                    "VALUES (?,?,?,?,?)";

    private static final String MARK_READ =
            "UPDATE notifications SET is_read = TRUE WHERE id = ?";

    private static final String DELETE_BY_ID =
            "DELETE FROM notifications WHERE id = ?";

    private static final String DELETE_ALL_FOR_USER =
            "DELETE FROM notifications WHERE user_id = ?";


    public void insert(String userId, AppNotification notification){
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1,notification.getId());
            ps.setString(2,userId);
            ps.setString(3, notification.getMessage());
            ps.setTimestamp(4, Timestamp.valueOf(notification.getTimestamp()));
            ps.setBoolean(5,notification.isRead());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert notification: " + e.getMessage(),e);
        }
    }
    public void markAsRead(String notificationId) {
        executeUpdate(MARK_READ, notificationId);
    }

    public void delete(String notificationId) {
        executeUpdate(DELETE_BY_ID, notificationId);
    }

    public void deleteAll(String userId) {
        executeUpdate(DELETE_ALL_FOR_USER, userId);
    }

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