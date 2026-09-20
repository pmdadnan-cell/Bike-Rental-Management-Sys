package com.bikevault.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure rental and late-fee arithmetic. Controllers must not hard-code final prices.
 */
public final class PricingCalculator {

    private PricingCalculator() {
    }

    public static int rentalDays(LocalDate start, LocalDate expectedReturn) {
        if (start == null || expectedReturn == null) {
            throw new IllegalArgumentException("Rental start and expected return dates are required.");
        }
        if (expectedReturn.isBefore(start)) {
            throw new IllegalArgumentException("Expected return date cannot be before the start date.");
        }
        long days = ChronoUnit.DAYS.between(start, expectedReturn);
        return days < 1 ? 1 : (int) days;
    }

    public static int actualDays(LocalDate start, LocalDate actualReturn) {
        return rentalDays(start, actualReturn);
    }

    public static int lateDays(LocalDate expectedReturn, LocalDate actualReturn) {
        if (expectedReturn == null || actualReturn == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(expectedReturn, actualReturn);
        return days > 0 ? (int) days : 0;
    }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal rentalAmount(BigDecimal dailyRate, int days) {
        if (!ValidationUtil.isPositive(dailyRate)) {
            throw new IllegalArgumentException("Daily rate must be a positive number.");
        }
        if (days < 1) {
            throw new IllegalArgumentException("Rental duration must be at least one day.");
        }
        return money(dailyRate.multiply(BigDecimal.valueOf(days)));
    }

    public static BigDecimal lateFee(BigDecimal lateFeePerDay, int lateDays) {
        if (lateDays <= 0) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal perDay = lateFeePerDay == null ? BigDecimal.ZERO : lateFeePerDay;
        return money(perDay.multiply(BigDecimal.valueOf(lateDays)));
    }

    public static BigDecimal finalAmount(BigDecimal baseAmount, BigDecimal lateFee) {
        return money(money(baseAmount).add(money(lateFee)));
    }

    /**
     * Hold amount derived from the bike's daily rate (one day), never a hardcoded figure.
     */
    public static BigDecimal securityDeposit(BigDecimal dailyRate) {
        return money(dailyRate);
    }

    public static BigDecimal payable(BigDecimal rentalAmount, BigDecimal deposit, BigDecimal penalties) {
        return money(money(rentalAmount).add(money(deposit)).add(money(penalties)));
    }
}
