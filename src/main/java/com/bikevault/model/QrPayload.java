package com.bikevault.model;

/**
 * Structured payload stored inside a bike or rental QR image.
 */
public final class QrPayload {

    private final Long bikeId;
    private final String qrCode;
    private final String rawText;
    private final String rentalToken;

    public QrPayload(Long bikeId, String qrCode, String rawText) {
        this(bikeId, qrCode, rawText, null);
    }

    public QrPayload(Long bikeId, String qrCode, String rawText, String rentalToken) {
        this.bikeId = bikeId;
        this.qrCode = qrCode;
        this.rawText = rawText;
        this.rentalToken = rentalToken;
    }

    public Long getBikeId() {
        return bikeId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getRawText() {
        return rawText;
    }

    public String getRentalToken() {
        return rentalToken;
    }

    public boolean isRentalQr() {
        return rentalToken != null && !rentalToken.isBlank();
    }
}
