package com.bikevault.util;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.BookingContext;
import com.bikevault.model.Bike;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Customer bike details: large motorcycle image plus live registry fields.
 */
public final class BikeDetailsDialog {

    private BikeDetailsDialog() {
    }

    public static void show(Bike bike) {
        if (bike == null) {
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(bike.getBrand() + " " + bike.getModel());

        VBox root = new VBox(14);
        root.getStyleClass().add("customer-workspace");
        root.setPadding(new Insets(20));
        root.setPrefWidth(520);

        root.getChildren().add(BikeImageLoader.framed(bike, 480, 270));

        Label brand = new Label(nullToDash(bike.getBrand()));
        brand.getStyleClass().add("bike-card-brand");
        Label model = new Label(nullToDash(bike.getModel()));
        model.getStyleClass().add("bike-card-model");
        Label meta = new Label(nullToDash(bike.getCategoryName()) + "  ·  " + nullToDash(bike.getColor()));
        meta.getStyleClass().add("bike-card-meta");
        Label price = new Label("₹ " + bike.getDailyRate() + " / day");
        price.getStyleClass().add("bike-card-price");
        Label status = new Label("● " + nullToDash(bike.getStatus()));
        status.getStyleClass().add("status-badge");
        Label ids = new Label("Bike ID: " + bike.displayId()
                + "\nRegistration: " + nullToDash(bike.getRegistrationNumber())
                + (bike.getDescription() == null || bike.getDescription().isBlank()
                ? "" : "\n" + bike.getDescription()));
        ids.getStyleClass().add("trip-data");
        ids.setWrapText(true);

        Button rent = new Button("RENT NOW");
        rent.getStyleClass().add("primary-button");
        rent.setDisable(!AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(bike.getStatus()));
        rent.setOnAction(event -> {
            BookingContext.selectBike(bike);
            stage.close();
            NavigationUtil.showView(AppView.CUSTOMER_RENT);
        });
        Button close = new Button("CLOSE");
        close.getStyleClass().add("ghost-button");
        close.setOnAction(event -> stage.close());
        HBox actions = new HBox(10, rent, close);
        actions.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(brand, model, meta, price, status, ids, actions);
        Scene scene = new Scene(root);
        if (NavigationUtil.getPrimaryStage() != null && NavigationUtil.getPrimaryStage().getScene() != null) {
            scene.getStylesheets().addAll(NavigationUtil.getPrimaryStage().getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
