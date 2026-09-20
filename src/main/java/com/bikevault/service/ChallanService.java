package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.ChallanDAO;
import com.bikevault.exception.UnauthorizedException;
import com.bikevault.model.Challan;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Traffic challan records. Official amount/status is operator-controlled.
 */
public class ChallanService {

    private final ChallanDAO challanDAO;
    private final AuditLogDAO auditLogDAO;

    public ChallanService() {
        this(new ChallanDAO(), new AuditLogDAO());
    }

    public ChallanService(ChallanDAO challanDAO, AuditLogDAO auditLogDAO) {
        this.challanDAO = challanDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public List<Challan> findAll() {
        SessionManager.requireOperator();
        return challanDAO.findAll();
    }

    public List<Challan> findMine() {
        return challanDAO.findByCustomerId(SessionManager.requireCustomerId());
    }

    public List<Challan> search(List<Challan> source, String query, String status) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(row -> status == null || "ALL".equals(status) || status.equalsIgnoreCase(row.getStatus()))
                .filter(row -> needle.isEmpty()
                        || contains(row.getCustomerName(), needle)
                        || contains(row.getBikeLabel(), needle)
                        || contains(row.getDescription(), needle)
                        || contains(String.valueOf(row.getId()), needle))
                .sorted(Comparator.comparing(Challan::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Challan add(Challan challan) {
        SessionManager.requireOperator();
        validate(challan);
        if (challan.getChallanDate() == null) {
            challan.setChallanDate(LocalDate.now());
        }
        if (!ValidationUtil.hasText(challan.getStatus())) {
            challan.setStatus(AppConstants.STATUS_PENDING);
        }
        Challan saved = challanDAO.insert(challan);
        audit(AppConstants.ACTION_CHALLAN_CREATED, "Challan #" + saved.getId() + " ₹" + saved.getAmount());
        return saved;
    }

    public boolean update(Challan challan) {
        SessionManager.requireOperator();
        if (challan.getId() == null) {
            throw new IllegalArgumentException("Select a challan to update.");
        }
        validate(challan);
        return challanDAO.update(challan);
    }

    public void rejectCustomerMutation() {
        if (SessionManager.isCustomer()) {
            throw new UnauthorizedException("Customers cannot change official challan amounts or status.");
        }
    }

    private void validate(Challan challan) {
        if (challan.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        challan.setDescription(ValidationUtil.requireText(challan.getDescription(), "Description"));
        ValidationUtil.requirePositive(challan.getAmount() == null ? BigDecimal.ZERO : challan.getAmount(), "Amount");
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void audit(String action, String description) {
        Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
        auditLogDAO.insert(userId, action, description);
    }
}
