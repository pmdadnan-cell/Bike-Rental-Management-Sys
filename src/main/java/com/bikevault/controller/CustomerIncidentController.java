package com.bikevault.controller;

import com.bikevault.model.Incident;
import com.bikevault.service.IncidentService;
import com.bikevault.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class CustomerIncidentController {

    private final IncidentService incidentService = new IncidentService();

    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField locationField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea detailsArea;
    @FXML private FlowPane incidentPane;

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll("ACCIDENT", "BIKE_DAMAGE", "MECHANICAL_ISSUE", "THEFT", "TRAFFIC_CHALLAN", "OTHER");
        typeCombo.getSelectionModel().select("ACCIDENT");
        datePicker.setValue(LocalDate.now());
        reloadCards();
    }

    @FXML
    private void onSubmit() {
        try {
            Incident incident = new Incident();
            incident.setIncidentType(typeCombo.getValue());
            incident.setIncidentDate(datePicker.getValue());
            incident.setLocation(locationField.getText());
            incident.setDescription(descriptionArea.getText());
            incident.setDetails(detailsArea.getText());
            incidentService.report(incident);
            AlertUtil.info("Incident reported", "Status is REPORTED. Admin will review it.");
            descriptionArea.clear();
            detailsArea.clear();
            locationField.clear();
            reloadCards();
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not report incident", ex.getMessage());
        }
    }

    private void reloadCards() {
        List<Incident> rows = incidentService.findMine();
        incidentPane.getChildren().clear();
        if (rows.isEmpty()) {
            Label empty = new Label("No incidents submitted.");
            empty.getStyleClass().add("customer-tagline");
            incidentPane.getChildren().add(empty);
            return;
        }
        for (Incident incident : rows) {
            VBox card = new VBox(6);
            card.getStyleClass().add("trip-card");
            card.setPrefWidth(260);
            Label title = new Label("INCIDENT #INC-" + String.format("%03d", incident.getId()));
            title.getStyleClass().add("customer-kicker");
            Label type = new Label(incident.getIncidentType());
            type.getStyleClass().add("bike-card-model");
            Label date = new Label(String.valueOf(incident.getIncidentDate()));
            Label status = new Label("Status: " + incident.getStatus());
            card.getChildren().addAll(title, type, date, status,
                    new Label(incident.getLocation() == null ? "" : incident.getLocation()));
            incidentPane.getChildren().add(card);
        }
    }
}
