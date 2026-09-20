package com.bikevault.dao;

import com.bikevault.model.BikeCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code bike_categories}.
 */
public class CategoryDAO extends DaoSupport {

    private static final String COLUMNS =
            "category_id, name, description, late_fee_per_day, status, created_at, updated_at";

    public Optional<BikeCategory> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM bike_categories WHERE category_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load category");
        }
    }

    public List<BikeCategory> findAll() {
        return list("SELECT " + COLUMNS + " FROM bike_categories ORDER BY name");
    }

    public List<BikeCategory> findActive() {
        return list("SELECT " + COLUMNS + " FROM bike_categories WHERE status = 'ACTIVE' ORDER BY name");
    }

    public BikeCategory insert(BikeCategory category) {
        String sql = """
                INSERT INTO bike_categories (name, description, late_fee_per_day, status)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setBigDecimal(3, category.getLateFeePerDay());
            statement.setString(4, category.getStatus());
            statement.executeUpdate();
            category.setId(generatedId(statement));
            return category;
        } catch (SQLException ex) {
            throw wrap(ex, "create category");
        }
    }

    public boolean update(BikeCategory category) {
        String sql = """
                UPDATE bike_categories
                   SET name = ?, description = ?, late_fee_per_day = ?, status = ?
                 WHERE category_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setBigDecimal(3, category.getLateFeePerDay());
            statement.setString(4, category.getStatus());
            statement.setLong(5, category.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update category");
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM bike_categories WHERE category_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete category");
        }
    }

    private List<BikeCategory> list(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<BikeCategory> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(map(resultSet));
            }
            return rows;
        } catch (SQLException ex) {
            throw wrap(ex, "list categories");
        }
    }

    private BikeCategory map(ResultSet resultSet) throws SQLException {
        BikeCategory category = new BikeCategory();
        category.setId(resultSet.getLong("category_id"));
        category.setName(resultSet.getString("name"));
        category.setDescription(resultSet.getString("description"));
        category.setLateFeePerDay(readDecimal(resultSet, "late_fee_per_day"));
        category.setStatus(resultSet.getString("status"));
        category.setCreatedAt(readDateTime(resultSet, "created_at"));
        category.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        return category;
    }
}
