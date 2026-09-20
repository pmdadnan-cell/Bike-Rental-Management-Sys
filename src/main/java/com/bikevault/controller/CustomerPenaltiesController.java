package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.PenaltyContext;
import com.bikevault.model.Penalty;
import com.bikevault.service.PenaltyService;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerPenaltiesController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final PenaltyService penaltyService = new PenaltyService();

    @FXML private FlowPane penaltyPane;

    @FXML
    private void initialize() {
        List<Penalty> rows = penaltyService.findMine();
        penaltyPane.getChildren().clear();
        if (rows.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("trip-card");
            Label title = new Label("No penalties");
            title.getStyleClass().add("customer-title");
            empty.getChildren().addAll(title, new Label("You have no charges right now."));
            penaltyPane.getChildren().add(empty);
            return;
        }
        rows.forEach(penalty -> penaltyPane.getChildren().add(card(penalty)));
    }

    private VBox card(Penalty penalty) {
        VBox box = new VBox(8);
        box.getStyleClass().add("trip-card");
        box.setPrefWidth(300);
        boolean paid = AppConstants.STATUS_PAID.equalsIgnoreCase(penalty.getStatus());
        String type = penalty.getPenaltyType() == null ? "CHARGE" : penalty.getPenaltyType();
        Label heading = new Label((paid ? "✓ " : "⚠ ") + type);
        heading.getStyleClass().add("bike-card-model");
        Label bike = new Label(blank(penalty.getBikeLabel()));
        Label rental = new Label("Rental " + (penalty.getRentalId() == null
                ? "N/A" : String.format("RV-%06d", penalty.getRentalId())));
        Label description = new Label(blank(penalty.getDescription()));
        description.setWrapText(true);
        Label amount = new Label("₹ " + (penalty.getAmount() == null ? "0" : penalty.getAmount().toPlainString()));
        amount.getStyleClass().add("bike-card-price");
        Label badge = new Label(paid ? "✓ PAID" : "● UNPAID");
        badge.getStyleClass().add("status-badge");
        box.getChildren().addAll(heading, bike, rental, description, amount, badge);
        if (paid && penalty.getResolvedAt() != null) {
            box.getChildren().add(new Label(penalty.getResolvedAt().toLocalDate().format(DAY)));
        } else if (!paid) {
            Button pay = new Button("PAY PENALTY");
            pay.getStyleClass().add("primary-button");
            pay.setOnAction(event -> {
                PenaltyContext.select(penalty.getId());
                NavigationUtil.showView(AppView.CUSTOMER_PENALTY_PAY);
            });
            box.getChildren().add(pay);
        }
        return box;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
