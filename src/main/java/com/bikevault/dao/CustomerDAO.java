package com.bikevault.dao;

import com.bikevault.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD access for {@code customers}.
 */
public class CustomerDAO extends DaoSupport {

    private static final String COLUMNS = """
            customer_id, user_id, full_name, email, phone, address, driving_license_number,
            date_of_birth, gender, status, registration_date, created_at, updated_at
            """;

    public Optional<Customer> findByUserId(long userId) {
        String sql = "SELECT " + COLUMNS + " FROM customers WHERE user_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load customer by user");
        }
    }

    public Optional<Customer> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM customers WHERE customer_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw wrap(ex, "load customer");
        }
    }

    public List<Customer> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM customers ORDER BY full_name";
        return queryList(sql);
    }

    public List<Customer> search(String term) {
        String sql = """
                SELECT %s FROM customers
                 WHERE full_name LIKE ?
                    OR phone LIKE ?
                    OR email LIKE ?
                    OR driving_license_number LIKE ?
                    OR CAST(customer_id AS CHAR) LIKE ?
                 ORDER BY full_name
                """.formatted(COLUMNS);
        String like = like(term);
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                statement.setString(i, like);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException ex) {
            throw wrap(ex, "search customers");
        }
    }

    public Customer insert(Customer customer) {
        try (Connection connection = open()) {
            return insert(connection, customer);
        } catch (SQLException ex) {
            throw wrap(ex, "create customer");
        }
    }

    public Customer insert(Connection connection, Customer customer) throws SQLException {
        String sql = """
                INSERT INTO customers (
                    full_name, email, phone, address, driving_license_number,
                    date_of_birth, gender, status, registration_date, user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, statementGeneratedKeys())) {
            bind(statement, customer);
            setLong(statement, 10, customer.getUserId());
            statement.executeUpdate();
            customer.setId(generatedId(statement));
            return customer;
        }
    }

    public boolean update(Customer customer) {
        String sql = """
                UPDATE customers
                   SET full_name = ?, email = ?, phone = ?, address = ?, driving_license_number = ?,
                       date_of_birth = ?, gender = ?, status = ?, registration_date = ?
                 WHERE customer_id = ?
                """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, customer);
            statement.setLong(10, customer.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "update customer");
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw wrap(ex, "delete customer");
        }
    }

    public long countAll() {
        return count("SELECT COUNT(*) FROM customers");
    }

    private void bind(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getFullName());
        statement.setString(2, customer.getEmail());
        statement.setString(3, customer.getPhone());
        statement.setString(4, customer.getAddress());
        statement.setString(5, customer.getDrivingLicenseNumber());
        setDate(statement, 6, customer.getDateOfBirth());
        statement.setString(7, customer.getGender());
        statement.setString(8, customer.getStatus());
        setDate(statement, 9, customer.getRegistrationDate());
    }

    private List<Customer> queryList(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException ex) {
            throw wrap(ex, "list customers");
        }
    }

    private long count(String sql) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw wrap(ex, "count customers");
        }
    }

    private List<Customer> mapAll(ResultSet resultSet) throws SQLException {
        List<Customer> rows = new ArrayList<>();
        while (resultSet.next()) {
            rows.add(map(resultSet));
        }
        return rows;
    }

    private Customer map(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer();
        customer.setId(resultSet.getLong("customer_id"));
        customer.setUserId(readLong(resultSet, "user_id"));
        customer.setFullName(resultSet.getString("full_name"));
        customer.setEmail(resultSet.getString("email"));
        customer.setPhone(resultSet.getString("phone"));
        customer.setAddress(resultSet.getString("address"));
        customer.setDrivingLicenseNumber(resultSet.getString("driving_license_number"));
        customer.setDateOfBirth(readDate(resultSet, "date_of_birth"));
        customer.setGender(resultSet.getString("gender"));
        customer.setStatus(resultSet.getString("status"));
        customer.setRegistrationDate(readDate(resultSet, "registration_date"));
        customer.setCreatedAt(readDateTime(resultSet, "created_at"));
        customer.setUpdatedAt(readDateTime(resultSet, "updated_at"));
        return customer;
    }

    private String like(String term) {
        return "%" + (term == null ? "" : term.trim()) + "%";
    }
}
