package com.bikevault.database;

import com.bikevault.util.BikeImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Additive live-schema updates. Never drops tables or existing rental data.
 */
public final class SchemaMigrator {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaMigrator.class);

    private SchemaMigrator() {
    }

    public static void ensure() {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            ensureImagePath(connection);
            populateImagePaths(connection);
            ensureReturnRequestedStatus(connection);
        } catch (SQLException ex) {
            LOG.warn("Schema migration skipped: {}", ex.getMessage());
        }
    }

    private static void ensureImagePath(Connection connection) throws SQLException {
        if (columnExists(connection, "bikes", "image_path")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE bikes ADD COLUMN image_path VARCHAR(500) NULL");
            LOG.info("Added bikes.image_path");
        }
    }

    private static void populateImagePaths(Connection connection) throws SQLException {
        String select = "SELECT bike_id, brand, model, image_path FROM bikes";
        String update = "UPDATE bikes SET image_path = ? WHERE bike_id = ?";
        try (PreparedStatement query = connection.prepareStatement(select);
             PreparedStatement writer = connection.prepareStatement(update);
             ResultSet rows = query.executeQuery()) {
            int updated = 0;
            while (rows.next()) {
                String current = rows.getString("image_path");
                if (current != null && !current.isBlank()) {
                    continue;
                }
                writer.setString(1, BikeImageLoader.pathFor(rows.getString("brand"), rows.getString("model")));
                writer.setLong(2, rows.getLong("bike_id"));
                writer.addBatch();
                updated++;
            }
            if (updated > 0) {
                writer.executeBatch();
                LOG.info("Mapped image_path for {} bikes", updated);
            }
        }
    }

    private static void ensureReturnRequestedStatus(Connection connection) throws SQLException {
        String type = columnType(connection, "rentals", "status");
        if (type == null) {
            return;
        }
        if (!type.toUpperCase().contains("RETURN_REQUESTED")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        ALTER TABLE rentals
                        MODIFY COLUMN status ENUM(
                            'PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED',
                            'COMPLETED', 'CANCELLED', 'REJECTED'
                        ) NOT NULL DEFAULT 'PENDING_APPROVAL'
                        """);
                LOG.info("Added RETURN_REQUESTED to rentals.status");
            }
        }
        try (Statement statement = connection.createStatement()) {
            int moved = statement.executeUpdate("""
                    UPDATE rentals
                       SET status = 'RETURN_REQUESTED'
                     WHERE return_requested = 1
                       AND status = 'ACTIVE'
                    """);
            if (moved > 0) {
                LOG.info("Moved {} active rentals to RETURN_REQUESTED", moved);
            }
        }
    }

    private static String columnType(Connection connection, String table, String column) throws SQLException {
        String sql = """
                SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                   AND COLUMN_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                   AND COLUMN_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }
}
