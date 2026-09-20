package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.ScanContext;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.exception.QRException;
import com.bikevault.model.Bike;
import com.bikevault.model.QrPayload;
import com.bikevault.model.QrVerification;
import com.bikevault.model.Rental;
import com.bikevault.util.QRDecoder;
import com.bikevault.util.QRGenerator;
import com.bikevault.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * QR identity lifecycle: generate, persist lookup, and verify scanned text against MySQL.
 */
public class QRService {

    private static final Logger LOG = LoggerFactory.getLogger(QRService.class);

    private final BikeDAO bikeDAO;
    private final RentalDAO rentalDAO;
    private final AuditLogDAO auditLogDAO;

    public QRService() {
        this(new BikeDAO(), new RentalDAO(), new AuditLogDAO());
    }

    public QRService(BikeDAO bikeDAO, AuditLogDAO auditLogDAO) {
        this(bikeDAO, new RentalDAO(), auditLogDAO);
    }

    public QRService(BikeDAO bikeDAO, RentalDAO rentalDAO, AuditLogDAO auditLogDAO) {
        this.bikeDAO = bikeDAO;
        this.rentalDAO = rentalDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public String allocateUniqueIdentity() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String identity = QRGenerator.newIdentity();
            if (bikeDAO.findByQrCode(identity).isEmpty()) {
                return identity;
            }
        }
        throw new QRException("Unable to allocate a unique QR identity.");
    }

    public Path generateImage(Bike bike) {
        Path path = QRGenerator.writeImage(bike);
        auditQuietly(AppConstants.ACTION_GENERATE_QR,
                "Generated QR image for " + bike.displayId() + " / " + bike.getQrCode());
        return path;
    }

    /**
     * Parse → validate → database lookup → verify. The numeric id in the payload is never trusted alone.
     */
    public QrVerification verify(String rawText) {
        QrPayload payload;
        try {
            payload = QRDecoder.parse(rawText);
        } catch (QRException ex) {
            String message = ex.getMessage() == null ? "Invalid BikeVault rental QR." : ex.getMessage();
            if (rawLooksLikeRental(rawText) || message.toLowerCase().contains("rental")) {
                return QrVerification.invalid("Invalid BikeVault rental QR.");
            }
            return QrVerification.invalid(message);
        }

        if (payload.isRentalQr()) {
            return verifyRental(payload);
        }

        Optional<Bike> found = bikeDAO.findByQrCode(payload.getQrCode());
        if (found.isEmpty()) {
            return QrVerification.invalid("Invalid or unregistered Bike QR Code.");
        }

        Bike bike = found.get();
        if (payload.getBikeId() != null && bike.getId() != null && !payload.getBikeId().equals(bike.getId())) {
            return QrVerification.invalid("Invalid or unregistered Bike QR Code.");
        }
        if (AppConstants.BIKE_INACTIVE.equalsIgnoreCase(bike.getStatus())) {
            auditQuietly(AppConstants.ACTION_SCAN_QR, "Inactive bike scanned: " + bike.getQrCode());
            return QrVerification.inactive(bike, payload);
        }

        auditQuietly(AppConstants.ACTION_SCAN_QR,
                "Verified " + bike.displayId() + " / " + bike.getQrCode() + " status " + bike.getStatus());
        LOG.info("QR verified for bike {} ({})", bike.displayId(), bike.getQrCode());
        return QrVerification.verified(bike, payload);
    }

    public Optional<Bike> findByQr(String qrCode) {
        if (!ValidationUtil.hasText(qrCode)) {
            return Optional.empty();
        }
        return bikeDAO.findByQrCode(qrCode.trim());
    }

    private QrVerification verifyRental(QrPayload payload) {
        if (!ValidationUtil.hasText(payload.getRentalToken())) {
            return QrVerification.invalid("Invalid BikeVault rental QR.");
        }
        Optional<Rental> found = rentalDAO.findByToken(payload.getRentalToken());
        if (found.isEmpty()) {
            return QrVerification.invalid("Rental not found.");
        }
        Rental rental = found.get();
        Bike bike = bikeDAO.findById(rental.getBikeId()).orElse(null);
        boolean returning = ScanContext.isReturnMode();
        auditQuietly(returning ? AppConstants.ACTION_RETURN_QR_SCANNED : AppConstants.ACTION_RENTAL_QR_SCANNED,
                (returning ? "Return scan for " : "Scanned rental token for ") + rental.displayId());
        String status = rental.getStatus() == null ? "" : rental.getStatus().toUpperCase();
        String message = returning ? returnMessage(status) : approvalMessage(status);
        return QrVerification.rental(rental, bike, payload, message);
    }

    private String approvalMessage(String status) {
        return switch (status) {
            case "PENDING_APPROVAL" -> "✓ QR VERIFIED";
            case "ACTIVE", "RETURN_REQUESTED" -> "This rental has already been approved.";
            case "COMPLETED" -> "This rental is already completed.";
            case "REJECTED" -> "This rental was rejected.";
            default -> "✓ QR VERIFIED";
        };
    }

    private String returnMessage(String status) {
        return switch (status) {
            case "PENDING_APPROVAL" -> "This rental has not been approved yet.";
            case "ACTIVE" -> "Customer must request a return before completing the return.";
            case "RETURN_REQUESTED" -> "✓ QR VERIFIED";
            case "COMPLETED" -> "This rental has already been completed.";
            case "REJECTED" -> "This rental was rejected.";
            default -> "✓ QR VERIFIED";
        };
    }

    private boolean rawLooksLikeRental(String rawText) {
        String raw = rawText == null ? "" : rawText.toUpperCase();
        return raw.contains("RENTAL") || raw.contains("TOKEN=") || raw.startsWith("RVQR-");
    }

    private void auditQuietly(String action, String description) {
        try {
            Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
            auditLogDAO.insert(userId, action, description);
        } catch (RuntimeException ex) {
            LOG.warn("QR audit log could not be written", ex);
        }
    }
}
