package com.bikevault.service;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.CustomerDAO;
import com.bikevault.dao.PaymentDAO;
import com.bikevault.dao.RentalDAO;
import com.bikevault.model.Bike;
import com.bikevault.model.Customer;
import com.bikevault.model.Payment;
import com.bikevault.model.Rental;
import com.bikevault.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report queries with stream-based filtering, sorting, and grouping helpers.
 */
public class ReportService {

    private final BikeDAO bikeDAO;
    private final CustomerDAO customerDAO;
    private final RentalDAO rentalDAO;
    private final PaymentDAO paymentDAO;

    public ReportService() {
        this(new BikeDAO(), new CustomerDAO(), new RentalDAO(), new PaymentDAO());
    }

    public ReportService(BikeDAO bikeDAO, CustomerDAO customerDAO, RentalDAO rentalDAO, PaymentDAO paymentDAO) {
        this.bikeDAO = bikeDAO;
        this.customerDAO = customerDAO;
        this.rentalDAO = rentalDAO;
        this.paymentDAO = paymentDAO;
    }

    public List<Bike> bikes() {
        SessionManager.requireOperator();
        return bikeDAO.findAll();
    }

    public List<Customer> customers() {
        SessionManager.requireOperator();
        return customerDAO.findAll();
    }

    public List<Rental> rentals() {
        SessionManager.requireOperator();
        return rentalDAO.findAll();
    }

    public List<Payment> payments() {
        SessionManager.requireOperator();
        return paymentDAO.findAll();
    }

    public Map<Long, BigDecimal> completedPaidByRental(List<Payment> source) {
        return source.stream()
                .filter(payment -> AppConstants.STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus()))
                .filter(payment -> payment.getRentalId() != null && payment.getAmount() != null)
                .collect(Collectors.groupingBy(Payment::getRentalId,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
    }

    public List<Rental> filterRentals(List<Rental> source, LocalDate from, LocalDate to,
                                      String customerName, Long bikeId, String status) {
        return source.stream()
                .filter(rental -> from == null || rental.getStartDate() == null || !rental.getStartDate().isBefore(from))
                .filter(rental -> to == null || rental.getStartDate() == null || !rental.getStartDate().isAfter(to))
                .filter(rental -> !ValidationUtil.hasText(customerName)
                        || "ALL".equals(customerName)
                        || customerName.equalsIgnoreCase(rental.getCustomerName()))
                .filter(rental -> bikeId == null || bikeId.equals(rental.getBikeId()))
                .filter(rental -> !ValidationUtil.hasText(status)
                        || "ALL".equals(status)
                        || status.equalsIgnoreCase(rental.getStatus()))
                .sorted(Comparator.comparing(Rental::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<Bike> filterAvailability(List<Bike> source, String status) {
        return source.stream()
                .filter(bike -> !ValidationUtil.hasText(status)
                        || "ALL".equals(status)
                        || status.equalsIgnoreCase(bike.getStatus()))
                .sorted(Comparator.comparing(Bike::getStatus)
                        .thenComparing(Bike::getBrand, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Customer> filterCustomers(List<Customer> source, String query) {
        String needle = ValidationUtil.trimToEmpty(query).toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(customer -> needle.isEmpty()
                        || contains(customer.getFullName(), needle)
                        || contains(customer.getPhone(), needle)
                        || contains(customer.getEmail(), needle)
                        || contains(customer.getDrivingLicenseNumber(), needle))
                .sorted(Comparator.comparing(Customer::getFullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public long countByBikeStatus(List<Bike> bikes, String status) {
        return bikes.stream().filter(bike -> status.equalsIgnoreCase(bike.getStatus())).count();
    }

    public long activeRentals(List<Rental> rentals) {
        return rentals.stream()
                .filter(rental -> AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus()))
                .count();
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
