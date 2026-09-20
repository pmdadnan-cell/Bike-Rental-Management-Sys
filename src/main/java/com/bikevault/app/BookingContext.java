package com.bikevault.app;

import com.bikevault.model.Bike;
import com.bikevault.model.Rental;
import com.bikevault.model.ReturnRecord;

/**
 * Hands a selected bike and quoted rental between customer marketplace screens.
 */
public final class BookingContext {

    private static Bike selectedBike;
    private static Rental quotedRental;
    private static ReturnRecord pendingReturn;

    private BookingContext() {
    }

    public static void selectBike(Bike bike) {
        selectedBike = bike;
        quotedRental = null;
        pendingReturn = null;
    }

    public static Bike getSelectedBike() {
        return selectedBike;
    }

    public static void setQuotedRental(Rental rental) {
        quotedRental = rental;
    }

    public static Rental getQuotedRental() {
        return quotedRental;
    }

    public static void setPendingReturn(ReturnRecord record) {
        pendingReturn = record;
    }

    public static ReturnRecord getPendingReturn() {
        return pendingReturn;
    }

    public static void clear() {
        selectedBike = null;
        quotedRental = null;
        pendingReturn = null;
    }
}
