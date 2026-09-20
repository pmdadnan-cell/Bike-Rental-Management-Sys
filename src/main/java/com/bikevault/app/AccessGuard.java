package com.bikevault.app;

import com.bikevault.exception.UnauthorizedException;
import com.bikevault.util.AlertUtil;

/**
 * Shared authorization checks for destructive or role-restricted actions.
 */
public final class AccessGuard {

    private AccessGuard() {
    }

    public static boolean requireAdmin(String action) {
        if (SessionManager.isAdmin()) {
            return true;
        }
        AlertUtil.warn("Administrator only",
                "This account cannot " + action + ". Sign in as an administrator.");
        return false;
    }

    public static boolean requireOperator(String action) {
        if (SessionManager.isOperator()) {
            return true;
        }
        AlertUtil.warn("Staff only",
                "Customers cannot " + action + ".");
        return false;
    }

    public static boolean requireCustomer(String action) {
        if (SessionManager.isCustomer()) {
            return true;
        }
        AlertUtil.warn("Customer only",
                "Sign in with a customer account to " + action + ".");
        return false;
    }

    public static void assertOperator() {
        SessionManager.requireOperator();
    }

    public static void assertAdmin() {
        SessionManager.requireAdminRole();
    }

    public static void assertCustomer() {
        if (!SessionManager.isCustomer()) {
            throw new UnauthorizedException("This action is only available to a signed-in customer.");
        }
    }
}
