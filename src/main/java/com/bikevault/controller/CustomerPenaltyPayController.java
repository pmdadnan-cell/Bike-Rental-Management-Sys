package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.PenaltyContext;
import com.bikevault.model.Penalty;
import com.bikevault.service.PenaltyService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class CustomerPenaltyPayController {

    private final PenaltyService penaltyService = new PenaltyService();
    private Penalty penalty;

    @FXML private Label typeLabel;
    @FXML private Label rentalLabel;
    @FXML private Label bikeLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label amountLabel;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> methodCombo;
    @FXML private Button payButton;

    @FXML
    private void initialize() {
        methodCombo.getItems().setAll(AppConstants.PAYMENT_UPI, AppConstants.PAYMENT_CARD, AppConstants.PAYMENT_CASH);
        methodCombo.getSelectionModel().select(AppConstants.PAYMENT_UPI);
        Long id = PenaltyContext.getPenaltyId();
        if (id == null) {
            errorLabel.setText("Choose a penalty first.");
            payButton.setDisable(true);
            return;
        }
        try {
            penalty = penaltyService.findOwnById(id);
        } catch (RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
            payButton.setDisable(true);
            return;
        }
        typeLabel.setText(penalty.getPenaltyType() == null ? "N/A" : penalty.getPenaltyType());
        rentalLabel.setText(penalty.getRentalId() == null ? "N/A" : String.format("RV-%06d", penalty.getRentalId()));
        bikeLabel.setText(penalty.getBikeLabel() == null || penalty.getBikeLabel().isBlank() ? "N/A" : penalty.getBikeLabel());
        descriptionLabel.setText(penalty.getDescription() == null || penalty.getDescription().isBlank()
                ? "N/A" : penalty.getDescription());
        amountLabel.setText("₹ " + (penalty.getAmount() == null ? "0" : penalty.getAmount().toPlainString()));
        if (AppConstants.STATUS_PAID.equalsIgnoreCase(penalty.getStatus())) {
            payButton.setText("✓ PAID");
            payButton.setDisable(true);
            return;
        }
        payButton.setText("PAY ₹ " + (penalty.getAmount() == null ? "0" : penalty.getAmount().toPlainString()));
    }

    @FXML
    private void onPay() {
        if (penalty == null) {
            return;
        }
        try {
            penaltyService.payMine(penalty.getId(), methodCombo.getValue());
            PenaltyContext.clear();
            AlertUtil.info("✓ PAYMENT SUCCESSFUL", "Penalty payment completed.");
            NavigationUtil.showView(AppView.MY_PENALTIES);
        } catch (RuntimeException ex) {
            errorLabel.setText("Payment failed. No amount was charged.");
            payButton.setText("TRY AGAIN");
            AlertUtil.error("Payment failed", "Payment failed. No amount was charged.\n" + ex.getMessage());
        }
    }

    @FXML
    private void onBack() {
        NavigationUtil.showView(AppView.MY_PENALTIES);
    }
}
