package com.bikevault.controller;

import com.bikevault.app.AppView;
import com.bikevault.app.ScanContext;
import com.bikevault.app.TripContext;
import com.bikevault.model.Rental;
import com.bikevault.service.RentalService;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.QRGenerator;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class CustomerQrController {

    private final RentalService rentalService = new RentalService();

    @FXML private ImageView rentalQrView;
    @FXML private Label rentalHint;

    private Rental rental;

    @FXML
    private void initialize() {
        rental = rentalService.findOwnActive();
        if (rental == null) {
            var trips = rentalService.findMine();
            rental = trips.stream()
                    .filter(item -> item.getRentalQrToken() != null)
                    .findFirst()
                    .orElse(null);
        }
        if (rental == null || rental.getRentalQrToken() == null) {
            rentalHint.setText("No rental QR yet. Book a trip to generate a unique pass.");
            return;
        }
        TripContext.select(rental.getId());
        rentalQrView.setImage(SwingFXUtils.toFXImage(
                QRGenerator.render(QRGenerator.rentalPayload(rental.getRentalQrToken()), 360), null));
        rentalHint.setText(rental.displayId() + "  ·  " + rental.displayStatus()
                + "\nShow this to Admin. The QR holds only a secure token.");
    }

    @FXML
    private void onShowRental() {
        if (rental != null) {
            TripContext.select(rental.getId());
        }
        NavigationUtil.showView(AppView.CUSTOMER_PASS);
    }

    @FXML
    private void onScanBike() {
        ScanContext.requestScan(AppView.BROWSE_BIKES);
    }
}
