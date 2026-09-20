package com.bikevault.dao;

import com.bikevault.model.Incident;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code incidents}.
 */
public class IncidentDAO extends DaoSupport {

    private static final String COLUMNS = """
            i.incident_id, i.customer_id, c.full_name AS customer_name, i.rental_id, i.bike_id,
            CONCAT(b.brand, ' ', b.model) AS bike_label, i.incident_type, i.description,
            i.incident_date, i.location, i.details, i.status, i.created_at, i.updated_at
            """;

    private static final String FROM_JOIN = """
            FROM incidents i
            JOIN customers c ON c.customer_id = i.customer_id
            LEFT JOIN bikes b ON b.bike_id = i.bike_id
            """;

    public Optional<Incident> findById(long id) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE i.incident_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load incident");
        }
    }

    public List<Incident> findAll() {
        return list("SELECT " + COLUMNS + FROM_JOIN + " ORDER BY i.created_at DESC");
    }

    public List<Incident> findByRentalId(long rentalId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE i.rental_id = ? ORDER BY i.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list incidents for rental");
        }
    }

    public List<Incident> findByCustomerId(long customerId) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE i.customer_id = ? ORDER BY i.created_at DESC";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list incidents for customer");
        }
    }

    public Incident insert(Incident incident) {
        String sql = """
                INSERT INTO incidents (customer_id, rental_id, bike_id, incident_type, description,
                                       incident_date, location, details, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            statement.setLong(1, incident.getCustomerId());
            setLong(statement, 2, incident.getRentalId());
            setLong(statement, 3, incident.getBikeId());
            statement.setString(4, incident.getIncidentType());
            statement.setString(5, incident.getDescription());
            setDate(statement, 6, incident.getIncidentDate());
            statement.setString(7, incident.getLocation());
            statement.setString(8, incident.getDetails());
            statement.setString(9, incident.getStatus());
            statement.executeUpdate();
            incident.setId(generatedId(statement));
            return incident;
        } catch (SQLException ex) {
            throw wrap(ex, "create incident");
        }
    }

    public boolean update(Incident incident) {
        String sql = """
                UPDATE incidents
                   SET incident_type = ?, description = ?, incident_date = ?, location = ?,
                       details = ?, status = ?, rental_id = ?, bike_id = ?
                 WHERE incident_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, incident.getIncidentType());
            statement.setString(2, incident.getDescription());
            setDate(statement, 3, incident.getIncidentDate());
            statement.setString(4, incident.getLocation());
            statement.setString(5, incident.getDetails());
            statement.setString(6, incident.getStatus());
            setLong(statement, 7, incident.getRentalId());
            setLong(statement, 8, incident.getBikeId());
            statement.setLong(9, incident.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update incident");
        }
    }

    public long countOpen() {
        return count("SELECT COUNT(*) FROM incidents WHERE status IN ('REPORTED', 'UNDER_REVIEW')");
    }

    private long count(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw wrap(ex, "count incidents");
        }
    }

    private List<Incident> list(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list incidents");
        }
    }

    private List<Incident> mapAll(ResultSet resultSet) throws SQLException {
        List<Incident> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Incident map(ResultSet resultSet) throws SQLException {
        Incident incident = new Incident();
        incident.setId(resultSet.getLong("incident_id"));
        incident.setCustomerId(resultSet.getLong("customer_id"));
        incident.setCustomerName(resultSet.getString("customer_name"));
        incident.setRentalId(readLong(resultSet, "rental_id"));
        incident.setBikeId(readLong(resultSet, "bike_id"));
        incident.setBikeLabel(resultSet.getString("bike_label"));
        incident.setIncidentType(resultSet.getString("incident_type"));
        incident.setDescription(resultSet.getString("description"));
        incident.setIncidentDate(readDate(resultSet, "incident_date"));
        incident.setLocation(resultSet.getString("location"));
        incident.setDetails(resultSet.getString("details"));
        incident.setStatus(resultSet.getString("status"));
        incident.setCreatedAt(readDateTime(resultSet, "created_at"));
        incident.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        return incident;
    }
}
