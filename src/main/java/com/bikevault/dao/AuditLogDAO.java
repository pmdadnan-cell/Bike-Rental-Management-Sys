package com.bikevault.dao;

import com.bikevault.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only access for {@code audit_logs}.
 */
public class AuditLogDAO extends DaoSupport {

    public void insert(Long userId, String action, String description) {
        try (Connection connection = open()) {
            insert(connection, userId, action, description);
        } catch (SQLException ex) {
            throw wrap(ex, "write audit log");
        }
    }

    public void insert(Connection connection, Long userId, String action, String description) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, description) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setLong(statement, 1, userId);
            statement.setString(2, action);
            statement.setString(3, description);
            statement.executeUpdate();
        }
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = """
                SELECT l.log_id, l.user_id, u.username, l.action, l.description, l.timestamp
                  FROM audit_logs l
                  LEFT JOIN users u ON u.user_id = l.user_id
                 ORDER BY l.timestamp DESC
                 LIMIT ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AuditLog> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(map(resultSet));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list audit logs");
        }
    }

    public List<AuditLog> findAll() {
        return findRecent(500);
    }

    private AuditLog map(ResultSet resultSet) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(resultSet.getLong("log_id"));
        log.setUserId(readLong(resultSet, "user_id"));
        log.setUsername(resultSet.getString("username"));
        log.setAction(resultSet.getString("action"));
        log.setDescription(resultSet.getString("description"));
        log.setTimestamp(readDateTime(resultSet, "timestamp"));
        return log;
    }
}
