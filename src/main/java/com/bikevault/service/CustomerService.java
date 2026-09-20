package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.CustomerDAO;
import com.bikevault.dao.PaymentDAO;
import com.bikevault.dao.PenaltyDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.dao.UserDAO;
import com.bikevault.exception.UnauthorizedException;
import com.bikevault.model.Customer;
import com.bikevault.model.Rental;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Customer use-cases. Filtering and sorting use the Streams API before the table is bound.
 */
public class CustomerService {

    private final CustomerDAO customerDAO;
    private final AuditLogDAO auditLogDAO;
    private final RentalDAO rentalDAO;
    private final PaymentDAO paymentDAO;
    private final PenaltyDAO penaltyDAO;
    private final UserDAO userDAO;

    public CustomerService() {
        this(new CustomerDAO(), new AuditLogDAO(), new RentalDAO(), new PaymentDAO(), new PenaltyDAO(), new UserDAO());
    }

    public CustomerService(CustomerDAO customerDAO, AuditLogDAO auditLogDAO) {
        this(customerDAO, auditLogDAO, new RentalDAO(), new PaymentDAO(), new PenaltyDAO(), new UserDAO());
    }

    public CustomerService(CustomerDAO customerDAO, AuditLogDAO auditLogDAO, RentalDAO rentalDAO,
                           PaymentDAO paymentDAO, PenaltyDAO penaltyDAO, UserDAO userDAO) {
        this.customerDAO = customerDAO;
        this.auditLogDAO = auditLogDAO;
        this.rentalDAO = rentalDAO;
        this.paymentDAO = paymentDAO;
        this.penaltyDAO = penaltyDAO;
        this.userDAO = userDAO;
    }

    public List<Customer> findAll() {
        SessionManager.requireOperator();
        return customerDAO.findAll().stream()
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    public Customer findOwn() {
        return customerDAO.findById(SessionManager.requireCustomerId())
                .map(this::enrich)
                .orElseThrow(() -> new IllegalStateException("Customer profile was not found."));
    }

    public List<Customer> search(List<Customer> source, String query) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        String statusFilter = null;
        return source.stream()
                .filter(customer -> needle.isEmpty() || matches(customer, needle))
                .filter(customer -> statusFilter == null || statusFilter.equalsIgnoreCase(customer.getStatus()))
                .sorted(Comparator.comparing(Customer::getFullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Customer> filterByStatus(List<Customer> source, String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return source;
        }
        return source.stream()
                .filter(customer -> status.equalsIgnoreCase(customer.getStatus()))
                .collect(Collectors.toList());
    }

    public Customer add(Customer customer) {
        SessionManager.requireOperator();
        validate(customer);
        if (customer.getRegistrationDate() == null) {
            customer.setRegistrationDate(LocalDate.now());
        }
        Customer saved = customerDAO.insert(customer);
        audit("ADD_CUSTOMER", "Added customer " + saved.getFullName());
        return saved;
    }

    public boolean update(Customer customer) {
        SessionManager.requireOperator();
        if (customer.getId() == null) {
            throw new IllegalArgumentException("Select a customer to update.");
        }
        validate(customer);
        Customer existing = customerDAO.findById(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));
        customer.setUserId(existing.getUserId());
        boolean updated = customerDAO.update(customer);
        if (updated) {
            audit("UPDATE_CUSTOMER", "Updated customer " + customer.getFullName());
        }
        return updated;
    }

    public boolean updateOwnProfile(Customer patch) {
        long customerId = SessionManager.requireCustomerId();
        Customer existing = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("Customer profile was not found."));
        existing.setFullName(ValidationUtil.requireText(patch.getFullName(), "Full name"));
        existing.setEmail(ValidationUtil.requireEmail(patch.getEmail()));
        existing.setPhone(ValidationUtil.requirePhone(patch.getPhone()));
        existing.setAddress(ValidationUtil.requireText(patch.getAddress(), "Address"));
        String license = ValidationUtil.requireText(patch.getDrivingLicenseNumber(), "Driving license number");
        existing.setDrivingLicenseNumber(license.toUpperCase(Locale.ROOT));
        if (patch.getDateOfBirth() != null) {
            existing.setDateOfBirth(patch.getDateOfBirth());
        }
        boolean updated = customerDAO.update(existing);
        if (updated) {
            audit("UPDATE_PROFILE", "Customer updated their profile");
        }
        return updated;
    }

    public boolean deactivate(long id) {
        SessionManager.requireAdminRole();
        Customer existing = customerDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));
        existing.setStatus(AppConstants.STATUS_INACTIVE);
        boolean updated = customerDAO.update(existing);
        if (updated && existing.getUserId() != null) {
            userDAO.updateStatus(existing.getUserId(), AppConstants.STATUS_INACTIVE);
        }
        if (updated) {
            audit("DEACTIVATE_CUSTOMER", "Deactivated customer #" + id);
        }
        return updated;
    }

    public boolean reactivate(long id) {
        SessionManager.requireAdminRole();
        Customer existing = customerDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));
        existing.setStatus(AppConstants.STATUS_ACTIVE);
        boolean updated = customerDAO.update(existing);
        if (updated && existing.getUserId() != null) {
            userDAO.updateStatus(existing.getUserId(), AppConstants.STATUS_ACTIVE);
        }
        if (updated) {
            audit("REACTIVATE_CUSTOMER", "Reactivated customer #" + id);
        }
        return updated;
    }

    public boolean delete(long id) {
        SessionManager.requireAdminRole();
        long history = rentalDAO.countByCustomerId(id);
        if (history > 0) {
            throw new IllegalStateException(
                    "This customer has rental history. Deactivate the account instead of deleting it.");
        }
        boolean deleted = customerDAO.delete(id);
        if (deleted) {
            audit("DELETE_CUSTOMER", "Deleted customer #" + id);
        }
        return deleted;
    }

    public BigDecimal totalSpentTillDate(long customerId) {
        BigDecimal spent = paymentDAO.getTotalSpentByCustomer(customerId);
        return spent == null ? BigDecimal.ZERO : spent;
    }

    public BigDecimal outstandingFor(long customerId) {
        List<Rental> rentals = rentalDAO.findByCustomerId(customerId);
        BigDecimal rentalDue = rentals.stream()
                .map(rental -> {
                    BigDecimal due = rental.getFinalAmount() != null ? rental.getFinalAmount() : rental.getTotalAmount();
                    BigDecimal paid = paymentDAO.sumCompletedByRental(rental.getId());
                    BigDecimal remaining = (due == null ? BigDecimal.ZERO : due)
                            .subtract(paid == null ? BigDecimal.ZERO : paid);
                    return remaining.signum() > 0 ? remaining : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal penalties = penaltyDAO.sumPendingByCustomer(customerId);
        return rentalDue.add(penalties == null ? BigDecimal.ZERO : penalties);
    }

    private Customer enrich(Customer customer) {
        if (customer.getId() != null) {
            customer.setTotalRentals(rentalDAO.countByCustomerId(customer.getId()));
            customer.setActiveRentals(rentalDAO.countActiveByCustomerId(customer.getId()));
            customer.setOutstandingAmount(outstandingFor(customer.getId()));
        }
        return customer;
    }

    private void validate(Customer customer) {
        customer.setFullName(ValidationUtil.requireText(customer.getFullName(), "Full name"));
        customer.setEmail(ValidationUtil.requireEmail(customer.getEmail()));
        customer.setPhone(ValidationUtil.requirePhone(customer.getPhone()));
        customer.setAddress(ValidationUtil.requireText(customer.getAddress(), "Address"));
        String license = ValidationUtil.requireText(customer.getDrivingLicenseNumber(), "Driving license number");
        if (!ValidationUtil.isLicense(license)) {
            throw new IllegalArgumentException("Enter a valid driving license number.");
        }
        customer.setDrivingLicenseNumber(license.toUpperCase(Locale.ROOT));
        if (customer.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (customer.getDateOfBirth().isAfter(LocalDate.now().minusYears(16))) {
            throw new IllegalArgumentException("Customer must be at least 16 years old.");
        }
        customer.setGender(ValidationUtil.requireText(customer.getGender(), "Gender"));
        customer.setStatus(ValidationUtil.hasText(customer.getStatus())
                ? customer.getStatus() : AppConstants.STATUS_ACTIVE);
        if (customer.getRegistrationDate() == null) {
            customer.setRegistrationDate(LocalDate.now());
        }
    }

    private boolean matches(Customer customer, String needle) {
        return contains(customer.getFullName(), needle)
                || contains(customer.getPhone(), needle)
                || contains(customer.getEmail(), needle)
                || contains(customer.getDrivingLicenseNumber(), needle)
                || contains(String.valueOf(customer.getId()), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void audit(String action, String description) {
        Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
        auditLogDAO.insert(userId, action, description);
    }

    public void assertNotCustomerListingAll() {
        if (SessionManager.isCustomer()) {
            throw new UnauthorizedException("Customers cannot view the full customer directory.");
        }
    }
}
