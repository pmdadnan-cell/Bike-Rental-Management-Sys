package com.bikevault.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Shared input checks used by controllers and services.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10,15}$");
    private static final Pattern LICENSE = Pattern.compile("^[A-Za-z0-9-]{6,30}$");

    private ValidationUtil() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String requireText(String value, String fieldName) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return trimmed;
    }

    public static boolean isEmail(String value) {
        return hasText(value) && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isPhone(String value) {
        return hasText(value) && PHONE.matcher(value.trim()).matches();
    }

    public static boolean isLicense(String value) {
        return hasText(value) && LICENSE.matcher(value.trim()).matches();
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static String requireEmail(String value) {
        String email = requireText(value, "Email");
        if (!isEmail(email)) {
            throw new IllegalArgumentException("Enter a valid email such as name@example.com.");
        }
        return email;
    }

    public static String requirePhone(String value) {
        String phone = requireText(value, "Phone").replaceAll("[\\s-()]", "");
        if (!isPhone(phone)) {
            throw new IllegalArgumentException("Enter a valid numeric phone number (10 to 15 digits).");
        }
        return phone;
    }

    public static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (!isPositive(value)) {
            throw new IllegalArgumentException(fieldName + " must be a positive number.");
        }
        return value;
    }
}
