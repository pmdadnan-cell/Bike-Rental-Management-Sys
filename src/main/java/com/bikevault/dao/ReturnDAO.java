package com.bikevault.dao;

import com.bikevault.model.ReturnRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code returns}.
 */
public class ReturnDAO extends DaoSupport {

    private static final String COLUMNS = """
            rt.return_id, rt.rental_id, rt.bike_id, rt.customer_id, c.full_name AS customer_name,
            CONCAT(b.brand, ' ', b.model) AS bike_label, rt.user_id, rt.return_date,
            rt.actual_duration_days, rt.late_days, rt.late_fee, rt.final_amount,
            rt.condition_notes, rt.created_at
            """;

    private static final String FROM_JOIN = """
            FROM returns rt
            JOIN customers c ON c.customer_id = rt.customer_id
            JOIN bikes b ON b.bike_id = rt.bike_id
            """;

    public Optional<ReturnRecord> findById(long id) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE rt.return_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load return");
        }
    }

    public Optional<ReturnRecord> findByRentalId(long rentalId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE rt.rental_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "find return by rental");
        }
    }

    public List<ReturnRecord> findAll() {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " ORDER BY rt.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ReturnRecord> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(map(resultSet));
            }
            return rows;
        } catch (SQLException ex) {
            throw wrap(ex, "list returns");
        }
    }

    public ReturnRecord insert(ReturnRecord record) {
        try (Connection connection = open()) {
            return insert(connection, record);
        } catch (SQLException ex) {
            throw wrap(ex, "create return");
        }
    }

    public ReturnRecord insert(Connection connection, ReturnRecord record) throws SQLException {
        String sql = """
                INSERT INTO returns (
                    rental_id, bike_id, customer_id, user_id, return_date,
                    actual_duration_days, late_days, late_fee, final_amount, condition_notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            statement.setLong(1, record.getRentalId());
            statement.setLong(2, record.getBikeId());
            statement.setLong(3, record.getCustomerId());
            statement.setLong(4, record.getUserId());
            setDate(statement, 5, record.getReturnDate());
            statement.setInt(6, record.getActualDurationDays());
            statement.setInt(7, record.getLateDays());
            statement.setBigDecimal(8, record.getLateFee());
            statement.setBigDecimal(9, record.getFinalAmount());
            statement.setString(10, record.getConditionNotes());
            statement.executeUpdate();
            record.setId(generatedId(statement));
            return record;
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM returns WHERE return_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete return");
        }
    }

    private ReturnRecord map(ResultSet resultSet) throws SQLException {
        ReturnRecord record = new ReturnRecord();
        record.setId(resultSet.getLong("return_id"));
        record.setRentalId(resultSet.getLong("rental_id"));
        record.setBikeId(resultSet.getLong("bike_id"));
        record.setCustomerId(resultSet.getLong("customer_id"));
        record.setCustomerName(resultSet.getString("customer_name"));
        record.setBikeLabel(resultSet.getString("bike_label"));
        record.setUserId(resultSet.getLong("user_id"));
        record.setReturnDate(readDate(resultSet, "return_date"));
        record.setActualDurationDays(readInteger(resultSet, "actual_duration_days"));
        record.setLateDays(readInteger(resultSet, "late_days"));
        record.setLateFee(readDecimal(resultSet, "late_fee"));
        record.setFinalAmount(readDecimal(resultSet, "final_amount"));
        record.setConditionNotes(resultSet.getString("condition_notes"));
        record.setCreatedAt(readDateTime(resultSet, "created_at"));
        return record;
    }
}
