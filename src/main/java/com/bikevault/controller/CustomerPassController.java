package com.bikevault.controller;

import com.bikevault.app.AppView;
import com.bikevault.app.SessionManager;
import com.bikevault.app.TripContext;
import com.bikevault.model.Rental;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BrandAssets;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.QRGenerator;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class CustomerPassController {

    private final RentalService rentalService = new RentalService();

    @FXML private ImageView logoView;
    @FXML private ImageView qrView;
    @FXML private Label passLabel;
    @FXML private Label statusBadge;
    @FXML private VBox passCard;
    @FXML private Label emptyHint;

    private Rental rental;

    @FXML
    private void initialize() {
        logoView.setImage(BrandAssets.mark());
        Long id = TripContext.getRentalId();
        try {
            if (id == null) {
                rental = rentalService.findOwnActive();
                if (rental == null) {
                    var trips = rentalService.findMine();
                    rental = trips.stream()
                            .filter(item -> item.getRentalQrToken() != null)
                            .findFirst()
                            .orElse(null);
                }
            } else {
                rental = rentalService.findOwnById(id);
            }
        } catch (RuntimeException ex) {
            showEmpty(ex.getMessage());
            return;
        }
        if (rental == null || rental.getRentalQrToken() == null) {
            showEmpty("No rental QR yet. Book a trip, complete payment, then show this pass to Admin.");
            return;
        }
        TripContext.select(rental.getId());
        qrView.setImage(SwingFXUtils.toFXImage(
                QRGenerator.render(QRGenerator.rentalPayload(rental.getRentalQrToken()), 480), null));
        statusBadge.setText(rental.displayStatus());
        java.time.format.DateTimeFormatter day = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
        passLabel.setText("Rental ID\n" + rental.displayId()
                + "\n\nBike\n" + (rental.getBikeLabel() == null || rental.getBikeLabel().isBlank() ? "N/A" : rental.getBikeLabel())
                + "\n\nCustomer\n" + SessionManager.displayName()
                + "\n\nPickup\n" + (rental.getStartDate() == null ? "N/A" : rental.getStartDate().format(day))
                + "\n\nReturn\n" + (rental.getExpectedReturnDate() == null ? "N/A" : rental.getExpectedReturnDate().format(day))
                + "\n\nStatus\n" + rental.displayStatus());
    }

    @FXML
    private void onShowQr() {
        if (qrView.getImage() != null) {
            AlertUtil.info("Show this QR to Admin",
                    "Only a secure rental token is encoded. Admin verifies it in MySQL before approval.");
        }
    }

    @FXML
    private void onViewTrip() {
        if (rental != null) {
            TripContext.select(rental.getId());
            NavigationUtil.showView(AppView.CUSTOMER_TRIP);
        } else {
            NavigationUtil.showView(AppView.MY_TRIPS);
        }
    }

    private void showEmpty(String message) {
        if (passCard != null) {
            passCard.setVisible(false);
            passCard.setManaged(false);
        }
        if (emptyHint != null) {
            emptyHint.setText(message);
            emptyHint.setVisible(true);
            emptyHint.setManaged(true);
        }
    }
}
