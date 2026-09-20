package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.TripContext;
import com.bikevault.model.Bike;
import com.bikevault.model.Payment;
import com.bikevault.model.Rental;
import com.bikevault.model.TripDetail;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BikeImageLoader;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.QRGenerator;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class CustomerTripController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerTripController.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final RentalService rentalService = new RentalService();
    private Long rentalId;

    @FXML private VBox loadingPane;
    @FXML private VBox errorPane;
    @FXML private Label errorLabel;
    @FXML private ScrollPane contentPane;
    @FXML private Label rentalIdLabel;
    @FXML private Label statusBadge;
    @FXML private Label summaryLabel;
    @FXML private Label paymentLabel;
    @FXML private StackPane bikeImageHost;
    @FXML private Label bikeLabel;
    @FXML private ImageView qrView;
    @FXML private Label qrLabel;
    @FXML private Label messageLabel;
    @FXML private Button returnButton;

    @FXML
    private void initialize() {
        rentalId = TripContext.getRentalId();
        loadTrip();
    }

    @FXML
    private void onRetry() {
        loadTrip();
    }

    @FXML
    private void onBack() {
        NavigationUtil.showView(AppView.MY_TRIPS);
    }

    @FXML
    private void onShowQr() {
        if (rentalId != null) {
            TripContext.select(rentalId);
        }
        NavigationUtil.showView(AppView.CUSTOMER_PASS);
    }

    @FXML
    private void onReturn() {
        if (rentalId == null) {
            return;
        }
        if (!AlertUtil.confirm("Request Bike Return?",
                "After requesting a return, please show your rental QR to the administrator.",
                "REQUEST RETURN")) {
            return;
        }
        try {
            rentalService.requestReturn(rentalId);
            AlertUtil.info("✓ RETURN REQUESTED",
                    "Please show your rental QR to the administrator to complete the return.");
            loadTrip();
        } catch (RuntimeException ex) {
            AlertUtil.error("Return request failed", ex.getMessage());
        }
    }

    private void loadTrip() {
        if (rentalId == null) {
            showError("Choose a trip first.");
            return;
        }
        showLoading();
        try {
            TripDetail detail = rentalService.loadTrip(rentalId);
            if (detail == null || detail.getRental() == null) {
                showError("This rental could not be found.");
                return;
            }
            bind(detail);
            showContent();
        } catch (RuntimeException ex) {
            LOG.error("Unable to load rental details for {}", rentalId, ex);
            showError(ex.getMessage() == null ? "A database error prevented this trip from loading." : ex.getMessage());
        }
    }

    private void bind(TripDetail detail) {
        Rental rental = detail.getRental();
        Bike bike = detail.getBike();
        Payment payment = detail.getLatestPayment();

        rentalIdLabel.setText(rental.displayId());
        statusBadge.setText(rental.displayStatus());
        bikeImageHost.getChildren().setAll(BikeImageLoader.framed(bike, 420, 160));
        bikeLabel.setText(na(rental.getBikeLabel()));

        String start = rental.getStartDate() == null ? "N/A" : (rental.getApprovedAt() == null
                ? rental.getStartDate().format(WHEN)
                : rental.getApprovedAt().format(STAMP));
        String expected = rental.getExpectedReturnDate() == null ? "N/A"
                : rental.getExpectedReturnDate().atStartOfDay().withHour(10).format(STAMP);
        summaryLabel.setText("Rental ID\n" + rental.displayId()
                + "\nRental status\n" + rental.displayStatus()
                + "\nCustomer\n" + na(rental.getCustomerName())
                + "\nSTART\n" + start
                + "\nEXPECTED RETURN\n" + expected
                + "\nDURATION\n" + (rental.getDurationDays() == null ? "N/A" : rental.getDurationDays() + " Days"));

        boolean paidOk = "PAID".equalsIgnoreCase(rental.getPaymentStatus())
                || (detail.getPaidAmount() != null && detail.getPaidAmount().signum() > 0)
                || (payment != null && AppConstants.STATUS_COMPLETED.equalsIgnoreCase(payment.getStatus()));
        paymentLabel.setText("PAYMENT\n₹ " + money(rental.getFinalAmount() == null ? rental.getTotalAmount() : rental.getFinalAmount())
                + "\nPAYMENT STATUS\n" + (paidOk ? "PAID" : na(rental.getPaymentStatus()))
                + "\nDAILY RATE\n₹ " + money(rental.getDailyRate()) + "/day");

        if (rental.getRentalQrToken() == null || rental.getRentalQrToken().isBlank()) {
            qrView.setImage(null);
            qrLabel.setText("N/A");
        } else {
            qrView.setImage(SwingFXUtils.toFXImage(
                    QRGenerator.render(QRGenerator.rentalPayload(rental.getRentalQrToken()), 220), null));
            qrLabel.setText(rental.displayId());
        }

        String status = rental.getStatus() == null ? "" : rental.getStatus().toUpperCase();
        returnButton.setDisable(true);
        returnButton.setVisible(false);
        returnButton.setManaged(false);
        switch (status) {
            case "PENDING_APPROVAL" -> {
                messageLabel.setText("Show this QR code to the BikeVault administrator to collect your bike.");
            }
            case "ACTIVE" -> {
                messageLabel.setText("ACTIVE RENTAL");
                returnButton.setText("RETURN BIKE");
                returnButton.setDisable(false);
                returnButton.setVisible(true);
                returnButton.setManaged(true);
            }
            case "RETURN_REQUESTED" -> {
                messageLabel.setText("✓ RETURN REQUESTED\nPlease show your rental QR to the administrator to complete the return.");
                returnButton.setText("SHOW RENTAL QR");
                returnButton.setVisible(true);
                returnButton.setManaged(true);
                returnButton.setDisable(false);
                returnButton.setOnAction(event -> onShowQr());
            }
            case "COMPLETED" -> messageLabel.setText("RENTAL COMPLETED\nBike:\n" + na(rental.getBikeLabel())
                    + "\nReturned:\n" + (rental.getActualReturnDate() == null ? "N/A" : rental.getActualReturnDate().format(WHEN)));
            case "REJECTED" -> messageLabel.setText("Rental rejected\nReason:\n" + na(rental.getRejectionReason()));
            default -> messageLabel.setText(rental.displayStatus());
        }
    }

    private void showLoading() {
        toggle(loadingPane, true);
        toggle(errorPane, false);
        toggle(contentPane, false);
    }

    private void showContent() {
        toggle(loadingPane, false);
        toggle(errorPane, false);
        toggle(contentPane, true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        toggle(loadingPane, false);
        toggle(errorPane, true);
        toggle(contentPane, false);
    }

    private void toggle(javafx.scene.Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private String na(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
