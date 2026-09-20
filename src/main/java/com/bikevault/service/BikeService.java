package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.model.Bike;
import com.bikevault.util.QRGenerator;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Bike registry, including automatic unique QR identity assignment.
 */
public class BikeService {

    private final BikeDAO bikeDAO;
    private final QRService qrService;
    private final AuditLogDAO auditLogDAO;
    private final RentalDAO rentalDAO;

    public BikeService() {
        this(new BikeDAO(), new QRService(), new AuditLogDAO(), new RentalDAO());
    }

    public BikeService(BikeDAO bikeDAO, QRService qrService, AuditLogDAO auditLogDAO, RentalDAO rentalDAO) {
        this.bikeDAO = bikeDAO;
        this.qrService = qrService;
        this.auditLogDAO = auditLogDAO;
        this.rentalDAO = rentalDAO;
    }

    public List<Bike> findAll() {
        return bikeDAO.findAll();
    }

    public List<Bike> search(List<Bike> source, String query, String status) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(bike -> status == null || "ALL".equals(status) || status.equalsIgnoreCase(bike.getStatus()))
                .filter(bike -> needle.isEmpty() || matches(bike, needle))
                .sorted(Comparator.comparing(Bike::getBrand, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Bike::getModel, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Bike> catalog(String query, String category, String availability, String color,
                              BigDecimal minRate, BigDecimal maxRate) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        String cat = ValidationUtil.trimToEmpty(category);
        String avail = ValidationUtil.trimToEmpty(availability);
        String colour = ValidationUtil.trimToEmpty(color).toLowerCase(Locale.ROOT);
        return bikeDAO.findAll().stream()
                .filter(bike -> needle.isEmpty() || matches(bike, needle))
                .filter(bike -> cat.isEmpty() || "ALL".equalsIgnoreCase(cat)
                        || cat.equalsIgnoreCase(bike.getCategoryName()))
                .filter(bike -> avail.isEmpty() || "ALL".equalsIgnoreCase(avail)
                        || avail.equalsIgnoreCase(bike.getStatus()))
                .filter(bike -> colour.isEmpty() || contains(bike.getColor(), colour))
                .filter(bike -> minRate == null || bike.getDailyRate() == null
                        || bike.getDailyRate().compareTo(minRate) >= 0)
                .filter(bike -> maxRate == null || bike.getDailyRate() == null
                        || bike.getDailyRate().compareTo(maxRate) <= 0)
                .sorted(Comparator.comparing(Bike::getBrand, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Bike::getModel, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public Bike add(Bike bike) {
        SessionManager.requireOperator();
        validate(bike, true);
        if (!ValidationUtil.hasText(bike.getQrCode())) {
            bike.setQrCode(qrService.allocateUniqueIdentity());
        }
        if (!ValidationUtil.hasText(bike.getStatus())) {
            bike.setStatus(AppConstants.BIKE_AVAILABLE);
        }
        if (!ValidationUtil.hasText(bike.getImagePath())) {
            bike.setImagePath(com.bikevault.util.BikeImageLoader.pathFor(bike.getBrand(), bike.getModel()));
        }
        Bike saved = bikeDAO.insert(bike);
        qrService.generateImage(saved);
        audit(AppConstants.ACTION_ADD_BIKE,
                "Added " + saved.displayId() + " with QR " + saved.getQrCode());
        return saved;
    }

    public boolean update(Bike bike) {
        SessionManager.requireOperator();
        if (bike.getId() == null) {
            throw new IllegalArgumentException("Select a bike to update.");
        }
        validate(bike, false);
        Bike existing = bikeDAO.findById(bike.getId())
                .orElseThrow(() -> new IllegalArgumentException("Bike was not found."));
        bike.setQrCode(existing.getQrCode());
        if (rentalDAO.findActiveByBikeId(existing.getId()).isPresent()
                && !AppConstants.BIKE_RENTED.equalsIgnoreCase(bike.getStatus())) {
            throw new IllegalStateException("This bike has an active rental. Complete the return before changing status.");
        }
        boolean updated = bikeDAO.update(bike);
        if (updated) {
            audit(AppConstants.ACTION_UPDATE_BIKE, "Updated " + existing.displayId());
        }
        return updated;
    }

    public boolean delete(long id) {
        SessionManager.requireOperator();
        SessionManager.requireAdminRole();
        Bike existing = bikeDAO.findById(id).orElse(null);
        if (existing != null && rentalDAO.findActiveByBikeId(id).isPresent()) {
            throw new IllegalStateException("This bike has an active rental and cannot be deleted.");
        }
        boolean deleted = bikeDAO.delete(id);
        if (deleted && existing != null) {
            audit(AppConstants.ACTION_DELETE_BIKE, "Deleted " + existing.displayId());
        }
        return deleted;
    }

    public Path generateQrImage(Bike bike) {
        if (bike == null || !ValidationUtil.hasText(bike.getQrCode())) {
            throw new IllegalArgumentException("Save the bike first so a QR identity exists.");
        }
        Bike persisted = bike.getId() == null ? bike : bikeDAO.findById(bike.getId()).orElse(bike);
        return qrService.generateImage(persisted);
    }

    public Path qrImagePath(String qrCode) {
        return QRGenerator.imagePath(qrCode);
    }

    private void validate(Bike bike, boolean creating) {
        bike.setBrand(ValidationUtil.requireText(bike.getBrand(), "Brand"));
        bike.setModel(ValidationUtil.requireText(bike.getModel(), "Model"));
        bike.setRegistrationNumber(ValidationUtil.requireText(bike.getRegistrationNumber(), "Registration number")
                .toUpperCase(Locale.ROOT));
        if (bike.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        bike.setColor(ValidationUtil.requireText(bike.getColor(), "Color"));
        ValidationUtil.requirePositive(bike.getDailyRate() == null ? BigDecimal.ZERO : bike.getDailyRate(),
                "Daily rental rate");
        if (creating && ValidationUtil.hasText(bike.getQrCode())
                && bikeDAO.findByQrCode(bike.getQrCode()).isPresent()) {
            throw new IllegalArgumentException("QR identity must be unique.");
        }
        bikeDAO.findByRegistrationNumber(bike.getRegistrationNumber())
                .filter(other -> creating || other.getId() == null || !other.getId().equals(bike.getId()))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Registration number must be unique.");
                });
    }

    private boolean matches(Bike bike, String needle) {
        return contains(bike.displayId(), needle)
                || contains(bike.getQrCode(), needle)
                || contains(bike.getRegistrationNumber(), needle)
                || contains(bike.getModel(), needle)
                || contains(bike.getBrand(), needle)
                || contains(String.valueOf(bike.getId()), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void audit(String action, String description) {
        Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
        auditLogDAO.insert(userId, action, description);
    }
}
