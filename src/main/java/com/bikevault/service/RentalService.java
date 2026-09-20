package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.CategoryDAO;
import com.bikevault.dao.ChallanDAO;
import com.bikevault.dao.CustomerDAO;
import com.bikevault.dao.IncidentDAO;
import com.bikevault.dao.PaymentDAO;
import com.bikevault.dao.PenaltyDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.dao.ReturnDAO;
import com.bikevault.database.DatabaseConnection;
import com.bikevault.exception.UnauthorizedException;
import com.bikevault.model.Bike;
import com.bikevault.model.BikeCategory;
import com.bikevault.model.Challan;
import com.bikevault.model.Customer;
import com.bikevault.model.Incident;
import com.bikevault.model.Payment;
import com.bikevault.model.Penalty;
import com.bikevault.model.Rental;
import com.bikevault.model.ReturnRecord;
import com.bikevault.model.TripDetail;
import com.bikevault.util.PricingCalculator;
import com.bikevault.util.QRGenerator;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Rental creation, approval, QR tokens, pricing, and returns.
 */
public class RentalService {

    private final RentalDAO rentalDAO;
    private final BikeDAO bikeDAO;
    private final CustomerDAO customerDAO;
    private final CategoryDAO categoryDAO;
    private final ReturnDAO returnDAO;
    private final PaymentDAO paymentDAO;
    private final PenaltyDAO penaltyDAO;
    private final IncidentDAO incidentDAO;
    private final ChallanDAO challanDAO;
    private final AuditLogDAO auditLogDAO;

    public RentalService() {
        this(new RentalDAO(), new BikeDAO(), new CustomerDAO(), new CategoryDAO(),
                new ReturnDAO(), new PaymentDAO(), new PenaltyDAO(), new IncidentDAO(),
                new ChallanDAO(), new AuditLogDAO());
    }

    public RentalService(RentalDAO rentalDAO, BikeDAO bikeDAO, CustomerDAO customerDAO,
                         CategoryDAO categoryDAO, ReturnDAO returnDAO, PaymentDAO paymentDAO,
                         AuditLogDAO auditLogDAO) {
        this(rentalDAO, bikeDAO, customerDAO, categoryDAO, returnDAO, paymentDAO, new PenaltyDAO(),
                new IncidentDAO(), new ChallanDAO(), auditLogDAO);
    }

    public RentalService(RentalDAO rentalDAO, BikeDAO bikeDAO, CustomerDAO customerDAO,
                         CategoryDAO categoryDAO, ReturnDAO returnDAO, PaymentDAO paymentDAO,
                         PenaltyDAO penaltyDAO, AuditLogDAO auditLogDAO) {
        this(rentalDAO, bikeDAO, customerDAO, categoryDAO, returnDAO, paymentDAO, penaltyDAO,
                new IncidentDAO(), new ChallanDAO(), auditLogDAO);
    }

    public RentalService(RentalDAO rentalDAO, BikeDAO bikeDAO, CustomerDAO customerDAO,
                         CategoryDAO categoryDAO, ReturnDAO returnDAO, PaymentDAO paymentDAO,
                         PenaltyDAO penaltyDAO, IncidentDAO incidentDAO, ChallanDAO challanDAO,
                         AuditLogDAO auditLogDAO) {
        this.rentalDAO = rentalDAO;
        this.bikeDAO = bikeDAO;
        this.customerDAO = customerDAO;
        this.categoryDAO = categoryDAO;
        this.returnDAO = returnDAO;
        this.paymentDAO = paymentDAO;
        this.penaltyDAO = penaltyDAO;
        this.incidentDAO = incidentDAO;
        this.challanDAO = challanDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public List<Rental> findAll() {
        SessionManager.requireOperator();
        return enrichAll(rentalDAO.findAll());
    }

    public List<Rental> findPendingApprovals() {
        return findByStatus(AppConstants.STATUS_PENDING_APPROVAL);
    }

    public List<Rental> findByStatus(String status) {
        SessionManager.requireOperator();
        return enrichAll(rentalDAO.findByStatus(status));
    }

    public List<Rental> findMine() {
        return enrichAll(rentalDAO.findByCustomerId(SessionManager.requireCustomerId()));
    }

    public Rental findOwnActive() {
        return rentalDAO.findActiveByCustomerId(SessionManager.requireCustomerId())
                .map(this::enrich)
                .orElse(null);
    }

    public Rental findByToken(String token) {
        Rental rental = rentalDAO.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Rental QR token was not found."));
        if (SessionManager.isCustomer() && (rental.getCustomerId() == null
                || rental.getCustomerId() != SessionManager.requireCustomerId())) {
            throw new UnauthorizedException("You can only view your own rental QR.");
        }
        return enrich(rental);
    }

    public Rental findOwnById(long rentalId) {
        Rental rental = rentalDAO.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
        if (SessionManager.isCustomer() && (rental.getCustomerId() == null
                || rental.getCustomerId() != SessionManager.requireCustomerId())) {
            throw new UnauthorizedException("You can only view your own rentals.");
        }
        if (SessionManager.isOperator()) {
            SessionManager.requireOperator();
        }
        return enrich(rental);
    }

    public TripDetail loadTrip(long rentalId) {
        Rental rental = findOwnById(rentalId);
        TripDetail detail = new TripDetail();
        detail.setRental(rental);
        if (rental.getBikeId() != null) {
            detail.setBike(bikeDAO.findById(rental.getBikeId()).orElse(null));
        }
        List<Payment> payments = paymentDAO.findByRentalId(rentalId);
        detail.setLatestPayment(payments.stream()
                .max(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null));
        detail.setPaidAmount(paymentDAO.sumCompletedByRental(rentalId));
        List<Penalty> penalties = penaltyDAO.findByRentalId(rentalId);
        List<Challan> challans = challanDAO.findByRentalId(rentalId);
        if (SessionManager.isCustomer()) {
            long customerId = SessionManager.requireCustomerId();
            penalties = penalties.stream()
                    .filter(item -> item.getCustomerId() != null && item.getCustomerId() == customerId)
                    .collect(Collectors.toList());
            challans = challans.stream()
                    .filter(item -> item.getCustomerId() != null && item.getCustomerId() == customerId)
                    .collect(Collectors.toList());
        }
        detail.setPenalties(penalties);
        detail.setChallans(challans);
        detail.setPenaltyTotal(penalties.stream()
                .map(penalty -> penalty.getAmount() == null ? BigDecimal.ZERO : penalty.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(challans.stream()
                        .filter(challan -> !"WAIVED".equalsIgnoreCase(challan.getStatus()))
                        .map(challan -> challan.getAmount() == null ? BigDecimal.ZERO : challan.getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));
        List<Incident> incidents = incidentDAO.findByRentalId(rentalId);
        if (SessionManager.isCustomer()) {
            long customerId = SessionManager.requireCustomerId();
            incidents = incidents.stream()
                    .filter(item -> item.getCustomerId() != null && item.getCustomerId() == customerId)
                    .collect(Collectors.toList());
        }
        detail.setIncidents(incidents);
        detail.setTimeline(buildTimeline(rental, detail.getLatestPayment()));
        return detail;
    }

    public List<ReturnRecord> findReturns() {
        SessionManager.requireOperator();
        return returnDAO.findAll();
    }

    public List<Rental> search(List<Rental> source, String query, String status) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(rental -> status == null || "ALL".equals(status)
                        || status.equalsIgnoreCase(rental.getStatus())
                        || status.equalsIgnoreCase(rental.displayStatus()))
                .filter(rental -> needle.isEmpty() || matches(rental, needle))
                .sorted(Comparator.comparing(Rental::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Rental quote(Rental draft, Bike bike) {
        if (bike == null) {
            throw new IllegalArgumentException("Scan or select a bike first.");
        }
        if (draft.getStartDate() == null || draft.getExpectedReturnDate() == null) {
            throw new IllegalArgumentException("Select a start date and expected return date.");
        }
        if (draft.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (draft.getExpectedReturnDate().isBefore(draft.getStartDate())) {
            throw new IllegalArgumentException("Return date cannot be before the start date.");
        }
        int days = PricingCalculator.rentalDays(draft.getStartDate(), draft.getExpectedReturnDate());
        BigDecimal total = PricingCalculator.rentalAmount(bike.getDailyRate(), days);
        BigDecimal deposit = PricingCalculator.securityDeposit(bike.getDailyRate());
        draft.setBikeId(bike.getId());
        draft.setBikeLabel((bike.getBrand() == null ? "" : bike.getBrand()) + " " + (bike.getModel() == null ? "" : bike.getModel()));
        draft.setDurationDays(days);
        draft.setDailyRate(bike.getDailyRate());
        draft.setTotalAmount(total);
        draft.setLateFee(BigDecimal.ZERO);
        if (SessionManager.isCustomer()) {
            draft.setSecurityDeposit(BigDecimal.ZERO);
            draft.setFinalAmount(total);
        } else {
            draft.setSecurityDeposit(deposit);
            draft.setFinalAmount(PricingCalculator.payable(total, deposit, BigDecimal.ZERO));
        }
        return draft;
    }

    public Rental createRental(Rental draft, Bike scannedBike, String paymentMethod, boolean collectPayment) {
        if (SessionManager.getCurrentUser() == null) {
            throw new IllegalStateException("Sign in before creating a rental.");
        }
        if (SessionManager.isCustomer()) {
            draft.setCustomerId(SessionManager.requireCustomerId());
        }
        if (draft.getCustomerId() == null) {
            throw new IllegalArgumentException("Select a customer.");
        }
        Customer customer = customerDAO.findById(draft.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));
        if (!AppConstants.STATUS_ACTIVE.equalsIgnoreCase(customer.getStatus())) {
            throw new IllegalStateException("This customer is inactive and cannot rent.");
        }
        if (SessionManager.isCustomer() && (customer.getId() == null
                || customer.getId() != SessionManager.requireCustomerId())) {
            throw new UnauthorizedException("You can only rent for your own account.");
        }

        boolean customerRequest = SessionManager.isCustomer();
        draft.setUserId(SessionManager.getCurrentUser().getId());
        draft.setActualReturnDate(null);
        if (customerRequest) {
            draft.setStatus(AppConstants.STATUS_PENDING_APPROVAL);
            draft.setRentalQrStatus(AppConstants.STATUS_PENDING_APPROVAL);
        } else {
            SessionManager.requireOperator();
            draft.setStatus(AppConstants.STATUS_ACTIVE);
            draft.setRentalQrStatus(AppConstants.STATUS_ACTIVE);
        }

        Rental saved = DatabaseConnection.getInstance().inTransaction(connection -> {
            Bike locked = bikeDAO.findByIdForUpdate(connection, scannedBike.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Bike was not found."));
            assertRentable(locked);
            if (rentalDAO.findBlockingByBike(connection, locked.getId(), true).isPresent()) {
                throw new IllegalStateException("This bike already has a pending or active rental.");
            }
            if (rentalDAO.findOpenRequestByCustomer(connection, customer.getId(), true).isPresent()) {
                throw new IllegalStateException("This customer already has a pending or active rental.");
            }
            quote(draft, locked);
            draft.setRentalQrToken(allocateRentalToken());
            rentalDAO.insert(connection, draft);
            if (!customerRequest) {
                bikeDAO.updateStatus(connection, locked.getId(), AppConstants.BIKE_RENTED);
            }
            if (collectPayment) {
                Payment payment = new Payment();
                payment.setRentalId(draft.getId());
                payment.setUserId(draft.getUserId());
                payment.setAmount(draft.getFinalAmount());
                payment.setMethod(ValidationUtil.hasText(paymentMethod) ? paymentMethod : AppConstants.PAYMENT_CASH);
                payment.setStatus(AppConstants.STATUS_COMPLETED);
                payment.setReferenceNumber("PAY-R" + draft.getId() + "-" + System.currentTimeMillis());
                payment.setNotes(customerRequest ? "Customer rental payment" : "Rental advance");
                paymentDAO.insert(connection, payment);
            }
            if (customerRequest) {
                auditLogDAO.insert(connection, draft.getUserId(), AppConstants.ACTION_RENTAL_CREATED,
                        "Rental " + draft.displayId() + " created for " + customer.getFullName()
                                + " / " + locked.displayId());
                auditLogDAO.insert(connection, draft.getUserId(), AppConstants.ACTION_RENTAL_QR_GENERATED,
                        "Rental QR issued for " + draft.displayId());
            } else {
                auditLogDAO.insert(connection, draft.getUserId(), AppConstants.ACTION_CREATE_RENTAL,
                        "Rental " + draft.displayId() + " for " + customer.getFullName() + " / " + locked.displayId());
                auditLogDAO.insert(connection, draft.getUserId(), AppConstants.ACTION_BIKE_RENTED,
                        "Bike " + locked.displayId() + " rented");
            }
            if (collectPayment) {
                auditLogDAO.insert(connection, draft.getUserId(), AppConstants.ACTION_PAYMENT_COMPLETED,
                        "Payment completed for " + draft.displayId());
            }
            return draft;
        });
        return enrich(saved);
    }

    public Rental requestRentalAndPay(Rental draft, Bike bike, String paymentMethod) {
        if (!SessionManager.isCustomer()) {
            throw new UnauthorizedException("Only a customer can submit this rental request.");
        }
        return createRental(draft, bike, paymentMethod, true);
    }

    public Rental approve(long rentalId) {
        SessionManager.requireAdminRole();
        Long adminId = SessionManager.getCurrentUser().getId();
        Rental saved = DatabaseConnection.getInstance().inTransaction(connection -> {
            Rental rental = rentalDAO.findById(connection, rentalId, true)
                    .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
            if (!rental.canBeApproved()) {
                throw new IllegalStateException("THIS RENTAL QR IS NO LONGER VALID");
            }
            BigDecimal due = rental.getFinalAmount() == null ? BigDecimal.ZERO : rental.getFinalAmount();
            BigDecimal paid = paymentDAO.sumCompletedByRental(rental.getId());
            if (paid.compareTo(due) < 0) {
                throw new IllegalStateException("Payment is not complete. Admin cannot approve an unpaid rental.");
            }
            Bike locked = bikeDAO.findByIdForUpdate(connection, rental.getBikeId())
                    .orElseThrow(() -> new IllegalArgumentException("Bike was not found."));
            if (rentalDAO.hasOverlappingBooking(connection, locked.getId(), rental.getId(),
                    rental.getStartDate(), rental.getExpectedReturnDate())) {
                throw new IllegalStateException("Bike is no longer available for these dates.");
            }
            assertRentable(locked);
            rental.setStatus(AppConstants.STATUS_ACTIVE);
            rental.setRentalQrStatus(AppConstants.STATUS_ACTIVE);
            rental.setApprovedBy(adminId);
            rental.setApprovedAt(LocalDateTime.now());
            rentalDAO.update(connection, rental);
            bikeDAO.updateStatus(connection, locked.getId(), AppConstants.BIKE_RENTED);
            auditLogDAO.insert(connection, adminId, AppConstants.ACTION_RENTAL_APPROVED,
                    "Approved " + rental.displayId() + " — bike authorized for collection");
            auditLogDAO.insert(connection, adminId, AppConstants.ACTION_BIKE_COLLECTED,
                    "Bike " + locked.displayId() + " released for " + rental.displayId());
            return rental;
        });
        return enrich(saved);
    }

    public Rental reject(long rentalId, String reason, String notes) {
        SessionManager.requireAdminRole();
        Long adminId = SessionManager.getCurrentUser().getId();
        String rejection = ValidationUtil.requireText(reason, "Rejection reason");
        Rental saved = DatabaseConnection.getInstance().inTransaction(connection -> {
            Rental rental = rentalDAO.findById(connection, rentalId, true)
                    .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
            if (!AppConstants.STATUS_PENDING_APPROVAL.equalsIgnoreCase(rental.getStatus())) {
                throw new IllegalStateException("Only a pending request can be rejected.");
            }
            rental.setStatus(AppConstants.STATUS_REJECTED);
            rental.setRentalQrStatus(AppConstants.STATUS_REJECTED);
            rental.setRejectionReason(rejection);
            rental.setRejectionNotes(notes);
            rental.setApprovedBy(adminId);
            rental.setApprovedAt(LocalDateTime.now());
            rentalDAO.update(connection, rental);
            auditLogDAO.insert(connection, adminId, AppConstants.ACTION_RENTAL_REJECTED,
                    "Rejected " + rental.displayId() + " — " + rejection);
            return rental;
        });
        return enrich(saved);
    }

    public Rental requestReturn(long rentalId) {
        long customerId = SessionManager.requireCustomerId();
        Rental rental = rentalDAO.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
        if (rental.getCustomerId() == null || rental.getCustomerId() != customerId) {
            throw new UnauthorizedException("You can only return your own rental.");
        }
        if (!AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus())) {
            throw new IllegalStateException("Only an active rental can request a return.");
        }
        rental.setStatus(AppConstants.STATUS_RETURN_REQUESTED);
        rental.setReturnRequested(true);
        rental.setReturnRequestedAt(LocalDateTime.now());
        DatabaseConnection.getInstance().inTransaction(connection -> {
            rentalDAO.update(connection, rental);
            auditLogDAO.insert(connection, SessionManager.getCurrentUser().getId(),
                    AppConstants.ACTION_RETURN_REQUESTED, "Return requested for " + rental.displayId());
            return true;
        });
        return enrich(rental);
    }

    public Rental acceptReturn(long rentalId) {
        SessionManager.requireOperator();
        Rental rental = rentalDAO.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
        if (!AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())
                && !(AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus()) && rental.isReturnRequested())) {
            throw new IllegalStateException("Only a requested return can be accepted.");
        }
        Bike bike = bikeDAO.findById(rental.getBikeId())
                .orElseThrow(() -> new IllegalArgumentException("Bike was not found."));
        completeReturn(bike, LocalDate.now(),
                "Return accepted by " + SessionManager.displayName(), null, false, false);
        return enrich(rentalDAO.findById(rentalId).orElse(rental));
    }

    public Rental rejectReturn(long rentalId, String reason) {
        SessionManager.requireOperator();
        String note = ValidationUtil.requireText(reason, "Reason");
        Rental saved = DatabaseConnection.getInstance().inTransaction(connection -> {
            Rental rental = rentalDAO.findById(connection, rentalId, true)
                    .orElseThrow(() -> new IllegalArgumentException("Rental was not found."));
            if (!AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())) {
                throw new IllegalStateException("Only a requested return can be rejected.");
            }
            rental.setNotes("Return not accepted: " + note);
            rentalDAO.update(connection, rental);
            auditLogDAO.insert(connection, SessionManager.getCurrentUser().getId(),
                    AppConstants.ACTION_RETURN_REQUESTED,
                    "Return rejected for " + rental.displayId() + " — " + note);
            return rental;
        });
        return enrich(saved);
    }

    public int lateDaysFor(Rental rental) {
        if (rental == null || rental.getExpectedReturnDate() == null) {
            return 0;
        }
        return PricingCalculator.lateDays(rental.getExpectedReturnDate(), LocalDate.now());
    }

    public BigDecimal unpaidPenaltyTotal(long rentalId) {
        return penaltyDAO.findByRentalId(rentalId).stream()
                .filter(item -> AppConstants.STATUS_PENDING.equalsIgnoreCase(item.getStatus()))
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public ReturnRecord previewReturn(Bike bike, LocalDate returnDate) {
        Rental rental = rentalDAO.findActiveByBikeId(bike.getId())
                .orElseThrow(() -> new IllegalStateException("No active rental was found for this bike."));
        LocalDate actual = returnDate == null ? LocalDate.now() : returnDate;
        BikeCategory category = categoryDAO.findById(bike.getCategoryId()).orElse(null);
        BigDecimal latePerDay = category == null ? BigDecimal.ZERO : category.getLateFeePerDay();

        int actualDays = PricingCalculator.actualDays(rental.getStartDate(), actual);
        int late = PricingCalculator.lateDays(rental.getExpectedReturnDate(), actual);
        BigDecimal lateFee = PricingCalculator.lateFee(latePerDay, late);
        BigDecimal finalAmount = PricingCalculator.finalAmount(rental.getTotalAmount(), lateFee);

        ReturnRecord record = new ReturnRecord();
        record.setRentalId(rental.getId());
        record.setBikeId(bike.getId());
        record.setCustomerId(rental.getCustomerId());
        record.setCustomerName(rental.getCustomerName());
        record.setBikeLabel(rental.getBikeLabel());
        record.setReturnDate(actual);
        record.setActualDurationDays(actualDays);
        record.setLateDays(late);
        record.setLateFee(lateFee);
        record.setFinalAmount(finalAmount);
        record.setUserId(SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId());
        return record;
    }

    public Rental findActiveRental(long bikeId) {
        return rentalDAO.findActiveByBikeId(bikeId).map(this::enrich).orElse(null);
    }

    public ReturnRecord completeReturn(Bike bike, LocalDate returnDate, String conditionNotes,
                                       String paymentMethod, boolean collectLateFee) {
        return completeReturn(bike, returnDate, conditionNotes, paymentMethod, collectLateFee, true);
    }

    public ReturnRecord completeReturn(Bike bike, LocalDate returnDate, String conditionNotes,
                                       String paymentMethod, boolean collectLateFee, boolean automaticLatePenalty) {
        if (SessionManager.getCurrentUser() == null) {
            throw new IllegalStateException("Sign in before completing a return.");
        }
        if (SessionManager.isCustomer()) {
            throw new UnauthorizedException("Customers request a return. An administrator must verify the bike.");
        }
        SessionManager.requireOperator();
        ReturnRecord saved = DatabaseConnection.getInstance().inTransaction(connection -> {
            Bike locked = bikeDAO.findByIdForUpdate(connection, bike.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Bike was not found."));
            Rental rental = rentalDAO.findActiveByBikeId(connection, locked.getId(), true)
                    .orElseThrow(() -> new IllegalStateException("No active rental was found for this bike."));

            ReturnRecord preview = previewReturn(locked, returnDate);
            preview.setConditionNotes(conditionNotes);
            preview.setUserId(SessionManager.getCurrentUser().getId());

            rental.setActualReturnDate(preview.getReturnDate());
            rental.setDurationDays(preview.getActualDurationDays());
            if (automaticLatePenalty) {
                rental.setLateFee(preview.getLateFee());
                rental.setFinalAmount(preview.getFinalAmount());
            }
            rental.setStatus(AppConstants.STATUS_COMPLETED);
            rental.setRentalQrStatus(AppConstants.STATUS_COMPLETED);
            rental.setNotes(conditionNotes);
            rental.setReturnRequested(false);

            returnDAO.insert(connection, preview);
            rentalDAO.update(connection, rental);
            bikeDAO.updateStatus(connection, locked.getId(), AppConstants.BIKE_AVAILABLE);
            if (automaticLatePenalty && preview.getLateDays() != null && preview.getLateDays() > 0
                    && preview.getLateFee() != null && preview.getLateFee().signum() > 0) {
                Penalty penalty = new Penalty();
                penalty.setRentalId(rental.getId());
                penalty.setCustomerId(rental.getCustomerId());
                penalty.setBikeId(locked.getId());
                penalty.setPenaltyType("LATE_RETURN");
                penalty.setDescription("Late return of " + preview.getLateDays() + " day(s)");
                penalty.setAmount(preview.getLateFee());
                penalty.setStatus(AppConstants.STATUS_PENDING);
                penaltyDAO.insert(connection, penalty);
                auditLogDAO.insert(connection, preview.getUserId(), AppConstants.ACTION_PENALTY_CREATED,
                        "Late return penalty for " + rental.displayId());
            }
            if (collectLateFee && preview.getLateFee() != null && preview.getLateFee().signum() > 0) {
                Payment payment = new Payment();
                payment.setRentalId(rental.getId());
                payment.setUserId(preview.getUserId());
                payment.setAmount(preview.getLateFee());
                payment.setMethod(ValidationUtil.hasText(paymentMethod) ? paymentMethod : AppConstants.PAYMENT_CASH);
                payment.setStatus(AppConstants.STATUS_COMPLETED);
                payment.setReferenceNumber("PAY-LATE-" + rental.getId() + "-" + System.currentTimeMillis());
                payment.setNotes("Late fee");
                paymentDAO.insert(connection, payment);
            }
            auditLogDAO.insert(connection, preview.getUserId(), AppConstants.ACTION_RETURN_ACCEPTED,
                    "Return accepted for " + rental.displayId());
            return preview;
        });
        return saved;
    }

    public void assertRentable(Bike bike) {
        if (bike == null) {
            throw new IllegalArgumentException("Scan a bike first.");
        }
        String status = bike.getStatus();
        if (AppConstants.BIKE_RENTED.equalsIgnoreCase(status)) {
            throw new IllegalStateException("This bike is currently rented.");
        }
        if (AppConstants.BIKE_MAINTENANCE.equalsIgnoreCase(status)) {
            throw new IllegalStateException("This bike is in maintenance and cannot be rented.");
        }
        if (AppConstants.BIKE_INACTIVE.equalsIgnoreCase(status)) {
            throw new IllegalStateException("This bike is inactive.");
        }
        if (!AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(status)) {
            throw new IllegalStateException("This bike is not available for rental.");
        }
    }

    private List<Rental> enrichAll(List<Rental> rentals) {
        rentals.forEach(this::enrich);
        return rentals;
    }

    private Rental enrich(Rental rental) {
        if (rental == null || rental.getId() == null) {
            return rental;
        }
        List<Payment> payments = paymentDAO.findByRentalId(rental.getId());
        Payment latest = payments.stream()
                .max(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        BigDecimal paid = paymentDAO.sumCompletedByRental(rental.getId());
        BigDecimal due = rental.getFinalAmount() == null ? BigDecimal.ZERO : rental.getFinalAmount();
        if (paid.compareTo(due) >= 0 && due.signum() > 0) {
            rental.setPaymentStatus(AppConstants.STATUS_PAID);
        } else if (latest != null) {
            rental.setPaymentStatus(latest.displayStatus());
        } else {
            rental.setPaymentStatus(AppConstants.STATUS_PENDING);
        }
        if (latest != null) {
            rental.setPaymentMethod(latest.getMethod());
            rental.setPaymentReference(latest.getReferenceNumber());
        }
        return rental;
    }

    private String allocateRentalToken() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String token = QRGenerator.newRentalToken();
            if (rentalDAO.findByToken(token).isEmpty()) {
                return token;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique rental QR token.");
    }

    private List<String> buildTimeline(Rental rental, Payment payment) {
        List<String> steps = new ArrayList<>();
        steps.add("✓ Rental requested");
        if (payment != null && AppConstants.STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus())) {
            steps.add("✓ Payment completed");
        } else {
            steps.add("○ Payment pending");
        }
        if (rental.getApprovedAt() != null && AppConstants.STATUS_REJECTED.equalsIgnoreCase(rental.getStatus())) {
            steps.add("✕ Admin rejected");
        } else if (rental.getApprovedAt() != null || AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus())
                || AppConstants.STATUS_COMPLETED.equalsIgnoreCase(rental.getStatus())) {
            steps.add("✓ Admin approved");
            steps.add("✓ Bike collected");
        } else {
            steps.add("○ Admin approval pending");
        }
        if (AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus())) {
            steps.add("● Currently active");
            steps.add("○ Return pending");
        } else if (AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())) {
            steps.add("● Currently active");
            steps.add("● Return requested");
        } else if (AppConstants.STATUS_COMPLETED.equalsIgnoreCase(rental.getStatus())) {
            steps.add("✓ Currently completed");
            steps.add("✓ Bike returned");
        } else if (AppConstants.STATUS_REJECTED.equalsIgnoreCase(rental.getStatus())) {
            steps.add("○ Collection cancelled");
        }
        return steps;
    }

    private boolean matches(Rental rental, String needle) {
        return contains(String.valueOf(rental.getId()), needle)
                || contains(rental.displayId(), needle)
                || contains(rental.getCustomerName(), needle)
                || contains(rental.getBikeLabel(), needle)
                || contains(rental.getRegistrationNumber(), needle)
                || contains(rental.getBikeQrCode(), needle)
                || contains(rental.getRentalQrToken(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
