package com.bikevault.app;

/**
 * Hands a selected rental between My Trips, trip details, and the rental pass.
 */
public final class TripContext {

    private static Long rentalId;

    private TripContext() {
    }

    public static void select(Long id) {
        rentalId = id;
    }

    public static Long getRentalId() {
        return rentalId;
    }

    public static void clear() {
        rentalId = null;
    }
}
