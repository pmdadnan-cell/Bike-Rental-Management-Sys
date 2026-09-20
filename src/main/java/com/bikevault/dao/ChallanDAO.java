package com.bikevault.dao;

import com.bikevault.model.Challan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code challans}.
 */
public class ChallanDAO extends DaoSupport {

    private static final String COLUMNS = """
            ch.challan_id, ch.rental_id, ch.customer_id, c.full_name AS customer_name, ch.bike_id,
            CONCAT(b.brand, ' ', b.model) AS bike_label, ch.challan_date, ch.description, ch.amount,
            ch.status, ch.created_at, ch.updated_at
            """;

    private static final String FROM_JOIN = """
            FROM challans ch
            JOIN customers c ON c.customer_id = ch.customer_id
            LEFT JOIN bikes b ON b.bike_id = ch.bike_id
            """;

    public Optional<Challan> findById(long id) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE ch.challan_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load challan");
        }
    }

    public List<Challan> findAll() {
        return list("SELECT " + COLUMNS + FROM_JOIN + " ORDER BY ch.created_at DESC");
    }

    public List<Challan> findByRentalId(long rentalId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE ch.rental_id = ? ORDER BY ch.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list challans for rental");
        }
    }

    public List<Challan> findByCustomerId(long customerId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE ch.customer_id = ? ORDER BY ch.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list challans for customer");
        }
    }

    public Challan insert(Challan challan) {
        String sql = """
                INSERT INTO challans (rental_id, customer_id, bike_id, challan_date, description, amount, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            setLong(statement, 1, challan.getRentalId());
            statement.setLong(2, challan.getCustomerId());
            setLong(statement, 3, challan.getBikeId());
            setDate(statement, 4, challan.getChallanDate());
            statement.setString(5, challan.getDescription());
            statement.setBigDecimal(6, challan.getAmount());
            statement.setString(7, challan.getStatus());
            statement.executeUpdate();
            challan.setId(generatedId(statement));
            return challan;
        } catch (SQLException ex) {
            throw wrap(ex, "create challan");
        }
    }

    public boolean update(Challan challan) {
        String sql = """
                UPDATE challans
                   SET rental_id = ?, customer_id = ?, bike_id = ?, challan_date = ?,
                       description = ?, amount = ?, status = ?
                 WHERE challan_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setLong(statement, 1, challan.getRentalId());
            statement.setLong(2, challan.getCustomerId());
            setLong(statement, 3, challan.getBikeId());
            setDate(statement, 4, challan.getChallanDate());
            statement.setString(5, challan.getDescription());
            statement.setBigDecimal(6, challan.getAmount());
            statement.setString(7, challan.getStatus());
            statement.setLong(8, challan.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update challan");
        }
    }

    public BigDecimal sumPendingByCustomer(long customerId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM challans WHERE customer_id = ? AND status = 'PENDING'";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "sum pending challans");
        }
    }

    private List<Challan> list(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list challans");
        }
    }

    private List<Challan> mapAll(ResultSet resultSet) throws SQLException {
        List<Challan> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Challan map(ResultSet resultSet) throws SQLException {
        Challan challan = new Challan();
        challan.setId(resultSet.getLong("challan_id"));
        challan.setRentalId(readLong(resultSet, "rental_id"));
        challan.setCustomerId(resultSet.getLong("customer_id"));
        challan.setCustomerName(resultSet.getString("customer_name"));
        challan.setBikeId(readLong(resultSet, "bike_id"));
        challan.setBikeLabel(resultSet.getString("bike_label"));
        challan.setChallanDate(readDate(resultSet, "challan_date"));
        challan.setDescription(resultSet.getString("description"));
        challan.setAmount(readDecimal(resultSet, "amount"));
        challan.setStatus(resultSet.getString("status"));
        challan.setCreatedAt(readDateTime(resultSet, "created_at"));
        challan.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        return challan;
    }
}
