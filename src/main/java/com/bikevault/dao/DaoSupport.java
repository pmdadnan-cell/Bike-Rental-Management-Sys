package com.bikevault.dao;

import com.bikevault.database.DatabaseConnection;
import com.bikevault.exception.DatabaseException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Shared JDBC helpers. Subclasses always use {@link PreparedStatement} and try-with-resources.
 */
public abstract class DaoSupport {

    protected Connection open() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    protected static Long readLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    protected static Integer readInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    protected static BigDecimal readDecimal(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getBigDecimal(column);
    }

    protected static LocalDate readDate(ResultSet resultSet, String column) throws SQLException {
        Date value = resultSet.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    protected static LocalDateTime readDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    protected static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    protected static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    protected static Long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new DatabaseException("Insert succeeded but no generated key was returned.");
        }
    }

    protected static int executeUpdate(PreparedStatement statement, String action) {
        try {
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw DatabaseException.from(ex, action);
        }
    }

    protected DatabaseException wrap(SQLException ex, String action) {
        return DatabaseException.from(ex, action);
    }

    protected static int statementGeneratedKeys() {
        return Statement.RETURN_GENERATED_KEYS;
    }
}
