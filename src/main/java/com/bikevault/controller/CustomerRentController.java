package com.bikevault.controller;

import com.bikevault.app.AppView;
import com.bikevault.app.BookingContext;
import com.bikevault.model.Bike;
import com.bikevault.model.Rental;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BikeImageLoader;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.time.LocalDate;

public class CustomerRentController {

    private final RentalService rentalService = new RentalService();

    @FXML private ImageView bikeImage;
    @FXML private Label bikeLabel;
    @FXML private Label categoryLabel;
    @FXML private Label rateLabel;
    @FXML private Label daysLabel;
    @FXML private Label rateLineLabel;
    @FXML private Label baseLabel;
    @FXML private Label depositLabel;
    @FXML private Label totalLabel;
    @FXML private Label summaryLabel;
    @FXML private DatePicker startPicker;
    @FXML private DatePicker returnPicker;

    @FXML
    private void initialize() {
        Bike bike = BookingContext.getSelectedBike();
        if (bike == null) {
            AlertUtil.warn("Select a bike", "Choose a bike from Browse first.");
            NavigationUtil.showView(AppView.BROWSE_BIKES);
            return;
        }
        bikeImage.setImage(BikeImageLoader.imageFor(bike));
        bikeImage.setPreserveRatio(true);
        bikeImage.setSmooth(true);
        bikeLabel.setText(bike.getBrand() + " " + bike.getModel());
        categoryLabel.setText("Model " + bike.getModel() + "  ·  Category " + bike.getCategoryName()
                + "  ·  " + bike.getColor());
        rateLabel.setText("Daily rate  ₹ " + bike.getDailyRate());
        startPicker.setValue(LocalDate.now());
        returnPicker.setValue(LocalDate.now().plusDays(2));
        startPicker.valueProperty().addListener((obs, o, n) -> quote());
        returnPicker.valueProperty().addListener((obs, o, n) -> quote());
        quote();
    }

    @FXML
    private void quote() {
        Bike bike = BookingContext.getSelectedBike();
        Rental draft = new Rental();
        draft.setStartDate(startPicker.getValue());
        draft.setExpectedReturnDate(returnPicker.getValue());
        try {
            rentalService.quote(draft, bike);
            BookingContext.setQuotedRental(draft);
            daysLabel.setText(draft.getDurationDays() + " days");
            rateLineLabel.setText("₹ " + draft.getDailyRate());
            baseLabel.setText("₹ " + draft.getTotalAmount());
            depositLabel.setText("₹ 0");
            totalLabel.setText("₹ " + draft.getFinalAmount());
            summaryLabel.setText("Admin must approve this request after payment. The bike stays available until then.");
        } catch (RuntimeException ex) {
            summaryLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onConfirm() {
        Bike bike = BookingContext.getSelectedBike();
        Rental draft = BookingContext.getQuotedRental();
        if (bike == null || draft == null) {
            quote();
            draft = BookingContext.getQuotedRental();
        }
        if (draft == null) {
            AlertUtil.warn("Dates required", "Choose valid rental dates.");
            return;
        }
        BookingContext.selectBike(bike);
        BookingContext.setQuotedRental(draft);
        NavigationUtil.showView(AppView.CUSTOMER_PAY);
    }

    @FXML
    private void onCancel() {
        BookingContext.clear();
        NavigationUtil.showView(AppView.BROWSE_BIKES);
    }
}
