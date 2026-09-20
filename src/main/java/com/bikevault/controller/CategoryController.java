package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.BikeCategory;
import com.bikevault.service.CategoryService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CategoryController implements WorkspaceView {

    private final CategoryService categoryService = new CategoryService();
    private List<BikeCategory> cache = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private TextField lateFeeField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea descriptionArea;
    @FXML private TableView<BikeCategory> table;
    @FXML private TableColumn<BikeCategory, String> idColumn;
    @FXML private TableColumn<BikeCategory, String> nameColumn;
    @FXML private TableColumn<BikeCategory, String> feeColumn;
    @FXML private TableColumn<BikeCategory, String> statusColumn;
    @FXML private TableColumn<BikeCategory, String> descriptionColumn;
    @FXML private Button deleteButton;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        statusCombo.getItems().setAll(AppConstants.STATUS_ACTIVE, AppConstants.STATUS_INACTIVE);
        statusCombo.getSelectionModel().select(AppConstants.STATUS_ACTIVE);
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getId())));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        feeColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getLateFeePerDay())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        descriptionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                bindForm(selected);
            }
        });
        table.setPlaceholder(new javafx.scene.control.Label("No categories found."));
        if (!SessionManager.isAdmin()) {
            deleteButton.setDisable(true);
        }
        reload();
    }

    @FXML private void onSearch() { applyFilter(); }
    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onAdd() {
        try {
            BikeCategory saved = categoryService.add(readForm(true));
            AlertUtil.info("Category saved", saved.getName() + " was added.");
            reload();
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML private void onSave() { persist(idField.getText() == null || idField.getText().isBlank()); }
    @FXML private void onUpdate() { persist(false); }

    @FXML
    private void onDelete() {
        BikeCategory selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Select a category", "Choose a row before deleting.");
            return;
        }
        if (!AccessGuard.requireAdmin("delete categories")) {
            return;
        }
        if (!AlertUtil.confirm("Delete category", "Delete " + selected.getName() + "? Bikes in this category cannot be deleted if still linked.")) {
            return;
        }
        try {
            categoryService.delete(selected.getId());
            AlertUtil.info("Category deleted", "The category was removed.");
            reload();
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML
    public void onClear() {
        clearForm();
    }

    @Override public void addRecord() { onAdd(); }
    @Override public void saveRecord() { onSave(); }
    @Override public void clearForm() {
        table.getSelectionModel().clearSelection();
        idField.clear();
        nameField.clear();
        lateFeeField.setText("0.00");
        descriptionArea.clear();
        statusCombo.getSelectionModel().select(AppConstants.STATUS_ACTIVE);
    }

    private void persist(boolean create) {
        try {
            if (create) {
                onAdd();
                return;
            }
            if (categoryService.update(readForm(false))) {
                AlertUtil.info("Category updated", "Changes were saved.");
                reload();
            }
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void reload() {
        UiAsync.run(categoryService::findAll, rows -> {
            cache = rows;
            applyFilter();
        });
    }

    private void applyFilter() {
        table.setItems(FXCollections.observableArrayList(
                categoryService.search(cache, searchField.getText())));
    }

    private void bindForm(BikeCategory category) {
        idField.setText(value(category.getId()));
        nameField.setText(category.getName());
        lateFeeField.setText(value(category.getLateFeePerDay()));
        descriptionArea.setText(category.getDescription());
        statusCombo.getSelectionModel().select(category.getStatus());
    }

    private BikeCategory readForm(boolean creating) {
        BikeCategory category = new BikeCategory();
        if (!creating && !idField.getText().isBlank()) {
            category.setId(Long.parseLong(idField.getText()));
        }
        category.setName(nameField.getText());
        category.setDescription(descriptionArea.getText());
        category.setStatus(statusCombo.getValue());
        try {
            category.setLateFeePerDay(new BigDecimal(lateFeeField.getText().isBlank() ? "0" : lateFeeField.getText().trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Late fee per day must be a number.");
        }
        return category;
    }

    private String value(Object object) {
        return object == null ? "" : String.valueOf(object);
    }

    private void showError(RuntimeException ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            AlertUtil.warn("Check the form", ex.getMessage());
        } else {
            AlertUtil.error("Could not save category", ex.getMessage());
        }
    }
}
