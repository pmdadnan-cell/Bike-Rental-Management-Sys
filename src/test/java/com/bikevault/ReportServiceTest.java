package com.bikevault;

import com.bikevault.app.AppConstants;
import com.bikevault.model.Bike;
import com.bikevault.model.Customer;
import com.bikevault.model.Rental;
import com.bikevault.service.ReportService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTest {

    private final ReportService reportService = new ReportService();

    @Test
    void rentalFiltersUseDateCustomerBikeAndStatus() {
        Rental active = rental(1L, "Aditya Rao", LocalDate.of(2026, 9, 18), AppConstants.STATUS_ACTIVE);
        Rental completed = rental(2L, "Meera Nair", LocalDate.of(2026, 9, 10), AppConstants.STATUS_COMPLETED);
        List<Rental> source = List.of(active, completed);

        assertEquals(1, reportService.filterRentals(source, LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 20), "ALL", null, AppConstants.STATUS_ACTIVE).size());
        assertEquals(1, reportService.filterRentals(source, null, null, "Meera Nair", null, "ALL").size());
        assertEquals(1, reportService.filterRentals(source, null, null, "ALL", 1L, "ALL").size());
    }

    @Test
    void availabilityFilterSortsAndCountsByStatus() {
        Bike available = bike(AppConstants.BIKE_AVAILABLE);
        Bike rented = bike(AppConstants.BIKE_RENTED);
        List<Bike> filtered = reportService.filterAvailability(List.of(rented, available), AppConstants.BIKE_AVAILABLE);
        assertEquals(1, filtered.size());
        assertEquals("Yamaha", filtered.get(0).getBrand());
        assertEquals(1, reportService.countByBikeStatus(List.of(available, rented), AppConstants.BIKE_RENTED));
    }

    @Test
    void customerSearchMatchesNamePhoneAndLicense() {
        Customer one = customer("Aditya Rao", "9876543210", "KA01-LIC");
        Customer two = customer("Meera Nair", "9812345678", "KL07-LIC");
        assertEquals(1, reportService.filterCustomers(List.of(one, two), "meera").size());
        assertEquals(1, reportService.filterCustomers(List.of(one, two), "98765").size());
        assertEquals(1, reportService.filterCustomers(List.of(one, two), "KL07").size());
        assertEquals(2, reportService.filterCustomers(List.of(one, two), " ").size());
    }

    private Rental rental(long bikeId, String customer, LocalDate start, String status) {
        Rental rental = new Rental();
        rental.setBikeId(bikeId);
        rental.setCustomerName(customer);
        rental.setStartDate(start);
        rental.setStatus(status);
        return rental;
    }

    private Bike bike(String status) {
        Bike bike = new Bike();
        bike.setBrand("Yamaha");
        bike.setModel("MT-15");
        bike.setStatus(status);
        return bike;
    }

    private Customer customer(String name, String phone, String license) {
        Customer customer = new Customer();
        customer.setFullName(name);
        customer.setPhone(phone);
        customer.setDrivingLicenseNumber(license);
        customer.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
        return customer;
    }
}
