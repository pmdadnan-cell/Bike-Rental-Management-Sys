package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Customer;
import com.bikevault.model.Penalty;
import com.bikevault.service.CustomerService;
import com.bikevault.service.PenaltyService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.UiAsync;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PenaltyController implements WorkspaceView {

    private final PenaltyService penaltyService = new PenaltyService();
    private final CustomerService customerService = new CustomerService();
    private List<Penalty> penalties = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField amountField;
    @FXML private TextField rentalField;
    @FXML private TextField bikeField;
    @FXML private TextArea descriptionArea;
    @FXML private FlowPane penaltyPane;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        typeCombo.getItems().setAll("LATE_RETURN", "DAMAGE", "TRAFFIC_CHALLAN", "OTHER");
        typeCombo.getSelectionModel().selectFirst();
        statusCombo.getItems().setAll("ALL", AppConstants.STATUS_PENDING, AppConstants.STATUS_PAID,
                AppConstants.STATUS_WAIVED, AppConstants.STATUS_DISPUTED);
        statusCombo.getSelectionModel().select("ALL");
        reload();
    }

    @FXML private void onSearch() { render(); }
    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        try {
            penaltyService.add(readPenalty());
            AlertUtil.info("Penalty applied", "The customer can see this charge under My Penalties.");
            clearForm();
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not add penalty", ex.getMessage());
        }
    }

    @Override public void addRecord() { onAdd(); }
    @Override public void saveRecord() { onAdd(); }
    @Override public void clearForm() {
        descriptionArea.clear();
        amountField.clear();
        rentalField.clear();
        bikeField.clear();
    }

    private Penalty readPenalty() {
        Penalty penalty = new Penalty();
        Customer customer = customerCombo.getValue();
        if (customer != null) {
            penalty.setCustomerId(customer.getId());
        }
        penalty.setPenaltyType(typeCombo.getValue());
        penalty.setAmount(new BigDecimal(amountField.getText().trim()));
        penalty.setDescription(descriptionArea.getText());
        if (rentalField.getText() != null && !rentalField.getText().isBlank()) {
            penalty.setRentalId(Long.parseLong(rentalField.getText().trim()));
        }
        if (bikeField.getText() != null && !bikeField.getText().isBlank()) {
            penalty.setBikeId(Long.parseLong(bikeField.getText().trim()));
        }
        penalty.setStatus(AppConstants.STATUS_PENDING);
        return penalty;
    }

    private void reload() {
        UiAsync.run(customerService::findAll, rows -> customerCombo.getItems().setAll(rows));
        UiAsync.run(penaltyService::findAll, rows -> {
            penalties = rows;
            render();
        });
    }

    private void render() {
        penaltyPane.getChildren().clear();
        List<Penalty> rows = penaltyService.search(penalties, searchField.getText(), statusCombo.getValue());
        if (rows.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("empty-state");
            empty.getChildren().addAll(new Label("No penalties"), new Label("Apply a charge from a return request or this form."));
            penaltyPane.getChildren().add(empty);
            return;
        }
        rows.forEach(penalty -> penaltyPane.getChildren().add(card(penalty)));
    }

    private VBox card(Penalty penalty) {
        VBox box = new VBox(8);
        box.getStyleClass().add("approval-card");
        box.setPrefWidth(280);
        Label type = new Label("⚠ " + (penalty.getPenaltyType() == null ? "CHARGE" : penalty.getPenaltyType()));
        type.getStyleClass().add("panel-title");
        Label bike = new Label(penalty.getBikeLabel() == null || penalty.getBikeLabel().isBlank()
                ? (penalty.getCustomerName() == null ? "Customer" : penalty.getCustomerName())
                : penalty.getBikeLabel());
        bike.getStyleClass().add("muted-label");
        Label amount = new Label("₹ " + (penalty.getAmount() == null ? "0" : penalty.getAmount().toPlainString()));
        amount.getStyleClass().add("panel-title");
        Label status = new Label("Status: " + (penalty.getStatus() == null ? "UNPAID" : penalty.getStatus()));
        status.getStyleClass().add("status-badge");
        HBox actions = new HBox(8);
        Button view = new Button("VIEW DETAILS");
        view.getStyleClass().add("ghost-button");
        view.setOnAction(event -> AlertUtil.info("Penalty details",
                (penalty.getPenaltyType() == null ? "CHARGE" : penalty.getPenaltyType())
                        + "\n₹ " + penalty.getAmount()
                        + "\n" + (penalty.getDescription() == null || penalty.getDescription().isBlank()
                        ? "N/A" : penalty.getDescription())
                        + "\n" + (penalty.getCustomerName() == null ? "" : penalty.getCustomerName())));
        Button paid = new Button("MARK PAID");
        paid.getStyleClass().add("secondary-button");
        paid.setOnAction(event -> {
            penaltyService.markPaid(penalty);
            reload();
        });
        actions.getChildren().addAll(view, paid);
        box.getChildren().addAll(type, bike, amount, status, actions);
        return box;
    }
}
