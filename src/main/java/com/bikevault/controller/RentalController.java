package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.ScanContext;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Penalty;
import com.bikevault.model.Rental;
import com.bikevault.service.PenaltyService;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.UiAsync;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RentalController implements WorkspaceView {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final RentalService rentalService = new RentalService();
    private final PenaltyService penaltyService = new PenaltyService();
    private List<Rental> cache = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private FlowPane returnPane;
    @FXML private FlowPane rentalPane;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        statusFilter.getItems().setAll("ALL", AppConstants.STATUS_PENDING_APPROVAL, AppConstants.STATUS_ACTIVE,
                AppConstants.STATUS_RETURN_REQUESTED, AppConstants.STATUS_COMPLETED, AppConstants.STATUS_REJECTED);
        statusFilter.getSelectionModel().select("ALL");
        ScanContext.takePendingRental().ifPresent(this::afterReturnScan);
        reload();
    }

    @FXML private void onSearch() { render(); }
    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onScanReturnQr() {
        ScanContext.requestReturnScan(AppView.RENTALS);
    }

    @Override public void addRecord() { }
    @Override public void saveRecord() { }
    @Override public void clearForm() { }

    private void reload() {
        UiAsync.run(rentalService::findAll, rows -> {
            cache = rows;
            render();
        });
    }

    private void render() {
        List<Rental> filtered = rentalService.search(cache, searchField.getText(), statusFilter.getValue());
        rentalPane.getChildren().clear();
        returnPane.getChildren().clear();
        List<Rental> returns = cache.stream()
                .filter(item -> AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(item.getStatus())
                        || item.isReturnRequested())
                .toList();
        if (returns.isEmpty()) {
            returnPane.getChildren().add(emptyCard("No return requests", "Customer return requests appear here."));
        } else {
            returns.forEach(rental -> returnPane.getChildren().add(returnCard(rental)));
        }
        if (filtered.isEmpty()) {
            rentalPane.getChildren().add(emptyCard("No rentals found", "New paid requests appear after checkout."));
            return;
        }
        filtered.forEach(rental -> rentalPane.getChildren().add(rentalCard(rental)));
    }

    private VBox rentalCard(Rental rental) {
        VBox box = baseCard(rental);
        HBox actions = new HBox(8);
        Button view = action("VIEW", "ghost-button");
        view.setOnAction(event -> showDetails(rental));
        actions.getChildren().add(view);
        if (AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus()) || rental.isReturnRequested()) {
            Button ret = action("RETURN REQUEST", "secondary-button");
            ret.setOnAction(event -> acceptReturn(rental));
            actions.getChildren().add(ret);
        }
        box.getChildren().add(actions);
        return box;
    }

    private VBox returnCard(Rental rental) {
        VBox box = new VBox(8);
        box.getStyleClass().addAll("approval-card", "rental-card");
        box.setPrefWidth(360);
        Label kicker = new Label("RETURN REQUEST");
        kicker.getStyleClass().add("kicker-label");
        Label status = badge(rental);
        HBox top = new HBox(8, kicker, spacer(), status);
        top.setAlignment(Pos.CENTER_LEFT);
        Label customer = new Label("Customer:\n" + na(rental.getCustomerName()));
        customer.getStyleClass().add("panel-title");
        Label body = new Label("Bike:\n" + na(rental.getBikeLabel())
                + "\nRental:\n" + rental.displayId()
                + "\nStarted:\n" + day(rental.getStartDate())
                + "\nExpected return:\n" + day(rental.getExpectedReturnDate())
                + "\nStatus:\n" + rental.displayStatus());
        body.setWrapText(true);
        body.getStyleClass().add("muted-label");
        HBox actions = new HBox(8);
        Button view = action("VIEW RENTAL", "ghost-button");
        view.setOnAction(event -> showDetails(rental));
        Button accept = action("ACCEPT RETURN", "success-button");
        accept.setOnAction(event -> acceptReturn(rental));
        Button penalty = action("APPLY PENALTY", "secondary-button");
        penalty.setOnAction(event -> applyPenalty(rental));
        Button scan = action("SCAN RETURN QR", "primary-button");
        scan.setOnAction(event -> onScanReturnQr());
        actions.getChildren().addAll(view, scan, accept, penalty);
        box.getChildren().addAll(top, customer, body, actions);
        return box;
    }

    private VBox baseCard(Rental rental) {
        VBox box = new VBox(8);
        box.getStyleClass().addAll("approval-card", "rental-card");
        box.setPrefWidth(340);
        HBox top = new HBox(8, title(rental.displayId()), spacer(), badge(rental));
        top.setAlignment(Pos.CENTER_LEFT);
        Label customer = new Label(na(rental.getCustomerName()));
        customer.getStyleClass().add("panel-title");
        Label bike = new Label(na(rental.getBikeLabel()));
        bike.getStyleClass().add("muted-label");
        Label dates = new Label(day(rental.getStartDate()) + " → " + day(rental.getExpectedReturnDate()));
        dates.getStyleClass().add("muted-label");
        Label amount = new Label("₹ " + money(rental.getFinalAmount()));
        amount.getStyleClass().add("panel-title");
        box.getChildren().addAll(top, customer, bike, dates, amount);
        return box;
    }

    private void acceptReturn(Rental rental) {
        if (!AlertUtil.confirm("Accept this bike return?",
                "Customer:\n" + na(rental.getCustomerName())
                        + "\nBike:\n" + na(rental.getBikeLabel())
                        + "\nRental:\n" + rental.displayId(),
                "ACCEPT RETURN")) {
            return;
        }
        try {
            rentalService.acceptReturn(rental.getId());
            AlertUtil.info("RETURN ACCEPTED", "Bike has been returned successfully.");
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not accept return", ex.getMessage());
        }
    }

    private void applyPenalty(Rental rental) {
        Dialog<Penalty> dialog = new Dialog<>();
        dialog.setTitle("Apply penalty");
        dialog.setHeaderText("Charge " + na(rental.getCustomerName()) + " for " + rental.displayId());
        ButtonType apply = new ButtonType("APPLY PENALTY", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(apply, ButtonType.CANCEL);
        ComboBox<String> type = new ComboBox<>();
        type.getItems().setAll("LATE_RETURN", "DAMAGE", "TRAFFIC_CHALLAN", "OTHER");
        type.getSelectionModel().selectFirst();
        TextField amount = new TextField();
        amount.setPromptText("Amount");
        TextArea description = new TextArea();
        description.setPromptText("Description");
        description.setPrefRowCount(3);
        VBox form = new VBox(8, new Label("Type"), type, new Label("Amount"), amount,
                new Label("Description"), description);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != apply) {
                return null;
            }
            Penalty penalty = new Penalty();
            penalty.setCustomerId(rental.getCustomerId());
            penalty.setRentalId(rental.getId());
            penalty.setBikeId(rental.getBikeId());
            penalty.setPenaltyType(type.getValue());
            penalty.setDescription(description.getText());
            penalty.setAmount(new BigDecimal(amount.getText().trim()));
            penalty.setStatus(AppConstants.STATUS_PENDING);
            return penalty;
        });
        dialog.showAndWait().ifPresent(penalty -> {
            try {
                penaltyService.add(penalty);
                AlertUtil.info("Penalty applied", "The customer can see this charge under My Penalties.");
            } catch (RuntimeException ex) {
                AlertUtil.error("Could not apply penalty", ex.getMessage());
            }
        });
    }

    private void afterReturnScan(Rental scanned) {
        try {
            Rental rental = rentalService.findOwnById(scanned.getId());
            String status = rental.getStatus() == null ? "" : rental.getStatus().toUpperCase();
            String message = switch (status) {
                case "PENDING_APPROVAL" -> "This rental has not been approved yet.";
                case "ACTIVE" -> "Customer must request a return before completing the return.";
                case "RETURN_REQUESTED" -> "✓ QR VERIFIED";
                case "COMPLETED" -> "This rental has already been completed.";
                case "REJECTED" -> "This rental was rejected.";
                default -> rental.displayStatus();
            };
            if (!AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())) {
                AlertUtil.warn("Return QR", message + "\n" + rental.displayId());
                return;
            }
            int late = rentalService.lateDaysFor(rental);
            BigDecimal unpaid = rentalService.unpaidPenaltyTotal(rental.getId());
            String lateLine = late > 0 ? "\n⚠ LATE RETURN\nLate by: " + late + " day(s)" : "";
            AlertUtil.info("RETURN VERIFICATION",
                    message
                            + "\nCustomer: " + na(rental.getCustomerName())
                            + "\nRental ID: " + rental.displayId()
                            + "\nBike: " + na(rental.getBikeLabel())
                            + "\nRental period: " + day(rental.getStartDate()) + " → " + day(rental.getExpectedReturnDate())
                            + "\nPayment: ₹ " + money(rental.getFinalAmount())
                            + "\nPenalty: ₹ " + money(unpaid)
                            + "\nStatus: " + rental.displayStatus()
                            + lateLine);
            acceptReturn(rental);
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not load rental", ex.getMessage());
        }
    }

    private void showDetails(Rental rental) {
        AlertUtil.info(rental.displayId(),
                "Customer: " + na(rental.getCustomerName())
                        + "\nBike: " + na(rental.getBikeLabel())
                        + "\nStatus: " + rental.displayStatus()
                        + "\n" + day(rental.getStartDate()) + " → " + day(rental.getExpectedReturnDate())
                        + "\n₹ " + money(rental.getFinalAmount())
                        + "\nPayment: " + na(rental.getPaymentStatus()));
    }

    private VBox emptyCard(String title, String hint) {
        VBox empty = new VBox(6);
        empty.getStyleClass().add("empty-state");
        empty.setPrefWidth(320);
        Label heading = new Label(title);
        heading.getStyleClass().add("panel-title");
        Label body = new Label(hint);
        body.getStyleClass().add("muted-label");
        empty.getChildren().addAll(heading, body);
        return empty;
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("panel-title");
        return label;
    }

    private Label badge(Rental rental) {
        Label badge = new Label(rental.displayStatus());
        badge.getStyleClass().addAll("status-badge", badgeClass(rental.getStatus()));
        return badge;
    }

    private String badgeClass(String status) {
        if (AppConstants.STATUS_ACTIVE.equalsIgnoreCase(status)) {
            return "badge-success";
        }
        if (AppConstants.STATUS_PENDING_APPROVAL.equalsIgnoreCase(status)
                || AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(status)) {
            return "badge-warning";
        }
        if (AppConstants.STATUS_REJECTED.equalsIgnoreCase(status)) {
            return "badge-danger";
        }
        return "badge-muted";
    }

    private Button action(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setMinWidth(110);
        button.setEllipsisString("");
        return button;
    }

    private HBox spacer() {
        HBox box = new HBox();
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String day(java.time.LocalDate value) {
        return value == null ? "N/A" : value.format(DAY);
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private String na(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
