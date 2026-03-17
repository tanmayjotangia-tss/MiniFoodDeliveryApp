package com.fooddeliveryapp.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.menu.MenuItem;
import com.fooddeliveryapp.models.users.Customer;

import java.sql.*;
import java.util.*;
public class DBCartRepository implements CartRepository {

    private static final String UPSERT_CART = """
            INSERT INTO carts (id, customer_id)
            VALUES (?,?)
            ON CONFLICT (customer_id) DO UPDATE SET id = EXCLUDED.id
            """;

    private static final String UPSERT_CART_ITEM = """
            INSERT INTO cart_items (cart_id, menu_item_id, quantity)
            VALUES (?,?,?)
            ON CONFLICT (cart_id, menu_item_id) DO UPDATE SET quantity = EXCLUDED.quantity
            """;

    private static final String DELETE_REMOVED_ITEMS_PREFIX =
            "DELETE FROM cart_items WHERE cart_id = ? AND menu_item_id NOT IN (";

    private static final String DELETE_ALL_CART_ITEMS =
            "DELETE FROM cart_items WHERE cart_id = ?";

    private static final String DELETE_ITEM_FROM_ALL_CARTS =
            "DELETE FROM cart_items WHERE menu_item_id = ?";

    private static final String SELECT_CART_BY_CUSTOMER =
            "SELECT id, customer_id FROM carts WHERE customer_id = ?";

    private static final String SELECT_ALL_CARTS =
            "SELECT id, customer_id FROM carts";

    private static final String SELECT_CART_ITEMS = """
            SELECT ci.menu_item_id, ci.quantity,
                   mi.name  AS item_name,
                   mi.price AS item_price
            FROM cart_items ci
            JOIN menu_items mi ON ci.menu_item_id = mi.id
            WHERE ci.cart_id = ?
            """;

    private final UserRepository userRepository;

    public DBCartRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void save(Cart cart) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                upsertCart(cart, conn);
                syncCartItems(cart, conn);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save cart for customer ["
                    + cart.getCustomer().getId() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Cart> findByCustomerId(String customerId) {
        String cartId;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CART_BY_CUSTOMER)) {

            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                cartId = rs.getString("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find cart for customer ["
                    + customerId + "]: " + e.getMessage(), e);
        }

        Customer customer = userRepository.findById(customerId)
                .filter(u -> u instanceof Customer)
                .map(u -> (Customer) u)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found for cart: " + customerId));

        List<CartItem> items = loadCartItems(cartId);

        return Optional.of(new Cart(cartId, customer, items));
    }

    @Override
    public List<Cart> findAll() {
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_CARTS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new String[]{ rs.getString("id"), rs.getString("customer_id") });
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all carts: " + e.getMessage(), e);
        }

        List<Cart> carts = new ArrayList<>();
        for (String[] row : rows) {
            String cartId     = row[0];
            String customerId = row[1];

            userRepository.findById(customerId)
                    .filter(u -> u instanceof Customer)
                    .map(u -> (Customer) u)
                    .ifPresent(customer -> {
                        List<CartItem> items = loadCartItems(cartId);
                        carts.add(new Cart(cartId, customer, items));
                    });
        }
        return carts;
    }

    @Override
    public void removeItemFromAllCarts(String itemId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_ITEM_FROM_ALL_CARTS)) {

            ps.setString(1, itemId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove item [" + itemId
                    + "] from carts: " + e.getMessage(), e);
        }
    }

    private void upsertCart(Cart cart, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_CART)) {
            ps.setString(1, cart.getId());
            ps.setString(2, cart.getCustomer().getId());
            ps.executeUpdate();
        }
    }

    private void syncCartItems(Cart cart, Connection conn) throws SQLException {

        List<CartItem> currentItems = cart.getItems();

        if (currentItems.isEmpty()) {
            try (PreparedStatement del = conn.prepareStatement(DELETE_ALL_CART_ITEMS)) {
                del.setString(1, cart.getId());
                del.executeUpdate();
            }
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(UPSERT_CART_ITEM)) {
            for (CartItem ci : currentItems) {
                ps.setString(1, cart.getId());
                ps.setString(2, ci.getItem().getId());
                ps.setInt(3, ci.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        Set<String> liveIds = new HashSet<>();
        for (CartItem ci : currentItems) liveIds.add(ci.getItem().getId());

        String placeholders = String.join(",", Collections.nCopies(liveIds.size(), "?"));
        String deleteSql = DELETE_REMOVED_ITEMS_PREFIX + placeholders + ")";

        try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
            del.setString(1, cart.getId());
            int i = 2;
            for (String id : liveIds) del.setString(i++, id);
            del.executeUpdate();
        }
    }

    private List<CartItem> loadCartItems(String cartId) {
        List<CartItem> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CART_ITEMS)) {

            ps.setString(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                            new MenuItem(
                                    rs.getString("menu_item_id"),
                                    rs.getString("item_name"),
                                    rs.getDouble("item_price")),
                            rs.getInt("quantity")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load items for cart ["
                    + cartId + "]: " + e.getMessage(), e);
        }
        return items;
    }
}