package com.bikevault.database;

import com.bikevault.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton JDBC gateway. Callers must close connections obtained from {@link #getConnection()}
 * unless they use {@link #inTransaction(SqlWork)}.
 */
public final class DatabaseConnection {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile DatabaseConnection instance;

    private final DatabaseConfig config;

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    private DatabaseConnection() {
        this.config = DatabaseConfig.load();
        LOG.info("JDBC target mysql://{}:{}/{}", config.getHost(), config.getPort(), config.getDatabase());
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    /**
     * Opens a new JDBC connection using {@link DriverManager} and configured credentials.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.getUsername(), config.getPassword());
    }

    /**
     * Runs work in a single transaction and rolls back on {@link SQLException}.
     */
    public <T> T inTransaction(SqlWork<T> work) {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            T result = work.execute(connection);
            connection.commit();
            return result;
        } catch (SQLException ex) {
            rollbackQuietly(connection);
            throw DatabaseException.from(ex, "complete a database transaction");
        } catch (RuntimeException ex) {
            rollbackQuietly(connection);
            throw ex;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    LOG.warn("Failed to close transactional connection", closeEx);
                }
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            LOG.error("Rollback failed", rollbackEx);
        }
    }

    /**
     * Lightweight reachability check used by the dashboard status bar. Never throws to the UI thread.
     */
    public boolean isReachable() {
        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException ex) {
            LOG.warn("Database is not reachable at {}:{} / {} — {}",
                    config.getHost(), config.getPort(), config.getDatabase(), ex.getMessage());
            return false;
        }
    }
}
