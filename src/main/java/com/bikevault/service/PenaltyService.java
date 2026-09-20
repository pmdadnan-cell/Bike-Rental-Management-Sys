package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.PaymentDAO;
import com.bikevault.dao.PenaltyDAO;
import com.bikevault.database.DatabaseConnection;
import com.bikevault.exception.UnauthorizedException;
import com.bikevault.model.Payment;
import com.bikevault.model.Penalty;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Penalty records. Amounts can only be changed by operators.
 */
public class PenaltyService {

    private final PenaltyDAO penaltyDAO;
    private final PaymentDAO paymentDAO;
    private final AuditLogDAO auditLogDAO;

    public PenaltyService() {
        this(new PenaltyDAO(), new PaymentDAO(), new AuditLogDAO());
    }

    public PenaltyService(PenaltyDAO penaltyDAO, AuditLogDAO auditLogDAO) {
        this(penaltyDAO, new PaymentDAO(), auditLogDAO);
    }

    public PenaltyService(PenaltyDAO penaltyDAO, PaymentDAO paymentDAO, AuditLogDAO auditLogDAO) {
        this.penaltyDAO = penaltyDAO;
        this.paymentDAO = paymentDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public List<Penalty> findAll() {
        SessionManager.requireOperator();
        return penaltyDAO.findAll();
    }

    public List<Penalty> findMine() {
        return penaltyDAO.findByCustomerId(SessionManager.requireCustomerId());
    }

    public List<Penalty> search(List<Penalty> source, String query, String status) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(row -> status == null || "ALL".equals(status) || status.equalsIgnoreCase(row.getStatus()))
                .filter(row -> needle.isEmpty()
                        || contains(row.getCustomerName(), needle)
                        || contains(row.getBikeLabel(), needle)
                        || contains(row.getPenaltyType(), needle)
                        || contains(row.getDescription(), needle)
                        || contains(String.valueOf(row.getId()), needle)
                        || contains(String.valueOf(row.getRentalId()), needle))
                .sorted(Comparator.comparing(Penalty::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Penalty add(Penalty penalty) {
        SessionManager.requireOperator();
        validate(penalty);
        if (!ValidationUtil.hasText(penalty.getStatus())) {
            penalty.setStatus(AppConstants.STATUS_PENDING);
        }
        Penalty saved = penaltyDAO.insert(penalty);
        audit(AppConstants.ACTION_PENALTY_CREATED,
                "Penalty #" + saved.getId() + " " + saved.getPenaltyType() + " ₹" + saved.getAmount());
        return saved;
    }

    public boolean update(Penalty penalty) {
        SessionManager.requireOperator();
        if (penalty.getId() == null) {
            throw new IllegalArgumentException("Select a penalty to update.");
        }
        validate(penalty);
        return penaltyDAO.update(penalty);
    }

    public Penalty findOwnById(long penaltyId) {
        Penalty penalty = penaltyDAO.findById(penaltyId)
                .orElseThrow(() -> new IllegalArgumentException("Penalty was not found."));
        if (SessionManager.isCustomer() && (penalty.getCustomerId() == null
                || penalty.getCustomerId() != SessionManager.requireCustomerId())) {
            throw new UnauthorizedException("You can only view your own penalties.");
        }
        return penalty;
    }

    public Penalty payMine(long penaltyId, String method) {
        long customerId = SessionManager.requireCustomerId();
        Penalty penalty = findOwnById(penaltyId);
        if (!AppConstants.STATUS_PENDING.equalsIgnoreCase(penalty.getStatus())) {
            throw new IllegalStateException("This penalty is already " + penalty.getStatus() + ".");
        }
        if (penalty.getRentalId() == null) {
            throw new IllegalStateException("This penalty is not linked to a rental and cannot be paid here.");
        }
        Long userId = SessionManager.getCurrentUser().getId();
        audit(AppConstants.ACTION_PENALTY_PAYMENT_STARTED,
                "Penalty payment started for #" + penalty.getId());
        try {
            Penalty saved = DatabaseConnection.getInstance().inTransaction(connection -> {
                if (AppConstants.STATUS_PAID.equalsIgnoreCase(penalty.getStatus())) {
                    throw new IllegalStateException("This penalty is already paid.");
                }
                Payment payment = new Payment();
                payment.setRentalId(penalty.getRentalId());
                payment.setUserId(userId);
                payment.setAmount(penalty.getAmount());
                payment.setMethod(ValidationUtil.requireText(method, "Payment method"));
                payment.setStatus(AppConstants.STATUS_COMPLETED);
                payment.setReferenceNumber("PAY-PEN-" + penalty.getId() + "-" + System.currentTimeMillis());
                payment.setNotes("PENALTY PAYMENT #" + penalty.getId() + " · " + penalty.getPenaltyType());
                paymentDAO.insert(connection, payment);
                penalty.setStatus(AppConstants.STATUS_PAID);
                penalty.setResolvedAt(LocalDateTime.now());
                penaltyDAO.update(connection, penalty);
                auditLogDAO.insert(connection, userId, AppConstants.ACTION_PENALTY_PAYMENT_COMPLETED,
                        "Penalty #" + penalty.getId() + " paid ₹" + penalty.getAmount()
                                + " for rental #" + penalty.getRentalId() + " by customer #" + customerId);
                return penalty;
            });
            return saved;
        } catch (RuntimeException ex) {
            audit(AppConstants.ACTION_PENALTY_PAYMENT_FAILED,
                    "Penalty payment failed for #" + penalty.getId() + " — " + ex.getMessage());
            throw ex;
        }
    }

    public boolean markPaid(Penalty penalty) {
        SessionManager.requireOperator();
        penalty.setStatus(AppConstants.STATUS_PAID);
        penalty.setResolvedAt(LocalDateTime.now());
        return penaltyDAO.update(penalty);
    }

    public boolean waive(Penalty penalty) {
        SessionManager.requireOperator();
        penalty.setStatus(AppConstants.STATUS_WAIVED);
        penalty.setResolvedAt(LocalDateTime.now());
        return penaltyDAO.update(penalty);
    }

    public void rejectCustomerMutation() {
        if (SessionManager.isCustomer()) {
            throw new UnauthorizedException("Customers cannot modify official penalty amounts or status.");
        }
    }

    private void validate(Penalty penalty) {
        penalty.setPenaltyType(ValidationUtil.requireText(penalty.getPenaltyType(), "Penalty type"));
        penalty.setDescription(ValidationUtil.requireText(penalty.getDescription(), "Description"));
        if (penalty.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        ValidationUtil.requirePositive(penalty.getAmount() == null ? BigDecimal.ZERO : penalty.getAmount(), "Amount");
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void audit(String action, String description) {
        Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
        auditLogDAO.insert(userId, action, description);
    }
}
