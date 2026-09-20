package com.bikevault.app;

import com.bikevault.exception.UnauthorizedException;
import com.bikevault.model.User;

import java.time.LocalDateTime;

/**
 * In-memory session for the signed-in operator or customer.
 */
public final class SessionManager {

    private static User currentUser;
    private static Long customerId;
    private static LocalDateTime loginTime;

    private SessionManager() {
    }

    public static void begin(User user) {
        begin(user, null);
    }

    public static void begin(User user, Long linkedCustomerId) {
        currentUser = user;
        customerId = linkedCustomerId;
        loginTime = LocalDateTime.now();
    }

    public static void clear() {
        currentUser = null;
        customerId = null;
        loginTime = null;
    }

    public static void logout() {
        clear();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    public static Long getCustomerId() {
        return customerId;
    }

    public static LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && AppConstants.ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole());
    }

    public static boolean isStaff() {
        return currentUser != null && AppConstants.ROLE_STAFF.equalsIgnoreCase(currentUser.getRole());
    }

    public static boolean isOperator() {
        return isAdmin() || isStaff();
    }

    public static boolean isCustomer() {
        return currentUser != null && AppConstants.ROLE_CUSTOMER.equalsIgnoreCase(currentUser.getRole());
    }

    public static long requireCustomerId() {
        if (!isCustomer() || customerId == null) {
            throw new UnauthorizedException("This action is only available to a signed-in customer.");
        }
        return customerId;
    }

    public static void requireOperator() {
        if (!isOperator()) {
            throw new UnauthorizedException("This action is only available to staff or administrators.");
        }
    }

    public static void requireAdminRole() {
        if (!isAdmin()) {
            throw new UnauthorizedException("This action is only available to administrators.");
        }
    }

    public static String displayName() {
        if (currentUser == null) {
            return "Guest";
        }
        return currentUser.getFullName() == null ? currentUser.getUsername() : currentUser.getFullName();
    }
}
