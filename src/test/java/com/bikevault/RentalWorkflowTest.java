package com.bikevault;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.BikeDAO;
import com.bikevault.dao.CustomerDAO;
import com.bikevault.dao.UserDAO;
import com.bikevault.database.SchemaMigrator;
import com.bikevault.model.Bike;
import com.bikevault.model.Customer;
import com.bikevault.model.Penalty;
import com.bikevault.model.Rental;
import com.bikevault.model.User;
import com.bikevault.service.CustomerService;
import com.bikevault.service.PenaltyService;
import com.bikevault.service.RentalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RentalWorkflowTest {

    private final UserDAO userDAO = new UserDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BikeDAO bikeDAO = new BikeDAO();
    private final RentalService rentalService = new RentalService();
    private final PenaltyService penaltyService = new PenaltyService();
    private final CustomerService customerService = new CustomerService();

    @AfterEach
    void clear() {
        SessionManager.clear();
    }

    @Test
    void customerPayThenAdminApproveReturnAndReject() {
        try {
            SchemaMigrator.ensure();
        } catch (RuntimeException ex) {
            assumeTrue(false, "Database is not available: " + ex.getMessage());
        }

        User customerUser = userDAO.findByUsername("hassan").orElse(null);
        User adminUser = userDAO.findByUsername("admin").orElse(null);
        assumeTrue(customerUser != null && adminUser != null, "Demo users are missing");
        Customer customer = customerDAO.findByUserId(customerUser.getId()).orElse(null);
        assumeTrue(customer != null, "Hassan customer profile is missing");

        closeOpenRentals(customerUser, customer, adminUser);

        Bike bike = availableBike();
        assumeTrue(bike != null, "No available bike for the approve path");
        Rental approved = customerRequest(customerUser, customer, bike, 2);
        assertEquals(AppConstants.STATUS_PENDING_APPROVAL, approved.getStatus());
        assertNotNull(approved.getRentalQrToken());
        assertEquals(AppConstants.STATUS_PAID, approved.getPaymentStatus());
        assertEquals(bike.getDailyRate().multiply(BigDecimal.valueOf(2)).setScale(2),
                approved.getTotalAmount());
        assertEquals(AppConstants.BIKE_AVAILABLE, bikeDAO.findById(bike.getId()).orElseThrow().getStatus());

        SessionManager.begin(adminUser);
        Rental loaded = rentalService.findByToken(approved.getRentalQrToken());
        assertEquals(approved.getId(), loaded.getId());
        rentalService.approve(loaded.getId());
        Rental active = rentalService.findOwnById(loaded.getId());
        assertEquals(AppConstants.STATUS_ACTIVE, active.getStatus());
        assertEquals(AppConstants.BIKE_RENTED, bikeDAO.findById(bike.getId()).orElseThrow().getStatus());

        SessionManager.begin(customerUser, customer.getId());
        rentalService.requestReturn(active.getId());
        assertEquals(AppConstants.STATUS_RETURN_REQUESTED, rentalService.findOwnById(active.getId()).getStatus());

        SessionManager.begin(adminUser);
        Rental returnScan = rentalService.findByToken(approved.getRentalQrToken());
        assertEquals(active.getId(), returnScan.getId());
        assertEquals(AppConstants.STATUS_RETURN_REQUESTED, returnScan.getStatus());
        rentalService.acceptReturn(returnScan.getId());
        assertEquals(AppConstants.STATUS_COMPLETED, rentalService.findOwnById(active.getId()).getStatus());
        assertEquals(AppConstants.BIKE_AVAILABLE, bikeDAO.findById(bike.getId()).orElseThrow().getStatus());

        Penalty penalty = new Penalty();
        penalty.setCustomerId(customer.getId());
        penalty.setRentalId(active.getId());
        penalty.setBikeId(bike.getId());
        penalty.setPenaltyType("DAMAGE");
        penalty.setDescription("Minor body damage");
        penalty.setAmount(new BigDecimal("1000.00"));
        penalty.setStatus(AppConstants.STATUS_PENDING);
        Penalty savedPenalty = penaltyService.add(penalty);
        BigDecimal beforePay = customerService.totalSpentTillDate(customer.getId());

        SessionManager.begin(customerUser, customer.getId());
        penaltyService.payMine(savedPenalty.getId(), AppConstants.PAYMENT_UPI);
        assertEquals(AppConstants.STATUS_PAID, penaltyService.findOwnById(savedPenalty.getId()).getStatus());
        BigDecimal afterPay = customerService.totalSpentTillDate(customer.getId());
        assertEquals(0, afterPay.subtract(beforePay).compareTo(new BigDecimal("1000.00")));

        Bike rejectBike = availableBike();
        assumeTrue(rejectBike != null, "No available bike for the reject path");
        Rental rejected = customerRequest(customerUser, customer, rejectBike, 2);
        SessionManager.begin(adminUser);
        rentalService.reject(rejected.getId(), "Documentation issue", null);
        assertEquals(AppConstants.STATUS_REJECTED, rentalService.findOwnById(rejected.getId()).getStatus());
        assertEquals(AppConstants.BIKE_AVAILABLE, bikeDAO.findById(rejectBike.getId()).orElseThrow().getStatus());
    }

    private void closeOpenRentals(User customerUser, Customer customer, User adminUser) {
        SessionManager.begin(customerUser, customer.getId());
        for (Rental rental : rentalService.findMine()) {
            if (AppConstants.STATUS_PENDING_APPROVAL.equalsIgnoreCase(rental.getStatus())) {
                SessionManager.begin(adminUser);
                rentalService.reject(rental.getId(), "Workflow reset", null);
                SessionManager.begin(customerUser, customer.getId());
            } else if (AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus())) {
                rentalService.requestReturn(rental.getId());
                SessionManager.begin(adminUser);
                rentalService.acceptReturn(rental.getId());
                SessionManager.begin(customerUser, customer.getId());
            } else if (AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())) {
                SessionManager.begin(adminUser);
                rentalService.acceptReturn(rental.getId());
                SessionManager.begin(customerUser, customer.getId());
            }
        }
    }

    private Rental customerRequest(User customerUser, Customer customer, Bike bike, int days) {
        SessionManager.begin(customerUser, customer.getId());
        Rental draft = new Rental();
        draft.setStartDate(LocalDate.now());
        draft.setExpectedReturnDate(LocalDate.now().plusDays(days));
        rentalService.quote(draft, bike);
        return rentalService.requestRentalAndPay(draft, bike, AppConstants.PAYMENT_UPI);
    }

    private Bike availableBike() {
        return bikeDAO.findAll().stream()
                .filter(bike -> AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(bike.getStatus()))
                .findFirst()
                .orElse(null);
    }
}
