package com.bikevault.util;

import com.bikevault.app.AppConstants;
import com.bikevault.exception.QRException;
import com.bikevault.model.Bike;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Creates unique QR identities and PNG files under {@code generated_qr/}.
 */
public final class QRGenerator {

    private static final int SIZE = 420;

    private QRGenerator() {
    }

    public static String newIdentity() {
        return "BIKE-" + UUID.randomUUID().toString().toUpperCase();
    }

    public static String newRentalToken() {
        return "RVQR-" + UUID.randomUUID().toString().toUpperCase();
    }

    public static String rentalPayload(String token) {
        return AppConstants.APP_NAME + "|RENTAL|TOKEN=" + token;
    }

    public static BufferedImage render(String payload, int size) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );
            int pixels = size <= 0 ? SIZE : size;
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, pixels, pixels, hints);
            return MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig());
        } catch (WriterException ex) {
            throw new QRException("Unable to render the QR image.", ex);
        }
    }

    public static String payloadFor(Bike bike) {
        String qr = bike.getQrCode() == null ? "" : bike.getQrCode();
        String idPart = bike.getId() == null ? "" : String.valueOf(bike.getId());
        return AppConstants.APP_NAME + "|BIKE_ID=" + idPart + "|QR=" + qr;
    }

    public static Path imagePath(String qrCode) {
        Path directory = Path.of(AppConstants.QR_DIRECTORY);
        return directory.resolve("bike_" + sanitize(qrCode) + ".png");
    }

    /**
     * Writes the QR PNG if it does not already exist. Existing files are never overwritten.
     */
    public static Path writeImage(Bike bike) {
        try {
            Path directory = Path.of(AppConstants.QR_DIRECTORY);
            Files.createDirectories(directory);
            Path target = imagePath(bike.getQrCode());
            if (Files.exists(target)) {
                return target;
            }
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );
            BitMatrix matrix = new QRCodeWriter().encode(payloadFor(bike), BarcodeFormat.QR_CODE, SIZE, SIZE, hints);
            MatrixToImageWriter.writeToPath(matrix, "PNG", target);
            return target;
        } catch (WriterException | IOException ex) {
            throw new QRException("Unable to generate the QR image for this bike.", ex);
        }
    }

    private static String sanitize(String qrCode) {
        return qrCode.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
