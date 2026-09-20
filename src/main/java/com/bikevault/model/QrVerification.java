package com.bikevault.model;

/**
 * Result of QR → parse → validate → database lookup.
 */
public final class QrVerification {

    public enum Status {
        VERIFIED,
        INVALID,
        INACTIVE
    }

    private final Status status;
    private final String message;
    private final Bike bike;
    private final Rental rental;
    private final QrPayload payload;

    private QrVerification(Status status, String message, Bike bike, Rental rental, QrPayload payload) {
        this.status = status;
        this.message = message;
        this.bike = bike;
        this.rental = rental;
        this.payload = payload;
    }

    public static QrVerification invalid(String message) {
        return new QrVerification(Status.INVALID, message, null, null, null);
    }

    public static QrVerification inactive(Bike bike, QrPayload payload) {
        return new QrVerification(Status.INACTIVE, "BIKE INACTIVE", bike, null, payload);
    }

    public static QrVerification verified(Bike bike, QrPayload payload) {
        return new QrVerification(Status.VERIFIED, "BIKE VERIFIED", bike, null, payload);
    }

    public static QrVerification rental(Rental rental, Bike bike, QrPayload payload, String message) {
        return new QrVerification(Status.VERIFIED, message, bike, rental, payload);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Bike getBike() {
        return bike;
    }

    public Rental getRental() {
        return rental;
    }

    public QrPayload getPayload() {
        return payload;
    }

    public boolean isVerified() {
        return status == Status.VERIFIED;
    }

    public boolean isRentalQr() {
        return rental != null;
    }
}
