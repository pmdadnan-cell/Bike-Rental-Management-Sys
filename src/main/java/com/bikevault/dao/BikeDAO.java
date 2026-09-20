package com.bikevault.dao;

import com.bikevault.model.Bike;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code bikes}. QR identity lookups are the primary verification path.
 */
public class BikeDAO extends DaoSupport {

    private static final String COLUMNS = """
            b.bike_id, b.qr_code, b.registration_number, b.model, b.brand, b.category_id,
            c.name AS category_name, b.color, b.daily_rate, b.status, b.purchase_date,
            b.description, b.image_path, b.created_at, b.updated_at
            """;

    private static final String FROM_JOIN = """
            FROM bikes b
            JOIN bike_categories c ON c.category_id = b.category_id
            """;

    public Optional<Bike> findById(long id) {
        return findOne("SELECT " + COLUMNS + FROM_JOIN + " WHERE b.bike_id = ?",
                statement -> statement.setLong(1, id), "load bike");
    }

    public Optional<Bike> findByIdForUpdate(Connection connection, long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE b.bike_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public long countByCategory(long categoryId) {
        String sql = "SELECT COUNT(*) FROM bikes WHERE category_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count bikes by category");
        }
    }

    public Optional<Bike> findByQrCode(String qrCode) {
        return findOne("SELECT " + COLUMNS + FROM_JOIN + " WHERE b.qr_code = ?",
                statement -> statement.setString(1, qrCode), "find bike by QR");
    }

    public Optional<Bike> findByRegistrationNumber(String registrationNumber) {
        return findOne("SELECT " + COLUMNS + FROM_JOIN + " WHERE b.registration_number = ?",
                statement -> statement.setString(1, registrationNumber), "find bike by registration");
    }

    public List<Bike> findAll() {
        return list("SELECT " + COLUMNS + FROM_JOIN + " ORDER BY b.brand, b.model");
    }

    public List<Bike> findByStatus(String status) {
        String sql = "SELECT " + COLUMNS + FROM_JOIN + " WHERE b.status = ? ORDER BY b.brand, b.model";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "list bikes by status");
        }
    }

    public List<Bike> search(String term) {
        String sql = """
                SELECT %s %s
                 WHERE b.qr_code LIKE ?
                    OR b.registration_number LIKE ?
                    OR b.model LIKE ?
                    OR b.brand LIKE ?
                    OR CAST(b.bike_id AS CHAR) LIKE ?
                 ORDER BY b.brand, b.model
                """.formatted(COLUMNS, FROM_JOIN);
        String like = "%" + (term == null ? "" : term.trim()) + "%";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                statement.setString(i, like);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "search bikes");
        }
    }

    public Bike insert(Bike bike) {
        String sql = """
                INSERT INTO bikes (
                    qr_code, registration_number, model, brand, category_id, color,
                    daily_rate, status, purchase_date, description, image_path
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = open()) {
            return insert(connection, bike);
        } catch (SQLException ex) {
            throw wrap(ex, "create bike");
        }
    }

    public Bike insert(Connection connection, Bike bike) throws SQLException {
        String sql = """
                INSERT INTO bikes (
                    qr_code, registration_number, model, brand, category_id, color,
                    daily_rate, status, purchase_date, description, image_path
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            bind(statement, bike);
            statement.executeUpdate();
            bike.setId(generatedId(statement));
            return bike;
        }
    }

    public boolean update(Bike bike) {
        String sql = """
                UPDATE bikes
                   SET registration_number = ?, model = ?, brand = ?, category_id = ?, color = ?,
                       daily_rate = ?, status = ?, purchase_date = ?, description = ?, image_path = ?
                 WHERE bike_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bike.getRegistrationNumber());
            statement.setString(2, bike.getModel());
            statement.setString(3, bike.getBrand());
            statement.setLong(4, bike.getCategoryId());
            statement.setString(5, bike.getColor());
            statement.setBigDecimal(6, bike.getDailyRate());
            statement.setString(7, bike.getStatus());
            setDate(statement, 8, bike.getPurchaseDate());
            statement.setString(9, bike.getDescription());
            statement.setString(10, bike.getImagePath());
            statement.setLong(11, bike.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update bike");
        }
    }

    public boolean updateStatus(long bikeId, String status) {
        try (Connection connection = open()) {
            return updateStatus(connection, bikeId, status);
        } catch (SQLException ex) {
            throw wrap(ex, "update bike status");
        }
    }

    public boolean updateStatus(Connection connection, long bikeId, String status) throws SQLException {
        String sql = "UPDATE bikes SET status = ? WHERE bike_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, bikeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM bikes WHERE bike_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete bike");
        }
    }

    public long countAll() {
        return count("SELECT COUNT(*) FROM bikes");
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM bikes WHERE status = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw wrap(ex, "count bikes by status");
        }
    }

    private void bind(PreparedStatement statement, Bike bike) throws SQLException {
        statement.setString(1, bike.getQrCode());
        statement.setString(2, bike.getRegistrationNumber());
        statement.setString(3, bike.getModel());
        statement.setString(4, bike.getBrand());
        statement.setLong(5, bike.getCategoryId());
        statement.setString(6, bike.getColor());
        statement.setBigDecimal(7, bike.getDailyRate());
        statement.setString(8, bike.getStatus());
        setDate(statement, 9, bike.getPurchaseDate());
        statement.setString(10, bike.getDescription());
        statement.setString(11, bike.getImagePath());
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private Optional<Bike> findOne(String sql, Binder binder, String action) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, action);
        }
    }

    private List<Bike> list(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list bikes");
        }
    }

    private long count(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw wrap(ex, "count bikes");
        }
    }

    private List<Bike> mapAll(ResultSet resultSet) throws SQLException {
        List<Bike> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Bike map(ResultSet resultSet) throws SQLException {
        Bike bike = new Bike();
        bike.setId(resultSet.getLong("bike_id"));
        bike.setQrCode(resultSet.getString("qr_code"));
        bike.setRegistrationNumber(resultSet.getString("registration_number"));
        bike.setModel(resultSet.getString("model"));
        bike.setBrand(resultSet.getString("brand"));
        bike.setCategoryId(resultSet.getLong("category_id"));
        bike.setCategoryName(resultSet.getString("category_name"));
        bike.setColor(resultSet.getString("color"));
        bike.setDailyRate(readDecimal(resultSet, "daily_rate"));
        bike.setStatus(resultSet.getString("status"));
        bike.setPurchaseDate(readDate(resultSet, "purchase_date"));
        bike.setDescription(resultSet.getString("description"));
        bike.setImagePath(resultSet.getString("image_path"));
        bike.setCreatedAt(readDateTime(resultSet, "created_at"));
        bike.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        return bike;
    }
}
