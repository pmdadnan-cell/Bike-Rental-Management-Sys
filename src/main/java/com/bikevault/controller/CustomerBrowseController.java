package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.BookingContext;
import com.bikevault.app.ScanContext;
import com.bikevault.model.Bike;
import com.bikevault.service.BikeService;
import com.bikevault.service.CategoryService;
import com.bikevault.service.QRService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BikeCardFactory;
import com.bikevault.util.BikeDetailsDialog;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.UiAsync;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.math.BigDecimal;
import java.util.List;

public class CustomerBrowseController {

    private final BikeService bikeService = new BikeService();
    private final CategoryService categoryService = new CategoryService();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> availabilityCombo;
    @FXML private TextField colorField;
    @FXML private TextField minField;
    @FXML private TextField maxField;
    @FXML private FlowPane cardsPane;

    @FXML
    private void initialize() {
        categoryCombo.getItems().add("ALL");
        categoryService.findActive().forEach(category -> categoryCombo.getItems().add(category.getName()));
        categoryCombo.getSelectionModel().select("ALL");
        availabilityCombo.getItems().setAll("ALL", AppConstants.BIKE_AVAILABLE, AppConstants.BIKE_RENTED,
                AppConstants.BIKE_MAINTENANCE);
        availabilityCombo.getSelectionModel().select("ALL");
        searchField.textProperty().addListener((obs, old, value) -> refresh());
        categoryCombo.setOnAction(event -> refresh());
        availabilityCombo.setOnAction(event -> refresh());
        colorField.textProperty().addListener((obs, old, value) -> refresh());
        ScanContext.takePending().ifPresent(this::onRent);
        refresh();
    }

    @FXML
    private void refresh() {
        BigDecimal min = parseMoney(minField.getText());
        BigDecimal max = parseMoney(maxField.getText());
        UiAsync.run(() -> bikeService.catalog(searchField.getText(), categoryCombo.getValue(),
                        availabilityCombo.getValue(), colorField.getText(), min, max),
                this::render);
    }

    private void render(List<Bike> bikes) {
        cardsPane.getChildren().clear();
        bikes.forEach(bike -> cardsPane.getChildren().add(BikeCardFactory.create(bike, this::onDetails, this::onRent)));
    }

    private void onDetails(Bike bike) {
        BikeDetailsDialog.show(bike);
    }

    private void onRent(Bike bike) {
        if (AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(bike.getStatus())) {
            BookingContext.selectBike(bike);
            NavigationUtil.showView(AppView.CUSTOMER_RENT);
            return;
        }
        if (AppConstants.BIKE_RENTED.equalsIgnoreCase(bike.getStatus())) {
            NavigationUtil.showView(AppView.CUSTOMER_RETURN);
            return;
        }
        AlertUtil.warn("Not available", "This bike cannot be rented right now (" + bike.getStatus() + ").");
    }

    private BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
