package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Payment;
import com.bikevault.service.PaymentService;
import com.bikevault.util.UiAsync;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentController implements WorkspaceView {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final PaymentService paymentService = new PaymentService();
    private List<Payment> cache = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private Label todayRevenueLabel;
    @FXML private Label paidLabel;
    @FXML private FlowPane paymentPane;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        reload();
    }

    @FXML private void onSearch() { render(); }
    @FXML private void onRefresh() { reload(); }

    @Override public void addRecord() { }
    @Override public void saveRecord() { }
    @Override public void clearForm() { }

    private void reload() {
        UiAsync.run(paymentService::findAll, rows -> {
            cache = rows;
            render();
        });
    }

    private void render() {
        List<Payment> rows = paymentService.search(cache, searchField.getText());
        paymentPane.getChildren().clear();
        if (rows.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("empty-state");
            empty.getChildren().addAll(new Label("No payments"), new Label("Completed rental and penalty payments appear here."));
            paymentPane.getChildren().add(empty);
        } else {
            rows.forEach(payment -> paymentPane.getChildren().add(card(payment)));
        }
        long paid = cache.stream().filter(item -> AppConstants.STATUS_COMPLETED.equalsIgnoreCase(item.getStatus())).count();
        BigDecimal today = cache.stream()
                .filter(item -> AppConstants.STATUS_COMPLETED.equalsIgnoreCase(item.getStatus()))
                .filter(item -> item.getPaidAt() != null && item.getPaidAt().toLocalDate().equals(java.time.LocalDate.now()))
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        todayRevenueLabel.setText("TODAY ₹ " + today.toPlainString());
        paidLabel.setText("PAID " + paid);
    }

    private VBox card(Payment payment) {
        VBox box = new VBox(8);
        box.getStyleClass().add("approval-card");
        box.setPrefWidth(300);
        Label type = new Label(payment.displayType());
        type.getStyleClass().add("kicker-label");
        Label status = new Label("✓ " + payment.displayStatus());
        status.getStyleClass().add("status-badge");
        Label customer = new Label("Customer:\n" + na(payment.getCustomerName()));
        customer.getStyleClass().add("panel-title");
        Label rental = new Label("Rental:\n" + (payment.getRentalId() == null
                ? "N/A" : String.format("RV-%06d", payment.getRentalId())));
        Label extra = payment.isPenaltyPayment()
                ? new Label("Penalty:\n" + na(payment.penaltyLabel()))
                : new Label("Bike:\n" + na(payment.getBikeLabel()));
        extra.setWrapText(true);
        extra.getStyleClass().add("muted-label");
        Label amount = new Label("₹ " + (payment.getAmount() == null ? "0" : payment.getAmount().toPlainString()));
        amount.getStyleClass().add("panel-title");
        Label method = new Label("Method:\n" + na(payment.getMethod()));
        method.getStyleClass().add("muted-label");
        Label date = new Label("Date:\n" + (payment.getPaidAt() == null ? "N/A" : payment.getPaidAt().toLocalDate().format(DAY)));
        date.getStyleClass().add("muted-label");
        box.getChildren().addAll(type, status, customer, rental, extra, amount, method, date);
        return box;
    }

    private String na(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
