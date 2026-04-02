package com.fooddeliveryapp.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.order.PaymentMode;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DBOrderRepository implements Repository<Order> {
    private static final String UPSERT_ORDER = """
            INSERT INTO orders
                (id, customer_id, customer_name, status, payment_mode,
                 delivery_partner_id, discount, created_at)
            VALUES (?,?,?,?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET
                status              = EXCLUDED.status,
                payment_mode        = EXCLUDED.payment_mode,
                delivery_partner_id = EXCLUDED.delivery_partner_id,
                discount            = EXCLUDED.discount
            """;

    private static final String INSERT_ORDER_ITEM = """
            INSERT INTO order_items
                (order_id, menu_item_id, item_name, item_price, quantity)
            VALUES (?,?,?,?,?)
            """;

    private static final String SELECT_ORDER_BY_ID =
            "SELECT * FROM orders WHERE id = ?";

    private static final String SELECT_ORDER_ITEMS =
            "SELECT * FROM order_items WHERE order_id = ?";

    private static final String SELECT_ALL_ORDERS =
            "SELECT * FROM orders ORDER BY created_at DESC";

    private static final String DELETE_ORDER =
            "DELETE FROM orders WHERE id = ?";

    @Override
    public void save(Order order) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                upsertOrder(order, conn);
                insertItemsOnce(order, conn);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order [" + order.getId() + "]: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ORDER_BY_ID)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapOrder(rs, conn));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order [" + id + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_ORDERS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) orders.add(mapOrder(rs, conn));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all orders: " + e.getMessage(), e);
        }
        return orders;
    }

    @Override
    public void delete(String id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_ORDER)) {

            ps.setString(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete order [" + id + "]: " + e.getMessage(), e);
        }
    }

    private void upsertOrder(Order order, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_ORDER)) {
            ps.setString(1, order.getId());
            ps.setString(2, order.getCustomerId());
            ps.setString(3, order.getCustomerName());
            ps.setString(4, order.getStatus().name());

            if (order.getPaymentMode() != null)
                ps.setString(5, order.getPaymentMode().name());
            else
                ps.setNull(5, Types.VARCHAR);

            if (order.getDeliveryPartnerId() != null)
                ps.setString(6, order.getDeliveryPartnerId());
            else
                ps.setNull(6, Types.VARCHAR);

            ps.setDouble(7, order.getTotalAmount() - order.getFinalAmount());
            ps.setTimestamp(8, Timestamp.valueOf(order.getCreatedAt()));
            ps.executeUpdate();
        }
    }

    private void insertItemsOnce(Order order, Connection conn) throws SQLException {
        if (order.getStatus() != OrderStatus.PAID) return;
        if (order.getItems().isEmpty()) return;

        try (PreparedStatement ins = conn.prepareStatement(INSERT_ORDER_ITEM)) {
            for (OrderItem oi : order.getItems()) {
                ins.setString(1, order.getId());
                ins.setString(2, oi.item().getId());
                ins.setString(3, oi.item().getName());
                ins.setDouble(4, oi.item().getPrice());
                ins.setInt(5, oi.quantity());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    private Order mapOrder(ResultSet rs, Connection conn) throws SQLException {
        String id = rs.getString("id");
        String customerId = rs.getString("customer_id");
        String customerName = rs.getString("customer_name");
        OrderStatus status = OrderStatus.valueOf(rs.getString("status"));
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        double      discount       = rs.getDouble("discount");

        String paymentModeStr      = rs.getString("payment_mode");
        PaymentMode paymentMode    = paymentModeStr != null ? PaymentMode.valueOf(paymentModeStr) : null;

        String deliveryPartnerId   = rs.getString("delivery_partner_id");

        List<OrderItem> items = loadOrderItems(id, conn);

        return new Order(id, customerId, customerName, items, createdAt,
                paymentMode, deliveryPartnerId, status, discount);
    }

    private List<OrderItem> loadOrderItems(String orderId, Connection conn) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ORDER_ITEMS)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemId    = rs.getString("menu_item_id");
                    String itemName  = rs.getString("item_name");
                    double itemPrice = rs.getDouble("item_price");
                    int    quantity  = rs.getInt("quantity");
                    String safeId   = itemId != null ? itemId : "deleted-" + itemName;
                    items.add(new OrderItem(new MenuItem(safeId, itemName, itemPrice), quantity));
                }
            }
        }
        return items;
    }
}