package com.fooddeliveryapp.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies schema.sql to the database on every application startup.
 *
 * Safe to run repeatedly — schema.sql uses CREATE TABLE IF NOT EXISTS
 * and CREATE INDEX IF NOT EXISTS, so existing tables and data are
 * never touched. No DROP statements anywhere.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() { }

    public static void initialize() {
        List<String> statements = loadStatements();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                stmt.execute(sql);
            }

            System.out.println("[DatabaseInitializer] Schema ready (" + statements.size() + " statements).");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Reads schema.sql and splits it into individual statements on semicolons.
     * Comment lines (-- ...) and blank lines are stripped before splitting.
     */
    private static List<String> loadStatements() {
        String raw = readFile();

        // Strip single-line SQL comments so semicolons inside comments don't split wrongly
        String stripped = raw.replaceAll("--[^\n]*", "");

        List<String> statements = new ArrayList<>();
        for (String part : stripped.split(";")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }

    private static String readFile() {
        try (InputStream is = DatabaseInitializer.class
                .getClassLoader()
                .getResourceAsStream("schema.sql")) {

            if (is == null) {
                throw new RuntimeException("schema.sql not found on the classpath. " +
                        "Ensure it is in src/main/resources/.");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not read schema.sql: " + e.getMessage(), e);
        }
    }
}