package com.bikevault.controller;

import com.bikevault.app.AppView;
import com.bikevault.app.BookingContext;
import com.bikevault.model.Rental;
import com.bikevault.service.RentalService;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.StatusCells;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CustomerRentalsController {

    private final RentalService rentalService = new RentalService();

    @FXML private TableView<Rental> table;
    @FXML private TableColumn<Rental, String> idColumn;
    @FXML private TableColumn<Rental, String> bikeColumn;
    @FXML private TableColumn<Rental, String> startColumn;
    @FXML private TableColumn<Rental, String> expectedColumn;
    @FXML private TableColumn<Rental, String> actualColumn;
    @FXML private TableColumn<Rental, String> daysColumn;
    @FXML private TableColumn<Rental, String> amountColumn;
    @FXML private TableColumn<Rental, String> penaltyColumn;
    @FXML private TableColumn<Rental, String> statusColumn;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        bikeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBikeLabel()));
        startColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStartDate())));
        expectedColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getExpectedReturnDate())));
        actualColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getActualReturnDate())));
        daysColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDurationDays())));
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty("₹ " + data.getValue().getFinalAmount()));
        penaltyColumn.setCellValueFactory(data -> new SimpleStringProperty("₹ " + data.getValue().getLateFee()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayStatus()));
        StatusCells.apply(statusColumn);
        UiAsync.run(rentalService::findMine, rows -> table.setItems(FXCollections.observableArrayList(rows)));
    }

    @FXML
    private void onPay() {
        Rental selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            BookingContext.setQuotedRental(selected);
            NavigationUtil.showView(AppView.CUSTOMER_PAY);
        }
    }
}
