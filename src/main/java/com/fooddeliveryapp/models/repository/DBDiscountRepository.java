package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.services.discount.TieredPercentageDiscount;

import java.sql.*;

/**
 * PostgreSQL-backed replacement for {@link FileDiscountRepository}.
 *
 * <p>Stores every slab of the {@link TieredPercentageDiscount} strategy in the
 * {@code discount_slabs} table.  Because there is only ever one discount
 * configuration, the save strategy is a simple <em>delete-all + re-insert</em>
 * inside a transaction — identical in behaviour to the original file approach.</p>
 *
 * <p>The public API ({@code load()} and {@code save()}) is identical to
 * {@link FileDiscountRepository} so no other class needs to change.</p>
 */
public class DBDiscountRepository {

    // ── SQL constants ────────────────────────────────────────────────────────

    private static final String SELECT_ALL_SLABS =
            "SELECT threshold, percentage FROM discount_slabs ORDER BY threshold ASC";

    private static final String DELETE_ALL_SLABS =
            "DELETE FROM discount_slabs";

    private static final String INSERT_SLAB =
            "INSERT INTO discount_slabs (threshold, percentage) VALUES (?,?)";

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Loads all discount slabs from the database and returns a fully populated
     * {@link TieredPercentageDiscount}.  If no slabs are configured, returns an
     * empty (zero-discount) instance — exactly the same behaviour as the old
     * file-based repository.
     */
    public TieredPercentageDiscount load() {
        TieredPercentageDiscount discount = new TieredPercentageDiscount();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SLABS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                double threshold  = rs.getDouble("threshold");
                double percentage = rs.getDouble("percentage");
                discount.addSlab(threshold, percentage);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load discount slabs: " + e.getMessage(), e);
        }

        return discount;
    }

    /**
     * Persists the current state of the discount strategy.
     * All existing slabs are deleted and the current ones are inserted atomically.
     *
     * @param discount the {@link TieredPercentageDiscount} whose slabs to persist
     */
    public void save(TieredPercentageDiscount discount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Wipe existing slabs
                try (PreparedStatement del = conn.prepareStatement(DELETE_ALL_SLABS)) {
                    del.executeUpdate();
                }

                // 2. Re-insert all current slabs
                if (!discount.getSlabs().isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement(INSERT_SLAB)) {
                        for (var entry : discount.getSlabs().entrySet()) {
                            ins.setDouble(1, entry.getKey());   // threshold
                            ins.setDouble(2, entry.getValue()); // percentage
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save discount slabs: " + e.getMessage(), e);
        }
    }
}
