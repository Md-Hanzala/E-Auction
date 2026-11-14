package com.eauction.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connectivity for the HASHbid application.
 * Connects to Oracle Database via JDBC thin driver.
 * Credentials are loaded dynamically from 'db.properties' or environment variables,
 * ensuring no sensitive credentials remain in tracked source code.
 */
public class DBConnection {

    private static final String DEFAULT_URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String DEFAULT_USER = "HASHBID";
    private static final String PROPERTIES_FILE = "db.properties";

    private static String url = DEFAULT_URL;
    private static String user = DEFAULT_USER;
    private static String password = null;

    static {
        // Register Oracle JDBC Driver
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("[HASHbid DBConnection] Oracle JDBC Driver not found in classpath!");
            e.printStackTrace();
        }

        loadConfiguration();
    }

    /**
     * Loads database connection settings in priority order:
     * 1. Application ClassLoader classpath (src/main/resources or WEB-INF/classes)
     * 2. Working directory / filesystem config file
     * 3. Environment variables / System properties fallback
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();
        boolean loaded = false;

        // 1. Try loading from thread context ClassLoader (standard for web containers like Tomcat)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            try (InputStream in = cl.getResourceAsStream(PROPERTIES_FILE)) {
                if (in != null) {
                    props.load(in);
                    loaded = true;
                }
            } catch (IOException ignored) {}
        }

        // 2. Try loading from class ClassLoader
        if (!loaded) {
            try (InputStream in = DBConnection.class.getResourceAsStream("/" + PROPERTIES_FILE)) {
                if (in != null) {
                    props.load(in);
                    loaded = true;
                }
            } catch (IOException ignored) {}
        }

        // 3. Try loading directly from filesystem (useful for local development / test runners)
        if (!loaded) {
            File localFile = new File(PROPERTIES_FILE);
            if (!localFile.exists()) {
                localFile = new File("src/main/resources/" + PROPERTIES_FILE);
            }
            if (localFile.exists() && localFile.isFile()) {
                try (InputStream in = new FileInputStream(localFile)) {
                    props.load(in);
                    loaded = true;
                } catch (IOException ignored) {}
            }
        }

        // Apply properties if file was located
        if (loaded) {
            String propUrl = props.getProperty("db.url");
            if (propUrl != null && !propUrl.trim().isEmpty()) {
                url = propUrl.trim();
            }

            String propUser = props.getProperty("db.username");
            if (propUser == null || propUser.trim().isEmpty()) {
                propUser = props.getProperty("db.user");
            }
            if (propUser != null && !propUser.trim().isEmpty()) {
                user = propUser.trim();
            }

            String propPass = props.getProperty("db.password");
            if (propPass != null && !propPass.trim().isEmpty()) {
                password = propPass.trim();
            }
        }

        // 4. Fallback to environment variables / system properties
        String envUrl = System.getenv("HASHBID_DB_URL");
        if (envUrl == null) envUrl = System.getProperty("db.url");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            url = envUrl.trim();
        }

        String envUser = System.getenv("HASHBID_DB_USER");
        if (envUser == null) envUser = System.getProperty("db.username");
        if (envUser != null && !envUser.trim().isEmpty()) {
            user = envUser.trim();
        }

        String envPass = System.getenv("HASHBID_DB_PASSWORD");
        if (envPass == null) envPass = System.getProperty("db.password");
        if (envPass != null && !envPass.trim().isEmpty()) {
            password = envPass.trim();
        }
    }

    /**
     * Obtains a live JDBC connection to Oracle Database.
     * Throws an explanatory SQLException if database credentials have not been configured.
     */
    public static Connection getConnection() throws SQLException {
        if (password == null || password.trim().isEmpty()) {
            // Re-attempt loading in case db.properties was added at runtime
            loadConfiguration();
        }

        if (password == null || password.trim().isEmpty()) {
            throw new SQLException(
                "Database password is not configured. Please create a 'db.properties' file " +
                "(using 'db.properties.example' as a template) in src/main/resources/ or the project root, " +
                "or specify 'db.password' via environment variable HASHBID_DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(url, user, password);
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }

    public static boolean isPasswordConfigured() {
        return password != null && !password.trim().isEmpty();
    }

    // Quick test runner to verify Java can reach Oracle 10g XE
    public static void main(String[] args) {
        System.out.println("Testing Oracle 10g XE connection...");
        System.out.println("Configured URL  : " + url);
        System.out.println("Configured User : " + user);
        System.out.println("Password loaded : " + (isPasswordConfigured() ? "YES" : "NO"));

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("SUCCESS: Connected to Oracle Database successfully!");
            }
        } catch (SQLException e) {
            System.err.println("FAILURE: Unable to connect to Oracle Database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
