package com.bikevault.dao;

import com.bikevault.model.User;
import com.bikevault.util.PasswordUtil;
import com.bikevault.util.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code users}. Password hashes stay inside this layer except during authentication.
 */
public class UserDAO extends DaoSupport {

    private static final String BASE_COLUMNS =
            "user_id, username, full_name, role, status, created_at, updated_at";

    public Optional<User> findById(long id) {
        String sql = "SELECT " + BASE_COLUMNS + " FROM users WHERE user_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet, false)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load user");
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT " + BASE_COLUMNS + ", password_hash FROM users WHERE username = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet, true)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "find user by username");
        }
    }

    public List<User> findAll() {
        String sql = "SELECT " + BASE_COLUMNS + " FROM users ORDER BY username";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet, false));
            }
            return users;
        } catch (SQLException ex) {
            throw wrap(ex, "list users");
        }
    }

    public User insert(User user, String rawPassword) {
        try (Connection connection = open()) {
            return insert(connection, user, rawPassword);
        } catch (SQLException ex) {
            throw wrap(ex, "create user");
        }
    }

    public User insert(Connection connection, User user, String rawPassword) throws SQLException {
        String sql = """
                INSERT INTO users (username, password_hash, full_name, role, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            statement.setString(1, user.getUsername());
            statement.setString(2, PasswordUtil.hash(rawPassword));
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getRole());
            statement.setString(5, user.getStatus());
            statement.executeUpdate();
            user.setId(generatedId(statement));
            user.setPasswordHash(null);
            return user;
        }
    }

    public boolean updateStatus(long userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update user status");
        }
    }

    public boolean update(User user) {
        String sql = """
                UPDATE users
                   SET full_name = ?, role = ?, status = ?
                 WHERE user_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getRole());
            statement.setString(3, user.getStatus());
            statement.setLong(4, user.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update user");
        }
    }

    public boolean updatePassword(long userId, String rawPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PasswordUtil.hash(rawPassword));
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update password");
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete user");
        }
    }

    private User mapUser(ResultSet resultSet, boolean includeHash) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(resultSet.getString("role"));
        user.setStatus(resultSet.getString("status"));
        user.setCreatedAt(readDateTime(resultSet, "created_at"));
        user.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        if (includeHash) {
            user.setPasswordHash(resultSet.getString("password_hash"));
        }
        return user;
    }

    public boolean usernameExists(String username) {
        if (!ValidationUtil.hasText(username)) {
            return false;
        }
        return findByUsername(username.trim()).isPresent();
    }
}
