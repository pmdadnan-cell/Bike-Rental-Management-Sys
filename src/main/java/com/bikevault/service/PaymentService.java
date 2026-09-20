package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.PaymentDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.model.Payment;
import com.bikevault.model.Rental;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Standalone payment recording against rentals.
 */
public class PaymentService {

    private final PaymentDAO paymentDAO;
    private final RentalDAO rentalDAO;
    private final AuditLogDAO auditLogDAO;

    public PaymentService() {
        this(new PaymentDAO(), new RentalDAO(), new AuditLogDAO());
    }

    public PaymentService(PaymentDAO paymentDAO, RentalDAO rentalDAO, AuditLogDAO auditLogDAO) {
        this.paymentDAO = paymentDAO;
        this.rentalDAO = rentalDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public List<Payment> findAll() {
        SessionManager.requireOperator();
        return paymentDAO.findAll();
    }

    public List<Payment> findMine() {
        return paymentDAO.findByCustomerId(SessionManager.requireCustomerId());
    }

    public List<Rental> findRentals() {
        if (SessionManager.isCustomer()) {
            return rentalDAO.findByCustomerId(SessionManager.requireCustomerId());
        }
        SessionManager.requireOperator();
        return rentalDAO.findAll();
    }

    public List<Payment> search(List<Payment> source, String query) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(payment -> needle.isEmpty()
                        || contains(String.valueOf(payment.getRentalId()), needle)
                        || contains(payment.getCustomerName(), needle)
                        || contains(payment.getBikeLabel(), needle)
                        || contains(payment.getReferenceNumber(), needle)
                        || contains(payment.getMethod(), needle)
                        || contains(payment.getStatus(), needle))
                .sorted(Comparator.comparing(Payment::getPaidAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Payment record(Payment payment) {
        if (SessionManager.getCurrentUser() == null) {
            throw new IllegalStateException("Sign in before recording a payment.");
        }
        if (payment.getRentalId() == null) {
            throw new IllegalArgumentException("Select a rental.");
        }
        Rental rental = rentalDAO.findById(payment.getRentalId())
                .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
        if (SessionManager.isCustomer()
                && (rental.getCustomerId() == null
                || rental.getCustomerId() != SessionManager.requireCustomerId())) {
            throw new com.bikevault.exception.UnauthorizedException("You can only pay for your own rentals.");
        }
        ValidationUtil.requirePositive(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount(), "Amount");
        BigDecimal due = rental.getFinalAmount() != null ? rental.getFinalAmount() : rental.getTotalAmount();
        if (due == null) {
            due = BigDecimal.ZERO;
        }
        BigDecimal alreadyPaid = paymentDAO.sumCompletedByRental(rental.getId());
        BigDecimal remaining = due.subtract(alreadyPaid == null ? BigDecimal.ZERO : alreadyPaid);
        if (remaining.signum() <= 0) {
            throw new IllegalStateException("This rental is already fully paid. A second payment was not recorded.");
        }
        if (payment.getAmount().compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Amount exceeds the unpaid balance of ₹ "
                    + remaining.setScale(2, java.math.RoundingMode.HALF_UP) + ".");
        }
        payment.setMethod(ValidationUtil.requireText(payment.getMethod(), "Payment method"));
        payment.setStatus(ValidationUtil.hasText(payment.getStatus())
                ? payment.getStatus() : AppConstants.STATUS_COMPLETED);
        payment.setUserId(SessionManager.getCurrentUser().getId());
        if (!ValidationUtil.hasText(payment.getReferenceNumber())) {
            payment.setReferenceNumber("PAY-" + payment.getRentalId() + "-" + System.currentTimeMillis());
        }
        Payment saved = paymentDAO.insert(payment);
        auditLogDAO.insert(saved.getUserId(), AppConstants.ACTION_PAYMENT_COMPLETED,
                "Payment #" + saved.getId() + " of " + saved.getAmount() + " for rental #" + saved.getRentalId());
        return saved;
    }

    public Payment verify(long paymentId) {
        SessionManager.requireAdminRole();
        Payment payment = paymentDAO.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found."));
        if (AppConstants.STATUS_FAILED.equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("A failed payment cannot be verified.");
        }
        payment.setStatus(AppConstants.STATUS_COMPLETED);
        paymentDAO.update(payment);
        auditLogDAO.insert(SessionManager.getCurrentUser().getId(), AppConstants.ACTION_PAYMENT_VERIFIED,
                "Verified payment #" + payment.getId() + " for rental #" + payment.getRentalId());
        return payment;
    }

    public boolean delete(long id) {
        SessionManager.requireAdminRole();
        return paymentDAO.delete(id);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
