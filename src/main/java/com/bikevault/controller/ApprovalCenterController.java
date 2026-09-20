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
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ApprovalCenterController implements WorkspaceView {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final RentalService rentalService = new RentalService();
    private final PenaltyService penaltyService = new PenaltyService();

    @FXML private VBox scannerPane;
    @FXML private VBox verifyPane;
    @FXML private VBox requestPane;
    @FXML private Label verifyTitle;
    @FXML private Label verifyBadge;
    @FXML private Label verifyBody;
    @FXML private HBox collectionActions;
    @FXML private HBox returnActions;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Button acceptReturnButton;
    @FXML private Button rejectReturnButton;

    private Rental selected;
    private boolean returnScan;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        returnScan = ScanContext.isReturnMode();
        ScanContext.takePendingRental().ifPresent(this::select);
        reload();
    }

    @FXML
    private void onScanRentalQr() {
        returnScan = false;
        ScanContext.requestRentalScan(AppView.APPROVAL_CENTER);
    }

    @FXML
    private void onScanReturnQr() {
        returnScan = true;
        ScanContext.requestReturnScan(AppView.APPROVAL_CENTER);
    }

    @FXML
    private void onApprove() {
        if (selected == null || !AccessGuard.requireAdmin("approve a rental")) {
            return;
        }
        if (!AlertUtil.confirm("Approve this rental?",
                "Customer:\n" + na(selected.getCustomerName())
                        + "\nBike:\n" + na(selected.getBikeLabel())
                        + "\nRental:\n" + selected.displayId(),
                "APPROVE")) {
            return;
        }
        try {
            Rental saved = rentalService.approve(selected.getId());
            selected = saved;
            present(saved, "✓ RENTAL APPROVED");
            AlertUtil.info("RENTAL APPROVED", "Bike is authorized for collection.");
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Approval failed", ex.getMessage());
        }
    }

    @FXML
    private void onReject() {
        if (selected == null || !AccessGuard.requireAdmin("reject a rental")) {
            return;
        }
        var reason = AlertUtil.promptReason("Reason for rejection",
                "This rental will be rejected and the bike stays available.",
                "Reject Rental");
        if (reason.isEmpty()) {
            return;
        }
        try {
            Rental saved = rentalService.reject(selected.getId(), reason.get(), null);
            selected = saved;
            present(saved, "This rental was rejected.");
            AlertUtil.info("Rental rejected", saved.displayId() + " is rejected. The bike remains AVAILABLE.");
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Rejection failed", ex.getMessage());
        }
    }

    @FXML
    private void onAcceptReturn() {
        if (selected == null || !AccessGuard.requireAdmin("accept a return")) {
            return;
        }
        if (!AlertUtil.confirm("Accept this bike return?",
                "Customer:\n" + na(selected.getCustomerName())
                        + "\nBike:\n" + na(selected.getBikeLabel())
                        + "\nRental:\n" + selected.displayId(),
                "ACCEPT RETURN")) {
            return;
        }
        try {
            Rental saved = rentalService.acceptReturn(selected.getId());
            selected = saved;
            present(saved, "✓ RETURN ACCEPTED");
            AlertUtil.info("RETURN ACCEPTED", "Bike has been returned successfully.");
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Return failed", ex.getMessage());
        }
    }

    @FXML
    private void onRejectReturn() {
        if (selected == null || !AccessGuard.requireAdmin("reject a return")) {
            return;
        }
        var reason = AlertUtil.promptReason("Reject return",
                "The bike stays with the customer. Status remains RETURN REQUESTED.",
                "Reject Return");
        if (reason.isEmpty()) {
            return;
        }
        try {
            selected = rentalService.rejectReturn(selected.getId(), reason.get());
            present(selected, "Return not accepted");
            AlertUtil.info("Return rejected", "The rental stays RETURN REQUESTED.");
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not reject return", ex.getMessage());
        }
    }

    @FXML
    private void onApplyPenalty() {
        if (selected == null) {
            return;
        }
        applyPenalty(selected);
    }

    @FXML
    private void onViewCustomer() {
        NavigationUtil.showView(AppView.CUSTOMERS);
    }

    @FXML
    private void onViewBike() {
        NavigationUtil.showView(AppView.BIKES);
    }

    private void reload() {
        List<Rental> rows = rentalService.findPendingApprovals();
        requestPane.getChildren().clear();
        if (rows.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("empty-state");
            Label title = new Label("No pending requests");
            title.getStyleClass().add("panel-title");
            Label hint = new Label("Scan a rental QR for collection, or SCAN RETURN QR when a customer comes back.");
            hint.getStyleClass().add("muted-label");
            empty.getChildren().addAll(title, hint);
            requestPane.getChildren().add(empty);
            return;
        }
        rows.forEach(rental -> requestPane.getChildren().add(card(rental)));
    }

    private VBox card(Rental rental) {
        VBox box = new VBox(8);
        box.getStyleClass().add("approval-card");
        Label id = new Label(rental.displayId());
        id.getStyleClass().add("kicker-label");
        Label customer = new Label(na(rental.getCustomerName()));
        customer.getStyleClass().add("panel-title");
        Label body = new Label(na(rental.getBikeLabel())
                + "\n" + day(rental.getStartDate()) + " → " + day(rental.getExpectedReturnDate())
                + "\n₹ " + money(rental.getFinalAmount()));
        body.setWrapText(true);
        body.getStyleClass().add("muted-label");
        HBox actions = new HBox(8);
        Button view = compact("VIEW", "ghost-button");
        view.setOnAction(event -> select(rental));
        actions.getChildren().add(view);
        box.getChildren().addAll(id, customer, body, actions);
        return box;
    }

    private Button compact(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setMinWidth(96);
        button.setEllipsisString("");
        return button;
    }

    private void select(Rental rental) {
        try {
            selected = rentalService.findOwnById(rental.getId());
            present(selected, statusMessage(selected));
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not load rental", ex.getMessage());
        }
    }

    private void present(Rental rental, String badge) {
        selected = rental;
        scannerPane.setVisible(false);
        scannerPane.setManaged(false);
        verifyPane.setVisible(true);
        verifyPane.setManaged(true);
        boolean returning = AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus());
        boolean pending = AppConstants.STATUS_PENDING_APPROVAL.equalsIgnoreCase(rental.getStatus());
        collectionActions.setVisible(!returning);
        collectionActions.setManaged(!returning);
        returnActions.setVisible(returning);
        returnActions.setManaged(returning);
        approveButton.setDisable(!pending);
        rejectButton.setDisable(!pending);
        if (acceptReturnButton != null) {
            acceptReturnButton.setDisable(!returning);
        }
        if (rejectReturnButton != null) {
            rejectReturnButton.setDisable(!returning);
        }
        verifyTitle.setText(returning ? "RETURN VERIFICATION" : "RENTAL VERIFICATION");
        verifyBadge.setText(badge);
        verifyBody.setText(returning ? returnBody(rental) : collectionBody(rental));
    }

    private String collectionBody(Rental rental) {
        return "Rental ID\n" + rental.displayId()
                + "\n\nCUSTOMER\n" + na(rental.getCustomerName())
                + "\n\nBIKE\n" + na(rental.getBikeLabel())
                + "\n\nREGISTRATION\n" + na(rental.getRegistrationNumber())
                + "\n\nRENTAL PERIOD\n" + day(rental.getStartDate()) + " → " + day(rental.getExpectedReturnDate())
                + "\n\nPAYMENT\n₹ " + money(rental.getFinalAmount())
                + "\n\nPAYMENT STATUS\n" + ("PAID".equalsIgnoreCase(rental.getPaymentStatus())
                ? "✓ PAID" : na(rental.getPaymentStatus()))
                + "\n\nRENTAL STATUS\n" + rental.displayStatus();
    }

    private String returnBody(Rental rental) {
        int late = rentalService.lateDaysFor(rental);
        BigDecimal unpaid = rentalService.unpaidPenaltyTotal(rental.getId());
        String lateLine = late > 0
                ? "\n\n⚠ LATE RETURN\nLate by:\n" + late + " day(s)"
                : "";
        return "Customer:\n" + na(rental.getCustomerName())
                + "\n\nRental ID:\n" + rental.displayId()
                + "\n\nBike:\n" + na(rental.getBikeLabel())
                + "\n\nRental period:\n" + day(rental.getStartDate()) + " → " + day(rental.getExpectedReturnDate())
                + "\n\nExpected return:\n" + day(rental.getExpectedReturnDate())
                + "\n\nActual return:\n" + day(LocalDate.now())
                + "\n\nPayment:\n₹ " + money(rental.getFinalAmount())
                + "\n\nPenalty:\n₹ " + money(unpaid)
                + "\n\nRental status:\n" + rental.displayStatus()
                + lateLine;
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
        dialog.getDialogPane().setContent(new VBox(8,
                new Label("Type"), type, new Label("Amount"), amount,
                new Label("Description"), description));
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
                AlertUtil.info("Penalty applied", "The customer can pay this from My Penalties.");
                present(rentalService.findOwnById(rental.getId()), statusMessage(rental));
            } catch (RuntimeException ex) {
                AlertUtil.error("Could not apply penalty", ex.getMessage());
            }
        });
    }

    private String statusMessage(Rental rental) {
        String status = rental.getStatus() == null ? "" : rental.getStatus().toUpperCase();
        if (returnScan) {
            return switch (status) {
                case "PENDING_APPROVAL" -> "This rental has not been approved yet.";
                case "ACTIVE" -> "Customer must request a return before completing the return.";
                case "RETURN_REQUESTED" -> "✓ QR VERIFIED";
                case "COMPLETED" -> "This rental has already been completed.";
                case "REJECTED" -> "This rental was rejected.";
                default -> "✓ QR VERIFIED";
            };
        }
        return switch (status) {
            case "PENDING_APPROVAL" -> "✓ QR VERIFIED";
            case "ACTIVE", "RETURN_REQUESTED" -> "This rental has already been approved.";
            case "COMPLETED" -> "This rental is already completed.";
            case "REJECTED" -> "This rental was rejected.";
            default -> "✓ QR VERIFIED";
        };
    }

    private String day(LocalDate value) {
        return value == null ? "N/A" : value.format(DAY);
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private String na(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    @Override
    public void onLeave() {
        // camera lives on the scanner screen
    }
}
