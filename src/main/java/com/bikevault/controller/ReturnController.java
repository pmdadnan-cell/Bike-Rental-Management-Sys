package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.ScanContext;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Bike;
import com.bikevault.model.Rental;
import com.bikevault.model.ReturnRecord;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;

public class ReturnController implements WorkspaceView {

    private final RentalService rentalService = new RentalService();
    private Bike scannedBike;
    private Rental activeRental;

    @FXML private TextField bikeIdField;
    @FXML private TextField qrField;
    @FXML private TextField customerField;
    @FXML private TextField rentalIdField;
    @FXML private TextField startField;
    @FXML private TextField expectedField;
    @FXML private DatePicker actualPicker;
    @FXML private TextField durationField;
    @FXML private TextField lateDaysField;
    @FXML private TextField lateFeeField;
    @FXML private TextField baseField;
    @FXML private TextField finalField;
    @FXML private ComboBox<String> methodCombo;
    @FXML private CheckBox collectLateCheck;
    @FXML private TextArea notesArea;
    @FXML private TableView<ReturnRecord> table;
    @FXML private TableColumn<ReturnRecord, String> idColumn;
    @FXML private TableColumn<ReturnRecord, String> rentalColumn;
    @FXML private TableColumn<ReturnRecord, String> customerColumn;
    @FXML private TableColumn<ReturnRecord, String> bikeColumn;
    @FXML private TableColumn<ReturnRecord, String> dateColumn;
    @FXML private TableColumn<ReturnRecord, String> lateColumn;
    @FXML private TableColumn<ReturnRecord, String> finalColumn;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        methodCombo.getItems().setAll(AppConstants.PAYMENT_CASH, AppConstants.PAYMENT_CARD,
                AppConstants.PAYMENT_UPI, AppConstants.PAYMENT_NET_BANKING);
        methodCombo.getSelectionModel().select(AppConstants.PAYMENT_CASH);
        actualPicker.setValue(LocalDate.now());
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getId())));
        rentalColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getRentalId())));
        customerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));
        bikeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBikeLabel()));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getReturnDate())));
        lateColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getLateFee())));
        finalColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getFinalAmount())));
        table.setPlaceholder(new javafx.scene.control.Label("No returns recorded yet."));
        reload();
        ScanContext.takePending().ifPresent(this::acceptScannedBike);
    }

    @FXML private void onScanQr() { ScanContext.requestScan(AppView.RETURNS); }
    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onCalculate() {
        try {
            if (scannedBike == null) {
                AlertUtil.warn("Scan a bike", "Scan the bike QR to load its active rental.");
                return;
            }
            ReturnRecord preview = rentalService.previewReturn(scannedBike, actualPicker.getValue());
            durationField.setText(value(preview.getActualDurationDays()));
            lateDaysField.setText(value(preview.getLateDays()));
            lateFeeField.setText(value(preview.getLateFee()));
            baseField.setText(activeRental == null ? "" : value(activeRental.getTotalAmount()));
            finalField.setText(value(preview.getFinalAmount()));
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML
    private void onReturnBike() {
        try {
            if (scannedBike == null || activeRental == null) {
                AlertUtil.warn("Scan a bike", "Scan a rented bike QR first.");
                return;
            }
            onCalculate();
            if (!AlertUtil.confirm("Confirm return",
                    "Return " + scannedBike.displayId() + " for " + customerField.getText()
                            + "?\nFinal amount: " + finalField.getText())) {
                return;
            }
            rentalService.completeReturn(scannedBike, actualPicker.getValue(), notesArea.getText(),
                    methodCombo.getValue(), collectLateCheck.isSelected());
            AlertUtil.info("Bike returned", scannedBike.displayId() + " is now AVAILABLE.");
            clearForm();
            reload();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML public void onClear() { clearForm(); }

    @Override public void addRecord() { onReturnBike(); }
    @Override public void saveRecord() { onReturnBike(); }
    @Override public void clearForm() {
        scannedBike = null;
        activeRental = null;
        bikeIdField.clear();
        qrField.clear();
        customerField.clear();
        rentalIdField.clear();
        startField.clear();
        expectedField.clear();
        durationField.clear();
        lateDaysField.clear();
        lateFeeField.clear();
        baseField.clear();
        finalField.clear();
        notesArea.clear();
        actualPicker.setValue(LocalDate.now());
    }

    private void acceptScannedBike(Bike bike) {
        scannedBike = bike;
        bikeIdField.setText(bike.displayId());
        qrField.setText(bike.getQrCode());
        customerField.clear();
        rentalIdField.clear();
        startField.clear();
        expectedField.clear();
        durationField.clear();
        lateDaysField.clear();
        lateFeeField.clear();
        baseField.clear();
        finalField.clear();
        activeRental = rentalService.findActiveRental(bike.getId());
        if (activeRental == null) {
            customerField.setText("No active rental");
            AlertUtil.warn("No active rental", "This QR is valid, but there is no active rental to return.");
            return;
        }
        customerField.setText(activeRental.getCustomerName());
        rentalIdField.setText(value(activeRental.getId()));
        startField.setText(value(activeRental.getStartDate()));
        expectedField.setText(value(activeRental.getExpectedReturnDate()));
        AlertUtil.info("QR code successfully verified.",
                "Active rental #" + activeRental.getId() + " loaded for " + activeRental.getCustomerName() + ".");
        onCalculate();
    }

    private void reload() {
        UiAsync.run(rentalService::findReturns, rows ->
                table.setItems(FXCollections.observableArrayList(rows)));
    }

    private String value(Object object) {
        return object == null ? "" : String.valueOf(object);
    }

    private void showError(RuntimeException ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            AlertUtil.warn("Cannot complete return", ex.getMessage());
        } else {
            AlertUtil.error("Return failed", ex.getMessage());
        }
    }
}
