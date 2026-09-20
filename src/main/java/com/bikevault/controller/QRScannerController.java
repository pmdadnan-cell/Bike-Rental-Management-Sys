package com.bikevault.controller;

import com.bikevault.app.AppView;
import com.bikevault.app.ScanContext;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Bike;
import com.bikevault.model.QrVerification;
import com.bikevault.model.Rental;
import com.bikevault.service.QRService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.QRDecoder;
import com.bikevault.util.UiAsync;
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScannerController implements WorkspaceView {

    private static final Logger LOG = LoggerFactory.getLogger(QRScannerController.class);

    private final QRService qrService = new QRService();
    private final AtomicBoolean cameraRunning = new AtomicBoolean(false);
    private Webcam webcam;
    private volatile boolean lockDetection;

    @FXML private ImageView cameraView;
    @FXML private Label cameraHint;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button scanButton;
    @FXML private TextField manualField;
    @FXML private Label statusLabel;
    @FXML private Label verifyTitle;
    @FXML private Label bikeIdLabel;
    @FXML private Label modelLabel;
    @FXML private Label registrationLabel;
    @FXML private Label statusValueLabel;
    @FXML private Label scannerTitle;
    @FXML private Label scannerHint;

    @FXML
    private void initialize() {
        stopButton.setDisable(true);
        statusLabel.setText("Waiting for QR...");
        resetResult();
        if (ScanContext.isRentalMode()) {
            scannerTitle.setText("SCAN RENTAL QR");
            scannerHint.setText("Customer shows their unique rental pass. The token is verified in MySQL before approval.");
            statusLabel.setText("Waiting for a rental QR…");
        }
    }

    @FXML
    private void onStartCamera() {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                AlertUtil.warn("No camera", "No webcam was found. Paste a QR payload below to verify.");
                cameraHint.setText("No webcam available");
                return;
            }
            if (!webcam.isOpen()) {
                try {
                    webcam.setViewSize(new Dimension(640, 480));
                } catch (RuntimeException ignored) {
                    // Some drivers reject a fixed size; opening with the default is enough.
                }
                webcam.open();
            }
            cameraRunning.set(true);
            startButton.setDisable(true);
            stopButton.setDisable(false);
            cameraHint.setText("");
            statusLabel.setText(ScanContext.isRentalMode()
                    ? "Camera live — looking for a rental QR…"
                    : "Camera live — looking for a bike QR…");
            Thread loop = new Thread(this::cameraLoop, "bikevault-webcam");
            loop.setDaemon(true);
            loop.start();
        } catch (RuntimeException ex) {
            LOG.error("Unable to start webcam", ex);
            AlertUtil.error("Camera error", "The webcam could not be started. You can still paste a QR payload.");
        }
    }

    @FXML
    private void onStopCamera() {
        stopCamera();
        statusLabel.setText("Camera stopped. Waiting for QR...");
        cameraHint.setText("Camera idle");
    }

    @FXML
    private void onScanDetect() {
        if (webcam == null || !webcam.isOpen()) {
            AlertUtil.warn("Start the camera", "Start the camera first, or paste a QR payload.");
            return;
        }
        BufferedImage image = webcam.getImage();
        QRDecoder.decodeImage(image).ifPresentOrElse(text -> handleRawText(text, false),
                () -> statusLabel.setText("No QR detected in this frame."));
    }

    @FXML
    private void onManualVerify() {
        handleRawText(manualField.getText(), false);
    }

    @Override
    public void onLeave() {
        stopCamera();
    }

    private void cameraLoop() {
        while (cameraRunning.get() && webcam != null && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            if (image != null) {
                Platform.runLater(() -> cameraView.setImage(SwingFXUtils.toFXImage(image, null)));
                if (!lockDetection) {
                    QRDecoder.decodeImage(image).ifPresent(text -> handleRawText(text, true));
                }
            }
            try {
                Thread.sleep(140);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void handleRawText(String raw, boolean fromCamera) {
        if (raw == null || raw.isBlank() || lockDetection) {
            return;
        }
        lockDetection = true;
        UiAsync.run(() -> qrService.verify(raw), verification -> {
            present(verification, fromCamera);
            if (verification.getStatus() == QrVerification.Status.INVALID) {
                lockDetection = false;
            }
        }, error -> {
            lockDetection = false;
            AlertUtil.error("QR verification failed", error.getMessage());
        });
    }

    private void present(QrVerification verification, boolean fromCamera) {
        if (verification.getStatus() == QrVerification.Status.INVALID) {
            resetResult();
            verifyTitle.setText("INVALID QR");
            verifyTitle.getStyleClass().removeAll("status-available", "status-maintenance");
            verifyTitle.getStyleClass().add("status-maintenance");
            String message = verification.getMessage() == null
                    ? "Invalid or unregistered QR Code." : verification.getMessage();
            if (ScanContext.isRentalMode()) {
                message = rentalScanMessage(message);
            }
            statusLabel.setText(message);
            if (!fromCamera) {
                AlertUtil.error("Invalid QR", message);
            }
            return;
        }

        if (verification.isRentalQr()) {
            presentRental(verification, fromCamera);
            return;
        }
        if (ScanContext.isRentalMode()) {
            resetResult();
            verifyTitle.setText("INVALID QR");
            statusLabel.setText("Invalid BikeVault rental QR.");
            if (!fromCamera) {
                AlertUtil.error("Invalid QR", "Invalid BikeVault rental QR.");
            }
            return;
        }

        Bike bike = verification.getBike();
        if (verification.getStatus() == QrVerification.Status.INACTIVE) {
            populateResult(bike);
            verifyTitle.setText("BIKE INACTIVE");
            verifyTitle.getStyleClass().removeAll("status-available", "status-maintenance");
            verifyTitle.getStyleClass().add("status-maintenance");
            statusLabel.setText("This QR is registered but the bike is inactive.");
            AlertUtil.warn("Bike inactive", "This QR is registered but the bike is inactive.");
            return;
        }

        populateResult(bike);
        verifyTitle.setText("QR VERIFIED");
        verifyTitle.getStyleClass().removeAll("status-available", "status-maintenance");
        verifyTitle.getStyleClass().add("status-available");
        statusLabel.setText("Bike verified against MySQL.");
        AlertUtil.info("QR code successfully verified.",
                "Bike ID: " + bike.displayId()
                        + "\nModel: " + bike.getBrand() + " " + bike.getModel()
                        + "\nStatus: " + bike.getStatus());
        stopCamera();
        if (ScanContext.getReturnView() != AppView.QR_SCANNER) {
            ScanContext.complete(bike);
        }
    }

    private void presentRental(QrVerification verification, boolean fromCamera) {
        Rental rental = verification.getRental();
        Bike bike = verification.getBike();
        verifyTitle.setText(verification.getMessage());
        verifyTitle.getStyleClass().removeAll("status-available", "status-maintenance");
        verifyTitle.getStyleClass().add("status-available");
        bikeIdLabel.setText("Rental ID: " + rental.displayId());
        modelLabel.setText("Customer: " + rental.getCustomerName());
        registrationLabel.setText("Bike: " + rental.getBikeLabel()
                + (bike == null ? "" : "  ·  " + bike.getQrCode()));
        statusValueLabel.setText("Status: " + rental.displayStatus()
                + "  ·  Payment: " + (rental.getPaymentStatus() == null ? "—" : rental.getPaymentStatus()));
        statusLabel.setText("Rental token verified against MySQL.");
        if (!fromCamera) {
            AlertUtil.info("Rental QR valid",
                    rental.displayId() + "\n" + rental.getCustomerName() + "\n" + rental.getBikeLabel());
        }
        stopCamera();
        if (ScanContext.getReturnView() != AppView.QR_SCANNER) {
            ScanContext.completeRental(rental);
        }
    }

    private void populateResult(Bike bike) {
        bikeIdLabel.setText("Bike ID: " + bike.displayId());
        modelLabel.setText("Model: " + bike.getBrand() + " " + bike.getModel());
        registrationLabel.setText("Registration: " + bike.getRegistrationNumber());
        statusValueLabel.setText("Status: " + bike.getStatus());
    }

    private void resetResult() {
        verifyTitle.setText("Scan a bike to verify");
        bikeIdLabel.setText("Bike ID: —");
        modelLabel.setText("Model: —");
        registrationLabel.setText("Registration: —");
        statusValueLabel.setText("Status: —");
    }

    private String rentalScanMessage(String message) {
        if (message == null) {
            return "Invalid BikeVault rental QR.";
        }
        String lower = message.toLowerCase();
        if (lower.contains("not found")) {
            return "Rental not found.";
        }
        if (lower.contains("already been approved") || lower.contains("already completed")
                || lower.contains("has been rejected") || lower.contains("was rejected")
                || lower.contains("not been approved") || lower.contains("must request a return")
                || lower.contains("invalid bikevault")) {
            return message;
        }
        return "Invalid BikeVault rental QR.";
    }

    private void stopCamera() {
        cameraRunning.set(false);
        startButton.setDisable(false);
        stopButton.setDisable(true);
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        webcam = null;
    }
}
