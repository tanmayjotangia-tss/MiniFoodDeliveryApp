package com.fooddeliveryapp.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream is = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (is == null) {
                throw new ExceptionInInitializerError(
                        "db.properties not found on the classpath. " +
                        "Place it under src/main/resources/.");
            }
            CONFIG.load(is);

            Class.forName(CONFIG.getProperty("db.driver", "org.postgresql.Driver"));

        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.username"),
                CONFIG.getProperty("db.password"));
    }
}
