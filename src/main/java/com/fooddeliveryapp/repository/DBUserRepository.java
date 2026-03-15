package com.fooddeliveryapp.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.models.notification.AppNotification;
import com.fooddeliveryapp.models.notification.NotificationType;
import com.fooddeliveryapp.models.users.Admin;
import com.fooddeliveryapp.models.users.Customer;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.users.Role;
import com.fooddeliveryapp.models.users.User;

import java.sql.*;
import java.util.*;

public class DBUserRepository implements UserRepository {


    private static final String SELECT_USER_BASE = """
            SELECT
                u.id, u.name, u.email, u.phone, u.password, u.role,
                c.address,
                c.notification_preferences,
                dp.available,
                dp.basic_pay,
                dp.incentive_percentage
            FROM users u
            LEFT JOIN customers          c  ON c.user_id  = u.id
            LEFT JOIN delivery_partners  dp ON dp.user_id = u.id
            """;

    private static final String SELECT_ALL_USERS =
            SELECT_USER_BASE + " ORDER BY u.name";

    private static final String SELECT_BY_ID =
            SELECT_USER_BASE + " WHERE u.id = ?";

    private static final String SELECT_BY_EMAIL =
            SELECT_USER_BASE + " WHERE LOWER(u.email) = LOWER(?)";

    private static final String UPSERT_USERS = """
            INSERT INTO users (id, name, email, phone, password, role)
            VALUES (?,?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                name     = EXCLUDED.name,
                email    = EXCLUDED.email,
                phone    = EXCLUDED.phone,
                password = EXCLUDED.password,
                role     = EXCLUDED.role
            """;

    private static final String UPSERT_ADMIN = """
            INSERT INTO admins (user_id) VALUES (?)
            ON CONFLICT (user_id) DO NOTHING
            """;

    private static final String UPSERT_CUSTOMER = """
            INSERT INTO customers (user_id, address, notification_preferences)
            VALUES (?,?,?)
            ON CONFLICT (user_id) DO UPDATE SET
                address                  = EXCLUDED.address,
                notification_preferences = EXCLUDED.notification_preferences
            """;

    private static final String UPSERT_DELIVERY_PARTNER = """
            INSERT INTO delivery_partners
                (user_id, available, basic_pay, incentive_percentage)
            VALUES (?,?,?,?)
            ON CONFLICT (user_id) DO UPDATE SET
                available            = EXCLUDED.available,
                basic_pay            = EXCLUDED.basic_pay,
                incentive_percentage = EXCLUDED.incentive_percentage
            """;

    private static final String DELETE_USER =
            "DELETE FROM users WHERE id = ?";

    private static final String DELETE_NOTIFICATIONS =
            "DELETE FROM notifications WHERE user_id = ?";

    private static final String INSERT_NOTIFICATION = """
            INSERT INTO notifications (id, user_id, message, created_at, is_read)
            VALUES (?,?,?,?,?)
            """;

    private static final String SELECT_NOTIFICATIONS = """
            SELECT id, message, created_at, is_read
            FROM notifications
            WHERE user_id = ?
            ORDER BY created_at ASC
            """;

    @Override
    public void save(User user) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                upsertUsersRow(user, conn);
                upsertSubTypeRow(user, conn);
                syncNotifications(user, conn);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user [" + user.getId() + "]: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setString(1, id);
            return executeUserQuery(ps, conn).stream().findFirst();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {

            ps.setString(1, email);
            return executeUserQuery(ps, conn).stream().findFirst();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_USERS)) {

            return executeUserQuery(ps, conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all users: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER)) {

            ps.setString(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user [" + id + "]: "
                    + e.getMessage(), e);
        }
    }

    private void upsertUsersRow(User user, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_USERS)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPassword());
            ps.setString(6, user.getRole().name());
            ps.executeUpdate();
        }
    }

    private void upsertSubTypeRow(User user, Connection conn) throws SQLException {
        switch (user.getRole()) {

            case ADMIN -> {
                try (PreparedStatement ps = conn.prepareStatement(UPSERT_ADMIN)) {
                    ps.setString(1, user.getId());
                    ps.executeUpdate();
                }
            }

            case CUSTOMER -> {
                Customer c = (Customer) user;
                try (PreparedStatement ps = conn.prepareStatement(UPSERT_CUSTOMER)) {
                    ps.setString(1, user.getId());
                    ps.setString(2, c.getAddress());
                    ps.setString(3, serializePreferences(c.getNotificationPreferences()));
                    ps.executeUpdate();
                }
            }

            case DELIVERY_PARTNER -> {
                DeliveryPartner dp = (DeliveryPartner) user;
                try (PreparedStatement ps = conn.prepareStatement(UPSERT_DELIVERY_PARTNER)) {
                    ps.setString(1, user.getId());
                    ps.setBoolean(2, dp.isAvailable());
                    ps.setDouble(3, dp.getBasicPay());
                    ps.setDouble(4, dp.getIncentivePercentage());
                    ps.executeUpdate();
                }
            }
        }
    }

    private void syncNotifications(User user, Connection conn) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(DELETE_NOTIFICATIONS)) {
            del.setString(1, user.getId());
            del.executeUpdate();
        }

        List<AppNotification> notifs = user.getNotifications();
        if (notifs.isEmpty()) return;

        try (PreparedStatement ins = conn.prepareStatement(INSERT_NOTIFICATION)) {
            for (AppNotification n : notifs) {
                ins.setString(1, n.getId());
                ins.setString(2, user.getId());
                ins.setString(3, n.getMessage());
                ins.setTimestamp(4, Timestamp.valueOf(n.getTimestamp()));
                ins.setBoolean(5, n.isRead());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    private List<User> executeUserQuery(PreparedStatement ps,
                                        Connection conn) throws SQLException {
        List<User> users = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = mapRow(rs);
                loadNotifications(user, conn);
                users.add(user);
            }
        }
        return users;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String id       = rs.getString("id");
        String name     = rs.getString("name");
        String email    = rs.getString("email");
        String phone    = rs.getString("phone");      
        String password = rs.getString("password");
        Role   role     = Role.valueOf(rs.getString("role"));

        return switch (role) {

            case ADMIN ->
                new Admin(id, name, email, phone, password);

            case CUSTOMER -> {
                String address = rs.getString("address");
                String prefStr = rs.getString("notification_preferences");
                Set<NotificationType> prefs = deserializePreferences(prefStr);
                yield new Customer(id, name, email, phone, address, password, prefs);
            }

            case DELIVERY_PARTNER -> {
                boolean available    = rs.getBoolean("available");
                double  basicPay     = rs.getDouble("basic_pay");
                double  incentivePct = rs.getDouble("incentive_percentage");
                yield new DeliveryPartner(id, name, email, phone, password,
                        basicPay, available, incentivePct);
            }
        };
    }

    private void loadNotifications(User user, Connection conn) throws SQLException {
        List<AppNotification> notifs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_NOTIFICATIONS)) {
            ps.setString(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifs.add(new AppNotification(
                            rs.getString("id"),
                            rs.getString("message"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("is_read")
                    ));
                }
            }
        }
        user.restoreNotifications(notifs);
    }

    private String serializePreferences(Set<NotificationType> prefs) {
        if (prefs == null || prefs.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(",");
        for (NotificationType t : prefs) sj.add(t.name());
        return sj.toString();
    }

    private Set<NotificationType> deserializePreferences(String raw) {
        Set<NotificationType> prefs = new HashSet<>();
        if (raw == null || raw.isBlank()) return prefs;
        for (String token : raw.split(",")) {
            try {
                prefs.add(NotificationType.valueOf(token.trim()));
            } catch (IllegalArgumentException ignored) { }
        }
        return prefs;
    }
}