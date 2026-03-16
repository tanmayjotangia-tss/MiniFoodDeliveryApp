package com.fooddeliveryapp.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            try (Statement stmt = conn.createStatement()) {

                InputStream is = DatabaseInitializer.class
                        .getClassLoader()
                        .getResourceAsStream("schema.sql");

                if (is == null) {
                    System.out.println("schema.sql not found on classpath.");
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                    StringBuilder sql = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        sql.append(line);
                    }

                    String[] queries = sql.toString().split(";");

                    for (String query : queries) {
                        if (!query.trim().isEmpty()) {
                            stmt.execute(query);
                        }
                    }
                }
            }

            System.out.println("Database schema initialized successfully.");

        } catch (Exception e) {
            System.out.println("Database initialization failed.");
            e.printStackTrace();
        }
    }
}