package com.bikevault.controller;

import com.bikevault.model.Payment;
import com.bikevault.service.PaymentService;
import com.bikevault.util.StatusCells;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CustomerPaymentsController {

    private final PaymentService paymentService = new PaymentService();

    @FXML private TableView<Payment> table;
    @FXML private TableColumn<Payment, String> idColumn;
    @FXML private TableColumn<Payment, String> rentalColumn;
    @FXML private TableColumn<Payment, String> dateColumn;
    @FXML private TableColumn<Payment, String> amountColumn;
    @FXML private TableColumn<Payment, String> methodColumn;
    @FXML private TableColumn<Payment, String> statusColumn;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        rentalColumn.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getRentalId()
                + "  " + data.getValue().getBikeLabel()));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getPaidAt())));
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty("₹ " + data.getValue().getAmount()));
        methodColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMethod()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayStatus()));
        StatusCells.apply(statusColumn);
        UiAsync.run(paymentService::findMine, rows -> table.setItems(FXCollections.observableArrayList(rows)));
    }
}
