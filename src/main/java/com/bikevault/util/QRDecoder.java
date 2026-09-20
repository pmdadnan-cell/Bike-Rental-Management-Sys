package com.bikevault.util;

import com.bikevault.app.AppConstants;
import com.bikevault.exception.QRException;
import com.bikevault.model.QrPayload;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decodes camera frames and parses BIKEVAULT QR payloads. Never treats raw text as trusted identity.
 */
public final class QRDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(QRDecoder.class);

    private QRDecoder() {
    }

    public static Optional<String> decodeImage(BufferedImage image) {
        if (image == null) {
            return Optional.empty();
        }
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return Optional.ofNullable(result.getText());
        } catch (NotFoundException ex) {
            return Optional.empty();
        } catch (Exception ex) {
            LOG.warn("QR image decode failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public static QrPayload parse(String rawText) {
        String raw = ValidationUtil.trimToEmpty(rawText);
        if (raw.isEmpty()) {
            throw new QRException("Empty QR content.");
        }

        if (raw.startsWith("RENTAL-") || raw.startsWith("RVQR-")) {
            return new QrPayload(null, null, raw, raw);
        }

        if (raw.startsWith(AppConstants.APP_NAME + "|")) {
            Long bikeId = null;
            String qrCode = null;
            String rentalToken = null;
            String[] parts = raw.split("\\|");
            for (String part : parts) {
                if (part.startsWith("BIKE_ID=")) {
                    String value = part.substring("BIKE_ID=".length()).trim();
                    if (!value.isEmpty()) {
                        try {
                            bikeId = Long.parseLong(value);
                        } catch (NumberFormatException ex) {
                            throw new QRException("QR payload contains an invalid bike id.");
                        }
                    }
                } else if (part.startsWith("QR=")) {
                    qrCode = part.substring("QR=".length()).trim();
                } else if (part.startsWith("TOKEN=")) {
                    rentalToken = part.substring("TOKEN=".length()).trim();
                }
            }
            if (ValidationUtil.hasText(rentalToken)) {
                return new QrPayload(null, null, raw, rentalToken);
            }
            if (!ValidationUtil.hasText(qrCode)) {
                throw new QRException("QR payload is missing a bike or rental identity.");
            }
            return new QrPayload(bikeId, qrCode, raw);
        }

        if (raw.startsWith("BIKE-")) {
            return new QrPayload(null, raw, raw);
        }

        throw new QRException("Invalid or unregistered QR Code.");
    }
}
