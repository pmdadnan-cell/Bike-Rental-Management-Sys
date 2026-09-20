package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.BookingContext;
import com.bikevault.app.TripContext;
import com.bikevault.model.Bike;
import com.bikevault.model.Rental;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class CustomerPayController {

    private final RentalService rentalService = new RentalService();

    @FXML private Label rentalLabel;
    @FXML private Label rateLabel;
    @FXML private Label durationLabel;
    @FXML private Label rentalAmountLabel;
    @FXML private Label penaltyLabel;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> methodCombo;

    @FXML
    private void initialize() {
        methodCombo.getItems().setAll(AppConstants.PAYMENT_CASH, AppConstants.PAYMENT_CARD, AppConstants.PAYMENT_UPI);
        methodCombo.getSelectionModel().select(AppConstants.PAYMENT_UPI);
        Rental rental = BookingContext.getQuotedRental();
        Bike bike = BookingContext.getSelectedBike();
        if (rental == null || bike == null) {
            AlertUtil.warn("No rental", "Choose a bike and dates before paying.");
            NavigationUtil.showView(AppView.BROWSE_BIKES);
            return;
        }
        rentalLabel.setText((bike.getBrand() + " " + bike.getModel()).trim());
        rateLabel.setText("₹ " + rental.getDailyRate());
        durationLabel.setText((rental.getDurationDays() == null ? "1" : rental.getDurationDays()) + " days");
        rentalAmountLabel.setText("₹ " + rental.getTotalAmount());
        penaltyLabel.setText("₹ 0");
        totalLabel.setText("₹ " + rental.getFinalAmount());
    }

    @FXML
    private void onPay() {
        Rental rental = BookingContext.getQuotedRental();
        Bike bike = BookingContext.getSelectedBike();
        if (rental == null || bike == null) {
            return;
        }
        try {
            Rental saved = rentalService.requestRentalAndPay(rental, bike, methodCombo.getValue());
            TripContext.select(saved.getId());
            BookingContext.clear();
            AlertUtil.info("Payment successful",
                    "Rental requested. Show your unique QR to the BikeVault administrator to collect the bike.");
            NavigationUtil.showView(AppView.CUSTOMER_QR);
        } catch (RuntimeException ex) {
            AlertUtil.error("Payment failed", ex.getMessage());
        }
    }
}
