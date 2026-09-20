package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.WorkspaceView;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.model.AuditLog;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AuditController implements WorkspaceView {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private List<AuditLog> cache = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TableView<AuditLog> table;
    @FXML private TableColumn<AuditLog, String> timeColumn;
    @FXML private TableColumn<AuditLog, String> userColumn;
    @FXML private TableColumn<AuditLog, String> actionColumn;
    @FXML private TableColumn<AuditLog, String> detailColumn;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        filterCombo.getItems().setAll("All", "Authentication", "Rentals", "Payments", "Bikes", "Admin Actions");
        filterCombo.getSelectionModel().select("All");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTimestamp())));
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        actionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("action-badge", badgeClass(item));
                setGraphic(badge);
                setText(null);
            }
        });
        detailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        table.setPlaceholder(new Label("No audit activity recorded"));
        reload();
    }

    private String badgeClass(String action) {
        String value = action.toUpperCase(Locale.ROOT);
        if (value.contains("LOGIN") || value.contains("AUTH")) {
            return "badge-info";
        }
        if (value.contains("APPROV") || value.contains("PAYMENT") || value.contains("COMPLETED") || value.contains("PAID")) {
            return "badge-success";
        }
        if (value.contains("PENALTY") || value.contains("CHALLAN") || value.contains("REJECT") || value.contains("WARN")) {
            return "badge-warning";
        }
        if (value.contains("DELETE") || value.contains("FAIL") || value.contains("ERROR")) {
            return "badge-danger";
        }
        if (value.contains("LOGOUT")) {
            return "badge-muted";
        }
        return "badge-muted";
    }

    @FXML
    private void onSearch() {
        String needle = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        String filter = filterCombo.getValue() == null ? "All" : filterCombo.getValue();
        table.setItems(FXCollections.observableArrayList(cache.stream()
                .filter(log -> matchesFilter(log, filter))
                .filter(log -> needle.isBlank()
                        || String.valueOf(log.getAction()).toLowerCase(Locale.ROOT).contains(needle)
                        || String.valueOf(log.getDescription()).toLowerCase(Locale.ROOT).contains(needle)
                        || String.valueOf(log.getUsername()).toLowerCase(Locale.ROOT).contains(needle))
                .collect(Collectors.toList())));
    }

    private boolean matchesFilter(AuditLog log, String filter) {
        String action = log.getAction() == null ? "" : log.getAction().toUpperCase(Locale.ROOT);
        return switch (filter) {
            case "Authentication" -> action.contains("LOGIN") || action.contains("LOGOUT") || action.contains("REGISTER");
            case "Rentals" -> action.contains("RENTAL") || action.contains("RETURN") || action.contains("APPROV");
            case "Payments" -> action.contains("PAY");
            case "Bikes" -> action.contains("BIKE");
            case "Admin Actions" -> action.contains("DELETE") || action.contains("UPDATE") || action.contains("ADD")
                    || action.contains("REJECT") || action.contains("WAIVE");
            default -> true;
        };
    }

    @Override public void addRecord() { }
    @Override public void saveRecord() { }
    @Override public void clearForm() { searchField.clear(); onSearch(); }

    private void reload() {
        UiAsync.run(auditLogDAO::findAll, rows -> {
            cache = rows;
            onSearch();
        });
    }
}
