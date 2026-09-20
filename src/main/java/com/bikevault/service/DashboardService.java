package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.CustomerDAO;
import com.bikevault.dao.IncidentDAO;
import com.bikevault.dao.PaymentDAO;
import com.bikevault.dao.PenaltyDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.model.AuditLog;
import com.bikevault.model.Bike;
import com.bikevault.model.Customer;
import com.bikevault.model.CustomerHomeSnapshot;
import com.bikevault.model.DashboardSnapshot;
import com.bikevault.model.Payment;
import com.bikevault.model.Rental;
import com.bikevault.util.PricingCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds dashboard statistics from live MySQL records using the Streams API.
 */
public class DashboardService {

    private final BikeDAO bikeDAO;
    private final CustomerDAO customerDAO;
    private final RentalDAO rentalDAO;
    private final PaymentDAO paymentDAO;
    private final AuditLogDAO auditLogDAO;

    private final PenaltyDAO penaltyDAO;
    private final IncidentDAO incidentDAO;
    private final CustomerService customerService;

    public DashboardService() {
        this(new BikeDAO(), new CustomerDAO(), new RentalDAO(), new PaymentDAO(), new AuditLogDAO(),
                new PenaltyDAO(), new IncidentDAO());
    }

    public DashboardService(BikeDAO bikeDAO, CustomerDAO customerDAO, RentalDAO rentalDAO,
                            PaymentDAO paymentDAO, AuditLogDAO auditLogDAO) {
        this(bikeDAO, customerDAO, rentalDAO, paymentDAO, auditLogDAO, new PenaltyDAO(), new IncidentDAO());
    }

    public DashboardService(BikeDAO bikeDAO, CustomerDAO customerDAO, RentalDAO rentalDAO,
                            PaymentDAO paymentDAO, AuditLogDAO auditLogDAO,
                            PenaltyDAO penaltyDAO, IncidentDAO incidentDAO) {
        this.bikeDAO = bikeDAO;
        this.customerDAO = customerDAO;
        this.rentalDAO = rentalDAO;
        this.paymentDAO = paymentDAO;
        this.auditLogDAO = auditLogDAO;
        this.penaltyDAO = penaltyDAO;
        this.incidentDAO = incidentDAO;
        this.customerService = new CustomerService(customerDAO, auditLogDAO, rentalDAO, paymentDAO, penaltyDAO,
                new com.bikevault.dao.UserDAO());
    }

    public DashboardSnapshot load(LocalDate asOf, String period) {
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        List<Bike> bikes = bikeDAO.findAll();
        List<Customer> customers = customerDAO.findAll();
        List<Rental> rentals = rentalDAO.findAll();
        List<Payment> payments = paymentDAO.findAll();

        DashboardSnapshot snapshot = new DashboardSnapshot();
        snapshot.setTotalBikes(bikes.size());
        snapshot.setAvailableBikes(countStatus(bikes, AppConstants.BIKE_AVAILABLE));
        snapshot.setRentedBikes(countStatus(bikes, AppConstants.BIKE_RENTED));
        snapshot.setMaintenanceBikes(countStatus(bikes, AppConstants.BIKE_MAINTENANCE));
        snapshot.setTotalCustomers(customers.size());
        snapshot.setActiveRentals(rentals.stream()
                .filter(rental -> AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus()))
                .count());

        LocalDate from = periodStart(date, period);
        snapshot.setRevenueLabel(revenueTitle(period));
        snapshot.setRevenue(payments.stream()
                .filter(payment -> AppConstants.STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus()))
                .filter(payment -> payment.getPaidAt() != null)
                .filter(payment -> {
                    LocalDate paid = payment.getPaidAt().toLocalDate();
                    return !paid.isBefore(from) && !paid.isAfter(date);
                })
                .map(payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        snapshot.setRevenue(PricingCalculator.money(snapshot.getRevenue()));
        snapshot.setTodayRevenue(PricingCalculator.money(paymentDAO.sumCompletedOn(date)));
        snapshot.setTotalRevenue(PricingCalculator.money(paymentDAO.sumCompleted()));
        snapshot.setPendingPayments(paymentDAO.countPending());
        snapshot.setPendingReturns(rentals.stream()
                .filter(rental -> AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())
                        || rental.isReturnRequested())
                .count());
        snapshot.setPenalties(penaltyDAO.countPending());
        snapshot.setIncidents(rentals.stream()
                .filter(rental -> AppConstants.STATUS_COMPLETED.equalsIgnoreCase(rental.getStatus()))
                .count());
        snapshot.setPendingApprovals(rentalDAO.countByStatus(AppConstants.STATUS_PENDING_APPROVAL));

        List<AuditLog> logs = auditLogDAO.findRecent(30).stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        snapshot.setRecentActivity(logs);
        return snapshot;
    }

    public CustomerHomeSnapshot loadCustomerHome(long customerId) {
        List<Bike> bikes = bikeDAO.findAll();
        CustomerHomeSnapshot home = new CustomerHomeSnapshot();
        Customer customer = customerDAO.findById(customerId).orElse(null);
        home.setCustomerName(customer == null ? "Guest" : customer.getFullName());
        home.setAvailableBikes(countStatus(bikes, AppConstants.BIKE_AVAILABLE));
        home.setCurrentRental(rentalDAO.findActiveByCustomerId(customerId).orElse(null));
        home.setOutstandingAmount(PricingCalculator.money(customerService.totalSpentTillDate(customerId)));
        home.setActivePenalties(penaltyDAO.countPendingByCustomer(customerId));
        home.setFeaturedBikes(bikes.stream()
                .filter(bike -> AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(bike.getStatus()))
                .limit(4)
                .collect(Collectors.toList()));
        return home;
    }

    private long countStatus(List<Bike> bikes, String status) {
        return bikes.stream().filter(bike -> status.equalsIgnoreCase(bike.getStatus())).count();
    }

    private LocalDate periodStart(LocalDate asOf, String period) {
        if ("This Week".equals(period)) {
            return asOf.minusDays(6);
        }
        if ("This Month".equals(period)) {
            return asOf.withDayOfMonth(1);
        }
        if ("All Time".equals(period)) {
            return LocalDate.of(2000, 1, 1);
        }
        return asOf;
    }

    private String revenueTitle(String period) {
        if ("This Week".equals(period)) {
            return "WEEK REVENUE";
        }
        if ("This Month".equals(period)) {
            return "MONTH REVENUE";
        }
        if ("All Time".equals(period)) {
            return "TOTAL REVENUE";
        }
        return "TODAY'S REVENUE";
    }
}
