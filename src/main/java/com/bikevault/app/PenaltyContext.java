package com.bikevault.app;

/**
 * Selected penalty for the customer payment screen.
 */
public final class PenaltyContext {

    private static Long penaltyId;

    private PenaltyContext() {
    }

    public static void select(Long id) {
        penaltyId = id;
    }

    public static Long getPenaltyId() {
        return penaltyId;
    }

    public static void clear() {
        penaltyId = null;
    }
}
