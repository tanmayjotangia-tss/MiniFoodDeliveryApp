package com.fooddeliveryapp.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.services.discount.TieredPercentageDiscount;

import java.sql.*;
import java.util.Collections;
import java.util.Map;

public class DBDiscountRepository {

    private static final String UPSERT_SLAB = """
            INSERT INTO discount_slabs (threshold, percentage)
            VALUES (?,?)
            ON CONFLICT (threshold) DO UPDATE SET percentage = EXCLUDED.percentage
            """;

    private static final String DELETE_REMOVED_SLABS_PREFIX =
            "DELETE FROM discount_slabs WHERE threshold NOT IN (";

    private static final String DELETE_ALL_SLABS =
            "DELETE FROM discount_slabs";

    private static final String SELECT_ALL_SLABS =
            "SELECT threshold, percentage FROM discount_slabs ORDER BY threshold ASC";

    public TieredPercentageDiscount load() {
        TieredPercentageDiscount discount = new TieredPercentageDiscount();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SLABS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                discount.addSlab(rs.getDouble("threshold"), rs.getDouble("percentage"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load discount slabs: " + e.getMessage(), e);
        }
        return discount;
    }

    public void save(TieredPercentageDiscount discount) {
        Map<Double, Double> slabs = discount.getSlabs();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (slabs.isEmpty()) {
                    try (PreparedStatement del = conn.prepareStatement(DELETE_ALL_SLABS)) {
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(UPSERT_SLAB)) {
                        for (Map.Entry<Double, Double> entry : slabs.entrySet()) {
                            ps.setDouble(1, entry.getKey());   // threshold
                            ps.setDouble(2, entry.getValue()); // percentage
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    String placeholders = String.join(",",
                            Collections.nCopies(slabs.size(), "?"));
                    String deleteSql = DELETE_REMOVED_SLABS_PREFIX + placeholders + ")";

                    try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                        int i = 1;
                        for (Double threshold : slabs.keySet()) {
                            del.setDouble(i++, threshold);
                        }
                        del.executeUpdate();
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