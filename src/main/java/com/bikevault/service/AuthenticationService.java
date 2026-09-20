package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.CustomerDAO;
import com.bikevault.dao.UserDAO;
import com.bikevault.database.DatabaseConnection;
import com.bikevault.model.Customer;
import com.bikevault.model.User;
import com.bikevault.util.PasswordUtil;
import com.bikevault.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Authenticates operators and customers against the {@code users} table.
 */
public class AuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserDAO userDAO;
    private final CustomerDAO customerDAO;
    private final AuditLogDAO auditLogDAO;

    public AuthenticationService() {
        this(new UserDAO(), new CustomerDAO(), new AuditLogDAO());
    }

    public AuthenticationService(UserDAO userDAO, AuditLogDAO auditLogDAO) {
        this(userDAO, new CustomerDAO(), auditLogDAO);
    }

    public AuthenticationService(UserDAO userDAO, CustomerDAO customerDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO;
        this.customerDAO = customerDAO;
        this.auditLogDAO = auditLogDAO;
    }

    /**
     * Validates credentials and begins a role-aware session when authentication succeeds.
     */
    public Optional<User> authenticate(String username, String password) {
        if (!ValidationUtil.hasText(username) || !ValidationUtil.hasText(password)) {
            return Optional.empty();
        }

        Optional<User> account = userDAO.findByUsername(username.trim());
        if (account.isEmpty()) {
            LOG.warn("Authentication failed for unknown username '{}'", username.trim());
            return Optional.empty();
        }

        User user = account.get();
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            LOG.warn("Authentication failed for username '{}'", username.trim());
            return Optional.empty();
        }
        if (!AppConstants.STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            LOG.warn("Inactive account '{}' attempted to sign in", username.trim());
            return Optional.empty();
        }

        User sessionUser = user.withoutPassword();
        Long customerId = null;
        if (AppConstants.ROLE_CUSTOMER.equalsIgnoreCase(sessionUser.getRole())) {
            Customer linked = customerDAO.findByUserId(sessionUser.getId()).orElse(null);
            if (linked == null || !AppConstants.STATUS_ACTIVE.equalsIgnoreCase(linked.getStatus())) {
                LOG.warn("Customer account '{}' has no active customer profile", username.trim());
                return Optional.empty();
            }
            customerId = linked.getId();
        }

        SessionManager.begin(sessionUser, customerId);
        String action = loginAction(sessionUser.getRole());
        try {
            auditLogDAO.insert(sessionUser.getId(), action,
                    "User '" + sessionUser.getUsername() + "' signed in as " + sessionUser.getRole());
        } catch (RuntimeException ex) {
            LOG.warn("Login succeeded but the audit log could not be written", ex);
        }
        LOG.info("User '{}' signed in with role {}", sessionUser.getUsername(), sessionUser.getRole());
        return Optional.of(sessionUser);
    }

    public User registerCustomer(String username, String password, Customer profile) {
        String login = ValidationUtil.requireText(username, "Username").trim();
        String secret = ValidationUtil.requireText(password, "Password");
        if (login.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (secret.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (userDAO.usernameExists(login)) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        profile.setFullName(ValidationUtil.requireText(profile.getFullName(), "Full name"));
        profile.setEmail(ValidationUtil.requireEmail(profile.getEmail()));
        profile.setPhone(ValidationUtil.requirePhone(profile.getPhone()));
        profile.setAddress(ValidationUtil.requireText(profile.getAddress(), "Address"));
        profile.setDrivingLicenseNumber(ValidationUtil.requireText(profile.getDrivingLicenseNumber(), "Driving license")
                .toUpperCase());
        if (profile.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (profile.getDateOfBirth().isAfter(LocalDate.now().minusYears(16))) {
            throw new IllegalArgumentException("You must be at least 16 years old to register.");
        }
        if (!ValidationUtil.hasText(profile.getGender())) {
            profile.setGender("OTHER");
        }
        profile.setStatus(AppConstants.STATUS_ACTIVE);
        profile.setRegistrationDate(LocalDate.now());

        return DatabaseConnection.getInstance().inTransaction(connection -> {
            User user = new User();
            user.setUsername(login);
            user.setFullName(profile.getFullName());
            user.setRole(AppConstants.ROLE_CUSTOMER);
            user.setStatus(AppConstants.STATUS_ACTIVE);
            userDAO.insert(connection, user, secret);
            profile.setUserId(user.getId());
            customerDAO.insert(connection, profile);
            auditLogDAO.insert(connection, user.getId(), AppConstants.ACTION_REGISTER,
                    "Registered customer '" + login + "'");
            return user.withoutPassword();
        });
    }

    public void changeOwnPassword(String currentPassword, String newPassword) {
        User session = SessionManager.getCurrentUser();
        if (session == null) {
            throw new IllegalStateException("Sign in before changing a password.");
        }
        String current = ValidationUtil.requireText(currentPassword, "Current password");
        String next = ValidationUtil.requireText(newPassword, "New password");
        if (next.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }
        if (current.equals(next)) {
            throw new IllegalArgumentException("Choose a new password that is different from the current one.");
        }

        User account = userDAO.findByUsername(session.getUsername())
                .orElseThrow(() -> new IllegalStateException("The signed-in account could not be loaded."));
        if (!PasswordUtil.matches(current, account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        userDAO.updatePassword(session.getId(), next);
        try {
            auditLogDAO.insert(session.getId(), AppConstants.ACTION_CHANGE_PASSWORD,
                    "Password changed for '" + session.getUsername() + "'");
        } catch (RuntimeException ex) {
            LOG.warn("Password changed but the audit log could not be written", ex);
        }
    }

    private String loginAction(String role) {
        if (AppConstants.ROLE_ADMIN.equalsIgnoreCase(role)) {
            return AppConstants.ACTION_ADMIN_LOGIN;
        }
        if (AppConstants.ROLE_CUSTOMER.equalsIgnoreCase(role)) {
            return AppConstants.ACTION_CUSTOMER_LOGIN;
        }
        return AppConstants.ACTION_STAFF_LOGIN;
    }
}
