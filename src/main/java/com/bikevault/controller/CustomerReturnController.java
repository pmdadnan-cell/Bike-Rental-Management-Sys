package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.TripContext;
import com.bikevault.model.Rental;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class CustomerReturnController {

    private final RentalService rentalService = new RentalService();
    private Rental active;

    @FXML private Label summaryLabel;
    @FXML private Button requestButton;

    @FXML
    private void initialize() {
        Long selected = TripContext.getRentalId();
        try {
            active = selected == null ? rentalService.findOwnActive() : rentalService.findOwnById(selected);
        } catch (RuntimeException ex) {
            summaryLabel.setText(ex.getMessage());
            requestButton.setDisable(true);
            return;
        }
        if (active == null) {
            summaryLabel.setText("You do not have an active rental to return.");
            requestButton.setDisable(true);
            return;
        }
        if (AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(active.getStatus()) || active.isReturnRequested()) {
            summaryLabel.setText("✓ RETURN REQUESTED\nPlease show your rental QR to the administrator to complete the return.\n\n"
                    + "Bike: " + na(active.getBikeLabel())
                    + "\nRental: " + active.displayId());
            requestButton.setText("SHOW RENTAL QR");
            requestButton.setDisable(false);
            requestButton.setOnAction(event -> {
                TripContext.select(active.getId());
                NavigationUtil.showView(AppView.CUSTOMER_PASS);
            });
            return;
        }
        summaryLabel.setText("Your return request will be sent to the administrator.\n\n"
                + "Bike: " + na(active.getBikeLabel())
                + "\nRental: " + active.displayId()
                + "\nStart: " + active.getStartDate()
                + "\nExpected return: " + active.getExpectedReturnDate());
    }

    @FXML
    private void onCancel() {
        NavigationUtil.showView(AppView.MY_TRIPS);
    }

    @FXML
    private void onConfirm() {
        if (active == null) {
            NavigationUtil.showView(AppView.MY_TRIPS);
            return;
        }
        if (!AlertUtil.confirm("Request Bike Return?",
                "After requesting a return, please show your rental QR to the administrator.",
                "REQUEST RETURN")) {
            return;
        }
        try {
            rentalService.requestReturn(active.getId());
            AlertUtil.info("✓ RETURN REQUESTED",
                    "Please show your rental QR to the administrator to complete the return.");
            TripContext.select(active.getId());
            NavigationUtil.showView(AppView.CUSTOMER_PASS);
        } catch (RuntimeException ex) {
            AlertUtil.error("Return request failed", ex.getMessage());
        }
    }

    private String na(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
