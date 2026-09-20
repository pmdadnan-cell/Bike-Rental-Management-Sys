package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Incident;
import com.bikevault.service.IncidentService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.StatusCells;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;

public class IncidentController implements WorkspaceView {

    private final IncidentService incidentService = new IncidentService();
    private List<Incident> cache = new ArrayList<>();

    @FXML private TextArea detailsArea;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TableView<Incident> table;
    @FXML private TableColumn<Incident, String> idColumn;
    @FXML private TableColumn<Incident, String> customerColumn;
    @FXML private TableColumn<Incident, String> typeColumn;
    @FXML private TableColumn<Incident, String> dateColumn;
    @FXML private TableColumn<Incident, String> statusColumn;
    @FXML private TableColumn<Incident, String> descriptionColumn;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        statusCombo.getItems().setAll(AppConstants.STATUS_REPORTED, AppConstants.STATUS_UNDER_REVIEW,
                AppConstants.STATUS_RESOLVED, AppConstants.STATUS_REJECTED);
        idColumn.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        customerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIncidentType()));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getIncidentDate())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        descriptionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        StatusCells.apply(statusColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                detailsArea.setText(selected.getDescription() + "\n\n" + (selected.getDetails() == null ? "" : selected.getDetails()));
                statusCombo.setValue(selected.getStatus());
            }
        });
        reload();
    }

    @FXML
    private void onReview() {
        Incident selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            incidentService.review(selected, statusCombo.getValue());
            AlertUtil.info("Incident updated", "Review status saved.");
            reload();
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not review incident", ex.getMessage());
        }
    }

    @Override public void addRecord() { }
    @Override public void saveRecord() { onReview(); }
    @Override public void clearForm() { detailsArea.clear(); }

    private void reload() {
        UiAsync.run(incidentService::findAll, rows -> {
            cache = rows;
            table.setItems(FXCollections.observableArrayList(cache));
        });
    }
}
