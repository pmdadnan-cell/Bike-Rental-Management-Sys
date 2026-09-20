package com.bikevault.dao;

import com.bikevault.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code payments}.
 */
public class PaymentDAO extends DaoSupport {

    private static final String COLUMNS = """
            p.payment_id, p.rental_id, p.user_id, p.amount, p.method, p.status, p.reference_number,
            p.paid_at, p.notes, p.created_at, c.full_name AS customer_name,
            CONCAT(b.brand, ' ', b.model) AS bike_label
            """;

    private static final String FROM_JOIN = """
            FROM payments p
            JOIN rentals r ON r.rental_id = p.rental_id
            JOIN customers c ON c.customer_id = r.customer_id
            JOIN bikes b ON b.bike_id = r.bike_id
            """;

    public Optional<Payment> findById(long id) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE p.payment_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load payment");
        }
    }

    public List<Payment> findByRentalId(long rentalId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE p.rental_id = ? ORDER BY p.paid_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list payments for rental");
        }
    }

    public List<Payment> findAll() {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " ORDER BY p.paid_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list payments");
        }
    }

    public List<Payment> findByCustomerId(long customerId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE r.customer_id = ? ORDER BY p.paid_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list payments for customer");
        }
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM payments WHERE status = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count payments by status");
        }
    }

    public long countPending() {
        String sql = "SELECT COUNT(*) FROM payments WHERE status = 'PENDING'";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw wrap(ex, "count pending payments");
        }
    }

    public Payment insert(Payment payment) {
        try (Connection connection = open()) {
            return insert(connection, payment);
        } catch (SQLException ex) {
            throw wrap(ex, "create payment");
        }
    }

    public Payment insert(Connection connection, Payment payment) throws SQLException {
        String sql = """
                INSERT INTO payments (rental_id, user_id, amount, method, status, reference_number, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            statement.setLong(1, payment.getRentalId());
            statement.setLong(2, payment.getUserId());
            statement.setBigDecimal(3, payment.getAmount());
            statement.setString(4, payment.getMethod());
            statement.setString(5, payment.getStatus());
            statement.setString(6, payment.getReferenceNumber());
            statement.setString(7, payment.getNotes());
            statement.executeUpdate();
            payment.setId(generatedId(statement));
            return payment;
        }
    }

    public boolean update(Payment payment) {
        String sql = """
                UPDATE payments
                   SET amount = ?, method = ?, status = ?, reference_number = ?, notes = ?
                 WHERE payment_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, payment.getAmount());
            statement.setString(2, payment.getMethod());
            statement.setString(3, payment.getStatus());
            statement.setString(4, payment.getReferenceNumber());
            statement.setString(5, payment.getNotes());
            statement.setLong(6, payment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update payment");
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM payments WHERE payment_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete payment");
        }
    }

    public BigDecimal sumCompletedByRental(long rentalId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                  FROM payments
                 WHERE rental_id = ?
                   AND status = 'COMPLETED'
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "sum payments for rental");
        }
    }

    public BigDecimal getTotalSpentByCustomer(long customerId) {
        String sql = """
                SELECT COALESCE(SUM(p.amount), 0)
                  FROM payments p
                  JOIN rentals r ON r.rental_id = p.rental_id
                 WHERE r.customer_id = ?
                   AND p.status = 'COMPLETED'
                   AND r.status <> 'REJECTED'
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "sum customer spend");
        }
    }

    public BigDecimal sumCompleted() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'COMPLETED'";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException ex) {
            throw wrap(ex, "sum payments");
        }
    }

    public BigDecimal sumCompletedOn(LocalDate date) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                  FROM payments
                 WHERE status = 'COMPLETED'
                   AND DATE(paid_at) = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, date);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "sum daily payments");
        }
    }

    private List<Payment> mapAll(ResultSet resultSet) throws SQLException {
        List<Payment> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Payment map(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment();
        payment.setId(resultSet.getLong("payment_id"));
        payment.setRentalId(resultSet.getLong("rental_id"));
        payment.setUserId(resultSet.getLong("user_id"));
        payment.setAmount(readDecimal(resultSet, "amount"));
        payment.setMethod(resultSet.getString("method"));
        payment.setStatus(resultSet.getString("status"));
        payment.setReferenceNumber(resultSet.getString("reference_number"));
        payment.setPaidAt(readDateTime(resultSet, "paid_at"));
        payment.setNotes(resultSet.getString("notes"));
        payment.setCreatedAt(readDateTime(resultSet, "created_at"));
        payment.setCustomerName(resultSet.getString("customer_name"));
        payment.setBikeLabel(resultSet.getString("bike_label"));
        return payment;
    }
}
