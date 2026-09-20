package com.bikevault.dao;

import com.bikevault.model.Rental;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code rentals}.
 */
public class RentalDAO extends DaoSupport {

    private static final String COLUMNS = """
            r.rental_id, r.customer_id, c.full_name AS customer_name, r.bike_id,
            CONCAT(b.brand, ' ', b.model) AS bike_label, b.qr_code AS bike_qr_code,
            b.registration_number, r.user_id, r.start_date, r.expected_return_date,
            r.actual_return_date, r.duration_days, r.daily_rate, r.total_amount, r.late_fee,
            r.security_deposit, r.final_amount, r.status, r.rental_qr_token, r.rental_qr_status,
            r.approved_by, au.full_name AS approved_by_name, r.approved_at, r.rejection_reason,
            r.rejection_notes, r.return_requested, r.return_requested_at, r.notes, r.created_at, r.updated_at
            """;

    private static final String FROM_JOIN = """
            FROM rentals r
            JOIN customers c ON c.customer_id = r.customer_id
            JOIN bikes b ON b.bike_id = r.bike_id
            LEFT JOIN users au ON au.user_id = r.approved_by
            """;

    public Optional<Rental> findById(long id) {
        try (Connection connection = open()) {
            return findById(connection, id, false);
        } catch (SQLException ex) {
            throw wrap(ex, "load rental");
        }
    }

    public Optional<Rental> findById(Connection connection, long id, boolean lockRow) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE r.rental_id = ?"
                + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<Rental> findActiveByBikeId(long bikeId) {
        try (Connection connection = open()) {
            return findActiveByBikeId(connection, bikeId, false);
        } catch (SQLException ex) {
            throw wrap(ex, "find active rental by bike");
        }
    }

    public Optional<Rental> findActiveByBikeId(Connection connection, long bikeId, boolean lockRow) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM_JOIN
                + " WHERE r.bike_id = ? AND r.status IN ('ACTIVE', 'RETURN_REQUESTED') LIMIT 1"
                + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bikeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Rental> findAll() {
        return list("SELECT " + COLUMNS + FROM_JOIN + " ORDER BY r.created_at DESC");
    }

    public List<Rental> findByCustomerId(long customerId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE r.customer_id = ? ORDER BY r.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list rentals for customer");
        }
    }

    public Optional<Rental> findActiveByCustomerId(long customerId) {
        try (Connection connection = open()) {
            return findActiveByCustomerId(connection, customerId, false);
        } catch (SQLException ex) {
            throw wrap(ex, "find active rental by customer");
        }
    }

    public Optional<Rental> findActiveByCustomerId(Connection connection, long customerId, boolean lockRow)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM_JOIN
                + " WHERE r.customer_id = ? AND r.status IN ('ACTIVE', 'RETURN_REQUESTED') LIMIT 1"
                + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public long countByCustomerId(long customerId) {
        String sql = "SELECT COUNT(*) FROM rentals WHERE customer_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count rentals for customer");
        }
    }

    public long countActiveByCustomerId(long customerId) {
        String sql = "SELECT COUNT(*) FROM rentals WHERE customer_id = ? AND status IN ('ACTIVE', 'RETURN_REQUESTED')";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count active rentals for customer");
        }
    }

    public List<Rental> findByStatus(String status) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE r.status = ? ORDER BY r.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list rentals by status");
        }
    }

    public List<Rental> search(String term) {
        String sql = """
                SELECT %s %s
                 WHERE CAST(r.rental_id AS CHAR) LIKE ?
                    OR c.full_name LIKE ?
                    OR b.model LIKE ?
                    OR b.brand LIKE ?
                    OR b.registration_number LIKE ?
                    OR b.qr_code LIKE ?
                 ORDER BY r.created_at DESC
                """.formatted(COLUMNS, FROM_JOIN);
        String like = "%" + (term == null ? "" : term.trim()) + "%";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i++) {
                statement.setString(i, like);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "search rentals");
        }
    }

    public Rental insert(Rental rental) {
        try (Connection connection = open()) {
            return insert(connection, rental);
        } catch (SQLException ex) {
            throw wrap(ex, "create rental");
        }
    }

    public Optional<Rental> findByToken(String token) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE r.rental_qr_token = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "find rental by QR token");
        }
    }

    public Optional<Rental> findOpenRequestByCustomer(Connection connection, long customerId, boolean lockRow)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM_JOIN
                + " WHERE r.customer_id = ? AND r.status IN ('PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED') LIMIT 1"
                + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public boolean hasOverlappingBooking(Connection connection, long bikeId, Long excludeRentalId,
                                         java.time.LocalDate start, java.time.LocalDate end) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM rentals
                 WHERE bike_id = ?
                   AND status IN ('PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED')
                   AND start_date <= ?
                   AND expected_return_date >= ?
                   AND (? IS NULL OR rental_id <> ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bikeId);
            setDate(statement, 2, end);
            setDate(statement, 3, start);
            setLong(statement, 4, excludeRentalId);
            setLong(statement, 5, excludeRentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }

    public Optional<Rental> findBlockingByBike(Connection connection, long bikeId, boolean lockRow)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM_JOIN
                + " WHERE r.bike_id = ? AND r.status IN ('PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED') LIMIT 1"
                + (lockRow ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bikeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public Rental insert(Connection connection, Rental rental) throws SQLException {
        String sql = """
                INSERT INTO rentals (
                    customer_id, bike_id, user_id, start_date, expected_return_date, actual_return_date,
                    duration_days, daily_rate, total_amount, late_fee, security_deposit, final_amount,
                    status, notes, rental_qr_token, rental_qr_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            statement.setLong(1, rental.getCustomerId());
            statement.setLong(2, rental.getBikeId());
            statement.setLong(3, rental.getUserId());
            setDate(statement, 4, rental.getStartDate());
            setDate(statement, 5, rental.getExpectedReturnDate());
            setDate(statement, 6, rental.getActualReturnDate());
            statement.setInt(7, rental.getDurationDays());
            statement.setBigDecimal(8, rental.getDailyRate());
            statement.setBigDecimal(9, rental.getTotalAmount());
            statement.setBigDecimal(10, rental.getLateFee());
            statement.setBigDecimal(11, rental.getSecurityDeposit() == null
                    ? java.math.BigDecimal.ZERO : rental.getSecurityDeposit());
            statement.setBigDecimal(12, rental.getFinalAmount());
            statement.setString(13, rental.getStatus());
            statement.setString(14, rental.getNotes());
            statement.setString(15, rental.getRentalQrToken());
            statement.setString(16, rental.getRentalQrStatus());
            statement.executeUpdate();
            rental.setId(generatedId(statement));
            return rental;
        }
    }

    public boolean update(Rental rental) {
        String sql = """
                UPDATE rentals
                   SET customer_id = ?, bike_id = ?, start_date = ?, expected_return_date = ?,
                       actual_return_date = ?, duration_days = ?, daily_rate = ?, total_amount = ?,
                       late_fee = ?, final_amount = ?, status = ?, notes = ?
                 WHERE rental_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rental.getCustomerId());
            statement.setLong(2, rental.getBikeId());
            setDate(statement, 3, rental.getStartDate());
            setDate(statement, 4, rental.getExpectedReturnDate());
            setDate(statement, 5, rental.getActualReturnDate());
            statement.setInt(6, rental.getDurationDays());
            statement.setBigDecimal(7, rental.getDailyRate());
            statement.setBigDecimal(8, rental.getTotalAmount());
            statement.setBigDecimal(9, rental.getLateFee());
            statement.setBigDecimal(10, rental.getFinalAmount());
            statement.setString(11, rental.getStatus());
            statement.setString(12, rental.getNotes());
            statement.setLong(13, rental.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update rental");
        }
    }

    public boolean update(Connection connection, Rental rental) throws SQLException {
        String sql = """
                UPDATE rentals
                   SET actual_return_date = ?, duration_days = ?, late_fee = ?, final_amount = ?,
                       status = ?, notes = ?, rental_qr_status = ?, approved_by = ?, approved_at = ?,
                       rejection_reason = ?, rejection_notes = ?, return_requested = ?, return_requested_at = ?
                 WHERE rental_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, rental.getActualReturnDate());
            statement.setInt(2, rental.getDurationDays());
            statement.setBigDecimal(3, rental.getLateFee());
            statement.setBigDecimal(4, rental.getFinalAmount());
            statement.setString(5, rental.getStatus());
            statement.setString(6, rental.getNotes());
            statement.setString(7, rental.getRentalQrStatus());
            setLong(statement, 8, rental.getApprovedBy());
            if (rental.getApprovedAt() == null) {
                statement.setTimestamp(9, null);
            } else {
                statement.setTimestamp(9, java.sql.Timestamp.valueOf(rental.getApprovedAt()));
            }
            statement.setString(10, rental.getRejectionReason());
            statement.setString(11, rental.getRejectionNotes());
            statement.setBoolean(12, rental.isReturnRequested());
            if (rental.getReturnRequestedAt() == null) {
                statement.setTimestamp(13, null);
            } else {
                statement.setTimestamp(13, java.sql.Timestamp.valueOf(rental.getReturnRequestedAt()));
            }
            statement.setLong(14, rental.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM rentals WHERE rental_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete rental");
        }
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM rentals WHERE status = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count rentals");
        }
    }

    private List<Rental> list(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list rentals");
        }
    }

    private List<Rental> mapAll(ResultSet resultSet) throws SQLException {
        List<Rental> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Rental map(ResultSet resultSet) throws SQLException {
        Rental rental = new Rental();
        rental.setId(resultSet.getLong("rental_id"));
        rental.setCustomerId(resultSet.getLong("customer_id"));
        rental.setCustomerName(resultSet.getString("customer_name"));
        rental.setBikeId(resultSet.getLong("bike_id"));
        rental.setBikeLabel(resultSet.getString("bike_label"));
        rental.setBikeQrCode(resultSet.getString("bike_qr_code"));
        rental.setRegistrationNumber(resultSet.getString("registration_number"));
        rental.setUserId(resultSet.getLong("user_id"));
        rental.setStartDate(readDate(resultSet, "start_date"));
        rental.setExpectedReturnDate(readDate(resultSet, "expected_return_date"));
        rental.setActualReturnDate(readDate(resultSet, "actual_return_date"));
        rental.setDurationDays(readInteger(resultSet, "duration_days"));
        rental.setDailyRate(readDecimal(resultSet, "daily_rate"));
        rental.setTotalAmount(readDecimal(resultSet, "total_amount"));
        rental.setLateFee(readDecimal(resultSet, "late_fee"));
        rental.setSecurityDeposit(readDecimal(resultSet, "security_deposit"));
        rental.setFinalAmount(readDecimal(resultSet, "final_amount"));
        rental.setStatus(resultSet.getString("status"));
        rental.setRentalQrToken(resultSet.getString("rental_qr_token"));
        rental.setRentalQrStatus(resultSet.getString("rental_qr_status"));
        rental.setApprovedBy(readLong(resultSet, "approved_by"));
        rental.setApprovedByName(resultSet.getString("approved_by_name"));
        rental.setApprovedAt(readDateTime(resultSet, "approved_at"));
        rental.setRejectionReason(resultSet.getString("rejection_reason"));
        rental.setRejectionNotes(resultSet.getString("rejection_notes"));
        rental.setReturnRequested(resultSet.getBoolean("return_requested"));
        rental.setReturnRequestedAt(readDateTime(resultSet, "return_requested_at"));
        rental.setNotes(resultSet.getString("notes"));
        rental.setCreatedAt(readDateTime(resultSet, "created_at"));
        rental.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        return rental;
    }
}
