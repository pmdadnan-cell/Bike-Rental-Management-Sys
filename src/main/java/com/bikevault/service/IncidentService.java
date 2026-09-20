package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.IncidentDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.model.Incident;
import com.bikevault.model.Rental;
import com.bikevault.util.ValidationUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Accident / damage / theft incident reporting and review.
 */
public class IncidentService {

    private final IncidentDAO incidentDAO;
    private final RentalDAO rentalDAO;
    private final AuditLogDAO auditLogDAO;

    public IncidentService() {
        this(new IncidentDAO(), new RentalDAO(), new AuditLogDAO());
    }

    public IncidentService(IncidentDAO incidentDAO, RentalDAO rentalDAO, AuditLogDAO auditLogDAO) {
        this.incidentDAO = incidentDAO;
        this.rentalDAO = rentalDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public List<Incident> findAll() {
        SessionManager.requireOperator();
        return incidentDAO.findAll();
    }

    public List<Incident> findMine() {
        return incidentDAO.findByCustomerId(SessionManager.requireCustomerId());
    }

    public List<Incident> search(List<Incident> source, String query, String status) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(row -> status == null || "ALL".equals(status) || status.equalsIgnoreCase(row.getStatus()))
                .filter(row -> needle.isEmpty()
                        || contains(row.getCustomerName(), needle)
                        || contains(row.getBikeLabel(), needle)
                        || contains(row.getIncidentType(), needle)
                        || contains(row.getDescription(), needle)
                        || contains(String.valueOf(row.getId()), needle))
                .sorted(Comparator.comparing(Incident::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Incident report(Incident incident) {
        if (SessionManager.isCustomer()) {
            long customerId = SessionManager.requireCustomerId();
            incident.setCustomerId(customerId);
            Rental active = rentalDAO.findActiveByCustomerId(customerId).orElse(null);
            if (incident.getRentalId() == null && active != null) {
                incident.setRentalId(active.getId());
                incident.setBikeId(active.getBikeId());
            }
        } else {
            SessionManager.requireOperator();
        }
        incident.setIncidentType(ValidationUtil.requireText(incident.getIncidentType(), "Incident type"));
        incident.setDescription(ValidationUtil.requireText(incident.getDescription(), "Description"));
        if (incident.getIncidentDate() == null) {
            incident.setIncidentDate(LocalDate.now());
        }
        if (incident.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        incident.setStatus(AppConstants.STATUS_REPORTED);
        Incident saved = incidentDAO.insert(incident);
        audit(AppConstants.ACTION_INCIDENT_REPORTED,
                "Incident #" + saved.getId() + " " + saved.getIncidentType());
        return saved;
    }

    public boolean review(Incident incident, String status) {
        SessionManager.requireOperator();
        if (incident.getId() == null) {
            throw new IllegalArgumentException("Select an incident to review.");
        }
        incident.setStatus(ValidationUtil.requireText(status, "Status"));
        return incidentDAO.update(incident);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void audit(String action, String description) {
        Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
        auditLogDAO.insert(userId, action, description);
    }
}
