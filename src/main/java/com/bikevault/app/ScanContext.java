package com.bikevault.app;

import com.bikevault.model.Bike;
import com.bikevault.model.Rental;

import java.util.Optional;

/**
 * Hands a verified bike or rental QR result back to the screen that requested the scan.
 */
public final class ScanContext {

    private static AppView returnView = AppView.QR_SCANNER;
    private static Bike pendingBike;
    private static Rental pendingRental;
    private static boolean rentalMode;
    private static boolean returnMode;

    private ScanContext() {
    }

    public static void requestScan(AppView origin) {
        pendingBike = null;
        pendingRental = null;
        rentalMode = false;
        returnMode = false;
        returnView = origin == null ? AppView.QR_SCANNER : origin;
        com.bikevault.util.NavigationUtil.showView(AppView.QR_SCANNER);
    }

    public static void requestRentalScan(AppView origin) {
        pendingBike = null;
        pendingRental = null;
        rentalMode = true;
        returnMode = false;
        returnView = origin == null ? AppView.APPROVAL_CENTER : origin;
        com.bikevault.util.NavigationUtil.showView(AppView.QR_SCANNER);
    }

    public static void requestReturnScan(AppView origin) {
        pendingBike = null;
        pendingRental = null;
        rentalMode = true;
        returnMode = true;
        returnView = origin == null ? AppView.RENTALS : origin;
        com.bikevault.util.NavigationUtil.showView(AppView.QR_SCANNER);
    }

    public static void complete(Bike bike) {
        pendingBike = bike;
        if (returnView != null && returnView != AppView.QR_SCANNER) {
            com.bikevault.util.NavigationUtil.showView(returnView);
        }
    }

    public static void completeRental(Rental rental) {
        pendingRental = rental;
        if (returnView != null && returnView != AppView.QR_SCANNER) {
            com.bikevault.util.NavigationUtil.showView(returnView);
        }
    }

    public static Optional<Bike> takePending() {
        Bike bike = pendingBike;
        pendingBike = null;
        return Optional.ofNullable(bike);
    }

    public static Optional<Rental> takePendingRental() {
        Rental rental = pendingRental;
        pendingRental = null;
        return Optional.ofNullable(rental);
    }

    public static AppView getReturnView() {
        return returnView;
    }

    public static boolean isRentalMode() {
        return rentalMode;
    }

    public static boolean isReturnMode() {
        return returnMode;
    }

    public static void clear() {
        pendingBike = null;
        pendingRental = null;
        rentalMode = false;
        returnMode = false;
        returnView = AppView.QR_SCANNER;
    }
}
