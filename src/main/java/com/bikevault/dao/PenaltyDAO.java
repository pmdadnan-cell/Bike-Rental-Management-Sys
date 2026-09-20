package com.bikevault.dao;

import com.bikevault.model.Penalty;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code penalties}.
 */
public class PenaltyDAO extends DaoSupport {

    private static final String COLUMNS = """
            p.penalty_id, p.rental_id, p.customer_id, c.full_name AS customer_name, p.bike_id,
            CONCAT(b.brand, ' ', b.model) AS bike_label, p.penalty_type, p.description, p.amount,
            p.status, p.created_at, p.resolved_at
            """;

    private static final String FROM_JOIN = """
            FROM penalties p
            JOIN customers c ON c.customer_id = p.customer_id
            LEFT JOIN bikes b ON b.bike_id = p.bike_id
            """;

    public Optional<Penalty> findById(long id) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE p.penalty_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load penalty");
        }
    }

    public List<Penalty> findAll() {
        return list("SELECT " + COLUMNS + FROM_JOIN + " ORDER BY p.created_at DESC");
    }

    public List<Penalty> findByRentalId(long rentalId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE p.rental_id = ? ORDER BY p.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list penalties for rental");
        }
    }

    public List<Penalty> findByCustomerId(long customerId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE p.customer_id = ? ORDER BY p.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list penalties for customer");
        }
    }

    public Penalty insert(Penalty penalty) {
        try (Connection connection = open()) {
            return insert(connection, penalty);
        } catch (SQLException ex) {
            throw wrap(ex, "create penalty");
        }
    }

    public Penalty insert(Connection connection, Penalty penalty) throws SQLException {
        String sql = """
                INSERT INTO penalties (rental_id, customer_id, bike_id, penalty_type, description, amount, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            setLong(statement, 1, penalty.getRentalId());
            statement.setLong(2, penalty.getCustomerId());
            setLong(statement, 3, penalty.getBikeId());
            statement.setString(4, penalty.getPenaltyType());
            statement.setString(5, penalty.getDescription());
            statement.setBigDecimal(6, penalty.getAmount());
            statement.setString(7, penalty.getStatus());
            statement.executeUpdate();
            penalty.setId(generatedId(statement));
            return penalty;
        }
    }

    public boolean update(Penalty penalty) {
        try (Connection connection = open()) {
            return update(connection, penalty);
        } catch (SQLException ex) {
            throw wrap(ex, "update penalty");
        }
    }

    public boolean update(Connection connection, Penalty penalty) throws SQLException {
        String sql = """
                UPDATE penalties
                   SET rental_id = ?, customer_id = ?, bike_id = ?, penalty_type = ?, description = ?,
                       amount = ?, status = ?, resolved_at = ?
                 WHERE penalty_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setLong(statement, 1, penalty.getRentalId());
            statement.setLong(2, penalty.getCustomerId());
            setLong(statement, 3, penalty.getBikeId());
            statement.setString(4, penalty.getPenaltyType());
            statement.setString(5, penalty.getDescription());
            statement.setBigDecimal(6, penalty.getAmount());
            statement.setString(7, penalty.getStatus());
            if (penalty.getResolvedAt() == null) {
                statement.setTimestamp(8, null);
            } else {
                statement.setTimestamp(8, Timestamp.valueOf(penalty.getResolvedAt()));
            }
            statement.setLong(9, penalty.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public long countPending() {
        return count("SELECT COUNT(*) FROM penalties WHERE status = 'PENDING'");
    }

    public BigDecimal sumPendingByCustomer(long customerId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM penalties WHERE customer_id = ? AND status = 'PENDING'";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "sum pending penalties");
        }
    }

    public long countPendingByCustomer(long customerId) {
        String sql = "SELECT COUNT(*) FROM penalties WHERE customer_id = ? AND status = 'PENDING'";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count pending penalties");
        }
    }

    private long count(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw wrap(ex, "count penalties");
        }
    }

    private List<Penalty> list(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list penalties");
        }
    }

    private List<Penalty> mapAll(ResultSet resultSet) throws SQLException {
        List<Penalty> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Penalty map(ResultSet resultSet) throws SQLException {
        Penalty penalty = new Penalty();
        penalty.setId(resultSet.getLong("penalty_id"));
        penalty.setRentalId(readLong(resultSet, "rental_id"));
        penalty.setCustomerId(resultSet.getLong("customer_id"));
        penalty.setCustomerName(resultSet.getString("customer_name"));
        penalty.setBikeId(readLong(resultSet, "bike_id"));
        penalty.setBikeLabel(resultSet.getString("bike_label"));
        penalty.setPenaltyType(resultSet.getString("penalty_type"));
        penalty.setDescription(resultSet.getString("description"));
        penalty.setAmount(readDecimal(resultSet, "amount"));
        penalty.setStatus(resultSet.getString("status"));
        penalty.setCreatedAt(readDateTime(resultSet, "created_at"));
        penalty.setResolvedAt(readDateTime(resultSet, "resolved_at"));
        return penalty;
    }
}
