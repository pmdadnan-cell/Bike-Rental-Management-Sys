package com.bikevault.util;

import com.bikevault.app.AppConstants;
import com.bikevault.model.Bike;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Builds marketplace bike cards for the customer browse and home screens.
 */
public final class BikeCardFactory {

    private BikeCardFactory() {
    }

    public static VBox create(Bike bike, Consumer<Bike> onDetails, Consumer<Bike> onRent) {
        VBox card = new VBox(0);
        card.getStyleClass().add("bike-card");
        card.setPrefWidth(260);
        card.setMaxWidth(280);

        StackPane image = BikeImageLoader.framed(bike, 260, 146);
        image.getStyleClass().add("bike-card-image");

        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 14, 14));

        Label brand = new Label(bike.getBrand() == null ? "Bike" : bike.getBrand());
        brand.getStyleClass().add("bike-card-brand");
        Label model = new Label(bike.getModel() == null ? "" : bike.getModel());
        model.getStyleClass().add("bike-card-model");
        Label meta = new Label((bike.getCategoryName() == null ? "Fleet" : bike.getCategoryName())
                + "  ·  " + (bike.getColor() == null ? "" : bike.getColor()));
        meta.getStyleClass().add("bike-card-meta");

        BigDecimal rate = bike.getDailyRate() == null ? BigDecimal.ZERO : bike.getDailyRate();
        Label price = new Label("₹ " + rate.toPlainString() + " / day");
        price.getStyleClass().add("bike-card-price");

        Label badge = new Label("● " + (bike.getStatus() == null ? "" : bike.getStatus()));
        badge.getStyleClass().addAll("status-badge", statusClass(bike.getStatus()));

        boolean available = AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(bike.getStatus());
        Button details = new Button("VIEW DETAILS");
        details.getStyleClass().add("ghost-button");
        details.setMinWidth(108);
        details.setOnAction(event -> onDetails.accept(bike));
        Button rent = new Button("RENT NOW");
        rent.getStyleClass().add("primary-button");
        rent.setMinWidth(100);
        rent.setDisable(!available);
        rent.setOnAction(event -> onRent.accept(bike));

        HBox actions = new HBox(8, details, rent);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(details, Priority.ALWAYS);
        details.setMaxWidth(Double.MAX_VALUE);
        rent.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(brand, model, meta, price, badge, actions);
        card.getChildren().addAll(image, body);
        return card;
    }

    private static String statusClass(String status) {
        if (AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(status)) {
            return "badge-available";
        }
        if (AppConstants.BIKE_RENTED.equalsIgnoreCase(status)) {
            return "badge-rented";
        }
        if (AppConstants.BIKE_MAINTENANCE.equalsIgnoreCase(status)) {
            return "badge-maintenance";
        }
        return "badge-inactive";
    }
}
