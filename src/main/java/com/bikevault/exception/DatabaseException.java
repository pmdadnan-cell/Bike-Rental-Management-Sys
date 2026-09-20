package com.bikevault.exception;

import java.sql.SQLException;

/**
 * Checked-to-unchecked bridge for JDBC failures. Controllers should show {@link #getMessage()}
 * and log the cause.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Translates common MySQL error codes into operator-facing messages.
     */
    public static DatabaseException from(SQLException ex, String action) {
        int code = ex.getErrorCode();
        if (code == 1062) {
            return new DatabaseException(
                    "A unique value already exists. Check QR code, registration number, email, phone, license, or payment reference.",
                    ex);
        }
        if (code == 1451) {
            return new DatabaseException(
                    "This record is still referenced by other data and cannot be deleted.",
                    ex);
        }
        if (code == 1452) {
            return new DatabaseException(
                    "Related record was not found. Check the selected customer, bike, user, or category.",
                    ex);
        }
        if (code == 3819) {
            return new DatabaseException(
                    "A database rule was violated. Check dates, amounts, and status values.",
                    ex);
        }
        if (isConnectionFailure(ex)) {
            return new DatabaseException(
                    "Could not connect to MySQL. Confirm Docker is running and the database settings are correct.",
                    ex);
        }
        return new DatabaseException("Database error while attempting to " + action + ".", ex);
    }

    private static boolean isConnectionFailure(SQLException ex) {
        String state = ex.getSQLState();
        if (state != null && (state.startsWith("08") || "08001".equals(state) || "08S01".equals(state))) {
            return true;
        }
        String message = ex.getMessage();
        return message != null && (
                message.contains("Communications link failure")
                        || message.contains("Connection refused")
                        || message.contains("Could not create connection"));
    }
}
