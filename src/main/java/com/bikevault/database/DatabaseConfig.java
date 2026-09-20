package com.bikevault.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads JDBC settings from environment variables first, then {@code application.properties}.
 * Production credentials must never be hard-coded in source.
 */
public final class DatabaseConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String PROPERTIES_FILE = "/application.properties";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String params;

    private DatabaseConfig(String host, int port, String database, String username, String password, String params) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.params = params;
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                LOG.warn("application.properties was not found on the classpath");
            }
        } catch (IOException ex) {
            LOG.error("Unable to read application.properties", ex);
        }

        String host = first(System.getenv("DB_HOST"), properties.getProperty("db.host"), "localhost");
        int port = parsePort(first(System.getenv("DB_PORT"), properties.getProperty("db.port"), "3307"));
        String database = first(System.getenv("DB_NAME"), properties.getProperty("db.name"), "bikevault");
        String username = first(System.getenv("DB_USER"), properties.getProperty("db.user"), "bikevault");
        String password = first(System.getenv("DB_PASSWORD"), properties.getProperty("db.password"), "bikevault");
        String params = first(null, properties.getProperty("db.params"),
                "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        return new DatabaseConfig(host, port, database, username, password, params);
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + params;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    private static String first(String envValue, String propertyValue, String fallback) {
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return fallback;
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            LOG.warn("Invalid database port '{}', falling back to 3307", value);
            return 3307;
        }
    }
}
