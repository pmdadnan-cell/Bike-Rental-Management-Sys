package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.SessionManager;
import com.bikevault.app.TripContext;
import com.bikevault.model.Rental;
import com.bikevault.service.CustomerService;
import com.bikevault.service.RentalService;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

public class CustomerTripsController {

    private final RentalService rentalService = new RentalService();
    private final CustomerService customerService = new CustomerService();

    @FXML private Label totalValue;
    @FXML private Label activeValue;
    @FXML private Label pendingValue;
    @FXML private Label spentValue;
    @FXML private FlowPane tripPane;

    @FXML
    private void initialize() {
        List<Rental> trips = rentalService.findMine();
        totalValue.setText(String.valueOf(trips.size()));
        activeValue.setText(String.valueOf(trips.stream()
                .filter(item -> AppConstants.STATUS_ACTIVE.equalsIgnoreCase(item.getStatus())).count()));
        pendingValue.setText(String.valueOf(trips.stream()
                .filter(item -> AppConstants.STATUS_PENDING_APPROVAL.equalsIgnoreCase(item.getStatus())).count()));
        BigDecimal spent = customerService.totalSpentTillDate(SessionManager.requireCustomerId());
        spentValue.setText("₹ " + (spent == null ? "0" : spent.toPlainString()));
        tripPane.getChildren().clear();
        if (trips.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER_LEFT);
            empty.getStyleClass().add("trip-card");
            Label title = new Label("No trips yet");
            title.getStyleClass().add("customer-title");
            Button browse = new Button("BROWSE BIKES");
            browse.getStyleClass().add("primary-button");
            browse.setOnAction(event -> NavigationUtil.showView(AppView.BROWSE_BIKES));
            empty.getChildren().addAll(title, new Label("Find a bike and request your first rental."), browse);
            tripPane.getChildren().add(empty);
            return;
        }
        trips.forEach(trip -> tripPane.getChildren().add(card(trip)));
    }

    private VBox card(Rental rental) {
        VBox box = new VBox(8);
        box.getStyleClass().add("trip-card");
        box.setPrefWidth(420);
        box.setMinWidth(400);
        Label kicker = new Label("MY TRIP  ·  " + rental.displayId());
        kicker.getStyleClass().add("customer-kicker");
        Label bike = new Label(rental.getBikeLabel() == null ? "Bike" : rental.getBikeLabel());
        bike.getStyleClass().add("bike-card-model");
        Label dates = new Label(rental.getStartDate() + " → " + rental.getExpectedReturnDate());
        dates.getStyleClass().add("trip-data");
        Label amount = new Label("₹ " + rental.getFinalAmount());
        amount.getStyleClass().add("bike-card-price");
        Label status = new Label("● " + rental.displayStatus());
        status.getStyleClass().add("status-badge");
        Label payment = new Label("Payment: "
                + ("PAID".equalsIgnoreCase(rental.getPaymentStatus()) ? "✓ PAID" : rental.getPaymentStatus()));
        payment.getStyleClass().add("trip-data");
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button view = action("VIEW TRIP", "secondary-button");
        view.setOnAction(event -> {
            TripContext.select(rental.getId());
            NavigationUtil.showView(AppView.CUSTOMER_TRIP);
        });
        Button qr = action("SHOW RENTAL QR", "secondary-button");
        qr.setOnAction(event -> {
            TripContext.select(rental.getId());
            NavigationUtil.showView(AppView.CUSTOMER_PASS);
        });
        actions.getChildren().addAll(view, qr);
        if (AppConstants.STATUS_ACTIVE.equalsIgnoreCase(rental.getStatus())) {
            Button ret = action("RETURN BIKE", "primary-button");
            ret.setOnAction(event -> {
                TripContext.select(rental.getId());
                NavigationUtil.showView(AppView.CUSTOMER_TRIP);
            });
            actions.getChildren().add(ret);
        } else if (AppConstants.STATUS_RETURN_REQUESTED.equalsIgnoreCase(rental.getStatus())) {
            Button show = action("SHOW RENTAL QR", "primary-button");
            show.setOnAction(event -> {
                TripContext.select(rental.getId());
                NavigationUtil.showView(AppView.CUSTOMER_PASS);
            });
            actions.getChildren().add(show);
        }
        HBox.setHgrow(view, Priority.NEVER);
        box.getChildren().addAll(kicker, bike, dates, amount, status, payment, actions);
        return box;
    }

    private Button action(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setMinWidth(132);
        button.setWrapText(false);
        button.setEllipsisString("");
        return button;
    }
}
