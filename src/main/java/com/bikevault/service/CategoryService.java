package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.CategoryDAO;
import com.bikevault.model.BikeCategory;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Bike category maintenance.
 */
public class CategoryService {

    private final CategoryDAO categoryDAO;
    private final AuditLogDAO auditLogDAO;
    private final BikeDAO bikeDAO;

    public CategoryService() {
        this(new CategoryDAO(), new AuditLogDAO(), new BikeDAO());
    }

    public CategoryService(CategoryDAO categoryDAO, AuditLogDAO auditLogDAO, BikeDAO bikeDAO) {
        this.categoryDAO = categoryDAO;
        this.auditLogDAO = auditLogDAO;
        this.bikeDAO = bikeDAO;
    }

    public List<BikeCategory> findAll() {
        return categoryDAO.findAll();
    }

    public List<BikeCategory> findActive() {
        return categoryDAO.findActive();
    }

    public List<BikeCategory> search(List<BikeCategory> source, String query) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(category -> needle.isEmpty()
                        || contains(category.getName(), needle)
                        || contains(category.getDescription(), needle))
                .sorted(Comparator.comparing(BikeCategory::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public BikeCategory add(BikeCategory category) {
        SessionManager.requireOperator();
        validate(category);
        BikeCategory saved = categoryDAO.insert(category);
        audit("ADD_CATEGORY", "Added category " + saved.getName());
        return saved;
    }

    public boolean update(BikeCategory category) {
        SessionManager.requireOperator();
        if (category.getId() == null) {
            throw new IllegalArgumentException("Select a category to update.");
        }
        validate(category);
        boolean updated = categoryDAO.update(category);
        if (updated) {
            audit("UPDATE_CATEGORY", "Updated category " + category.getName());
        }
        return updated;
    }

    public boolean delete(long id) {
        SessionManager.requireAdminRole();
        if (bikeDAO.countByCategory(id) > 0) {
            throw new IllegalStateException("Bikes are still assigned to this category, so it cannot be deleted.");
        }
        boolean deleted = categoryDAO.delete(id);
        if (deleted) {
            audit("DELETE_CATEGORY", "Deleted category #" + id);
        }
        return deleted;
    }

    private void validate(BikeCategory category) {
        category.setName(ValidationUtil.requireText(category.getName(), "Category name"));
        if (category.getLateFeePerDay() == null) {
            category.setLateFeePerDay(BigDecimal.ZERO);
        }
        if (category.getLateFeePerDay().signum() < 0) {
            throw new IllegalArgumentException("Late fee per day cannot be negative.");
        }
        category.setStatus(ValidationUtil.hasText(category.getStatus())
                ? category.getStatus() : AppConstants.STATUS_ACTIVE);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void audit(String action, String description) {
        Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
        auditLogDAO.insert(userId, action, description);
    }
}
