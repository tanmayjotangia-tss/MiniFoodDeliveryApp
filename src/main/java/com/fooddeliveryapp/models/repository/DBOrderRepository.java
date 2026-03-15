package com.fooddeliveryapp.models.repository;

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

/**
 * PostgreSQL-backed implementation of {@link Repository} for {@link Order}.
 *
 * <p>Replaces the old {@code FileRepository<Order>} ("orders.dat").
 * All public method signatures are identical to the old implementation.</p>
 *
 * <p>Order items are stored with a <strong>price snapshot</strong>: even if the
 * admin later updates or removes a menu item, the original price is preserved
 * in the {@code order_items.item_price} column.</p>
 *
 * <p>Every multi-step write is wrapped in a transaction so the orders table and
 * the order_items table are always in sync.</p>
 */
public class DBOrderRepository implements Repository<Order> {

    // ── SQL constants ────────────────────────────────────────────────────────

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

    private static final String DELETE_ORDER_ITEMS =
            "DELETE FROM order_items WHERE order_id = ?";

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

    // ── Repository<Order> ────────────────────────────────────────────────────

    /**
     * Inserts or updates the order row and fully replaces its items.
     * Runs inside a single transaction.
     */
    @Override
    public void save(Order order) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                upsertOrder(order, conn);
                syncOrderItems(order, conn);
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
                if (rs.next()) {
                    Order order = mapOrder(rs, conn);
                    return Optional.of(order);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order [" + id + "]: "
                    + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_ORDERS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs, conn));
            }
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
            throw new RuntimeException("Failed to delete order [" + id + "]: "
                    + e.getMessage(), e);
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /** Upserts the core orders row. id / created_at are never overwritten. */
    private void upsertOrder(Order order, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_ORDER)) {
            ps.setString(1, order.getId());
            ps.setString(2, order.getCustomerId());
            ps.setString(3, order.getCustomerName());
            ps.setString(4, order.getStatus().name());

            if (order.getPaymentMode() != null) {
                ps.setString(5, order.getPaymentMode().name());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            if (order.getDeliveryPartnerId() != null) {
                ps.setString(6, order.getDeliveryPartnerId());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            // Store discount; finalAmount is computed in Java from total - discount
            ps.setDouble(7, order.getTotalAmount() - order.getFinalAmount());
            ps.setTimestamp(8, Timestamp.valueOf(order.getCreatedAt()));

            ps.executeUpdate();
        }
    }

    /**
     * Deletes all existing items for this order then re-inserts the current list.
     * Items carry a price snapshot so historical invoices are unaffected by
     * future menu price changes.
     */
    private void syncOrderItems(Order order, Connection conn) throws SQLException {
        // 1. Delete old items
        try (PreparedStatement del = conn.prepareStatement(DELETE_ORDER_ITEMS)) {
            del.setString(1, order.getId());
            del.executeUpdate();
        }

        // 2. Re-insert current items
        if (order.getItems().isEmpty()) return;

        try (PreparedStatement ins = conn.prepareStatement(INSERT_ORDER_ITEM)) {
            for (OrderItem oi : order.getItems()) {
                ins.setString(1, order.getId());
                ins.setString(2, oi.item().getId());   // FK (nullable — item may be deleted later)
                ins.setString(3, oi.item().getName()); // price snapshot: name
                ins.setDouble(4, oi.item().getPrice()); // price snapshot: price
                ins.setInt(5, oi.quantity());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    /**
     * Maps a ResultSet row to an {@link Order}, including loading its items.
     * Uses the price snapshot stored in order_items so reconstruction is
     * independent of the current menu state.
     */
    private Order mapOrder(ResultSet rs, Connection conn) throws SQLException {
        String      id                = rs.getString("id");
        String      customerId        = rs.getString("customer_id");
        String      customerName      = rs.getString("customer_name");
        OrderStatus status            = OrderStatus.valueOf(rs.getString("status"));
        LocalDateTime createdAt       = rs.getTimestamp("created_at").toLocalDateTime();
        double      discount          = rs.getDouble("discount");

        String paymentModeStr         = rs.getString("payment_mode");
        PaymentMode paymentMode       = (paymentModeStr != null)
                ? PaymentMode.valueOf(paymentModeStr) : null;

        String deliveryPartnerId      = rs.getString("delivery_partner_id");

        // Load items from snapshot
        List<OrderItem> items = loadOrderItems(id, conn);

        return new Order(id, customerId, customerName, items, createdAt,
                paymentMode, deliveryPartnerId, status, discount);
    }

    /**
     * Loads the persisted order items, reconstructing each {@link MenuItem}
     * from the stored snapshot (name + price) rather than from the live menu.
     * This preserves the historical view of every order.
     */
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

                    // Rebuild MenuItem purely from snapshot — id may be null if item deleted
                    String safeId = (itemId != null) ? itemId : "deleted-" + itemName;
                    MenuItem snapshot = new MenuItem(safeId, itemName, itemPrice);

                    items.add(new OrderItem(snapshot, quantity));
                }
            }
        }
        return items;
    }
}
