package com.fooddeliveryapp.repository;

import com.fooddeliveryapp.db.DatabaseConnection;
import com.fooddeliveryapp.services.discount.TieredPercentageDiscount;

import java.sql.*;

public class DBDiscountRepository {

    private static final String SELECT_ALL_SLABS =
            "SELECT threshold, percentage FROM discount_slabs ORDER BY threshold ASC";

    private static final String DELETE_ALL_SLABS =
            "DELETE FROM discount_slabs";

    private static final String INSERT_SLAB =
            "INSERT INTO discount_slabs (threshold, percentage) VALUES (?,?)";

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

    public void save(TieredPercentageDiscount discount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(DELETE_ALL_SLABS)) {
                    del.executeUpdate();
                }

                if (!discount.getSlabs().isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement(INSERT_SLAB)) {
                        for (var entry : discount.getSlabs().entrySet()) {
                            ins.setDouble(1, entry.getKey());   
                            ins.setDouble(2, entry.getValue()); 
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
