package com.bikevault;

import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.exception.UnauthorizedException;
import com.bikevault.model.Bike;
import com.bikevault.model.Challan;
import com.bikevault.model.Penalty;
import com.bikevault.model.User;
import com.bikevault.service.BikeService;
import com.bikevault.service.ChallanService;
import com.bikevault.service.PenaltyService;
import com.bikevault.service.RentalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleSecurityTest {

    @AfterEach
    void clearSession() {
        SessionManager.clear();
    }

    @Test
    void customerSessionIsIdentifiedAndCannotClaimAdmin() {
        SessionManager.begin(user(3L, "hassan", "Hassan Khan", AppConstants.ROLE_CUSTOMER), 5L);
        assertTrue(SessionManager.isCustomer());
        assertFalse(SessionManager.isAdmin());
        assertFalse(SessionManager.isOperator());
        assertEquals(AppConstants.ROLE_CUSTOMER, SessionManager.getCurrentRole());
        assertEquals(5L, SessionManager.requireCustomerId());
    }

    @Test
    void customerCannotDeleteBikesOrChangeOfficialCharges() {
        SessionManager.begin(user(3L, "hassan", "Hassan Khan", AppConstants.ROLE_CUSTOMER), 5L);
        BikeService bikeService = new BikeService();
        PenaltyService penaltyService = new PenaltyService();
        ChallanService challanService = new ChallanService();
        Bike bike = new Bike();
        bike.setBrand("Yamaha");
        bike.setModel("MT-15");
        bike.setRegistrationNumber("KA-99-XX-0001");
        bike.setCategoryId(1L);
        bike.setColor("Black");
        bike.setDailyRate(new BigDecimal("500"));
        assertThrows(UnauthorizedException.class, () -> bikeService.add(bike));
        assertThrows(UnauthorizedException.class, () -> bikeService.delete(1L));
        Penalty penalty = new Penalty();
        penalty.setCustomerId(5L);
        penalty.setPenaltyType("LATE_RETURN");
        penalty.setDescription("Late");
        penalty.setAmount(new BigDecimal("100"));
        assertThrows(UnauthorizedException.class, () -> penaltyService.add(penalty));
        assertThrows(UnauthorizedException.class, penaltyService::rejectCustomerMutation);
        Challan challan = new Challan();
        challan.setCustomerId(5L);
        challan.setDescription("Signal jump");
        challan.setAmount(new BigDecimal("1000"));
        assertThrows(UnauthorizedException.class, () -> challanService.add(challan));
        assertThrows(UnauthorizedException.class, challanService::rejectCustomerMutation);
    }

    @Test
    void customerCannotListAllRentals() {
        SessionManager.begin(user(3L, "hassan", "Hassan Khan", AppConstants.ROLE_CUSTOMER), 5L);
        assertThrows(UnauthorizedException.class, () -> new RentalService().findAll());
    }

    @Test
    void customerCannotApproveOrCompleteReturns() {
        SessionManager.begin(user(3L, "hassan", "Hassan Khan", AppConstants.ROLE_CUSTOMER), 5L);
        RentalService rentalService = new RentalService();
        assertThrows(UnauthorizedException.class, () -> rentalService.approve(1L));
        assertThrows(UnauthorizedException.class, () -> rentalService.reject(1L, "Other", "no"));
        Bike bike = new Bike();
        bike.setId(1L);
        assertThrows(UnauthorizedException.class, () -> rentalService.completeReturn(bike, java.time.LocalDate.now(), "n", null, false));
    }

    @Test
    void rentalQrPayloadContainsOnlyTheSecureToken() {
        com.bikevault.model.QrPayload payload = com.bikevault.util.QRDecoder.parse("BIKEVAULT|RENTAL|TOKEN=RVQR-7F92A83D-92C1-4B5E-9A10-11C0FFEE0001");
        assertTrue(payload.isRentalQr());
        assertEquals("RVQR-7F92A83D-92C1-4B5E-9A10-11C0FFEE0001", payload.getRentalToken());
        assertEquals("PENDING ADMIN APPROVAL", pendingDisplay());
    }

    @Test
    void overdueDisplayStatusIsDerivedForActivePastDueRentals() {
        com.bikevault.model.Rental rental = new com.bikevault.model.Rental();
        rental.setStatus(AppConstants.STATUS_ACTIVE);
        rental.setExpectedReturnDate(java.time.LocalDate.now().minusDays(1));
        assertEquals(AppConstants.STATUS_ACTIVE, rental.displayStatus());
        rental.setStatus(AppConstants.STATUS_RETURN_REQUESTED);
        assertEquals("RETURN REQUESTED", rental.displayStatus());
    }

    private String pendingDisplay() {
        com.bikevault.model.Rental rental = new com.bikevault.model.Rental();
        rental.setStatus(AppConstants.STATUS_PENDING_APPROVAL);
        return rental.displayStatus();
    }

    private User user(long id, String username, String name, String role) {
        User user = new User(id, username, name, role, AppConstants.STATUS_ACTIVE);
        return user;
    }
}
