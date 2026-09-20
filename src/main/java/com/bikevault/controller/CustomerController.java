package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Customer;
import com.bikevault.service.CustomerService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.StatusCells;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerController implements WorkspaceView {

    private final CustomerService customerService = new CustomerService();
    private List<Customer> cache = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField licenseField;
    @FXML private TextArea addressArea;
    @FXML private DatePicker dobPicker;
    @FXML private DatePicker registeredPicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private RadioButton otherRadio;
    @FXML private TableView<Customer> table;
    @FXML private TableColumn<Customer, String> idColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> licenseColumn;
    @FXML private TableColumn<Customer, String> statusColumn;
    @FXML private TableColumn<Customer, String> activeColumn;
    @FXML private TableColumn<Customer, String> totalColumn;
    @FXML private TableColumn<Customer, String> outstandingColumn;
    @FXML private Button deleteButton;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        statusCombo.getItems().setAll(AppConstants.STATUS_ACTIVE, AppConstants.STATUS_INACTIVE);
        statusCombo.getSelectionModel().select(AppConstants.STATUS_ACTIVE);
        registeredPicker.setValue(LocalDate.now());
        maleRadio.setSelected(true);

        idColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getId())));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        phoneColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        licenseColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDrivingLicenseNumber()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getActiveRentals())));
        totalColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalRentals())));
        outstandingColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getOutstandingAmount() == null ? "₹ 0.00" : "₹ " + data.getValue().getOutstandingAmount()));
        StatusCells.apply(statusColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                bindForm(selected);
            }
        });
        table.setPlaceholder(new javafx.scene.control.Label("No customers found."));
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
            Customer saved = customerService.add(readForm(true));
            AlertUtil.info("Customer saved", "Customer " + saved.getFullName() + " was added.");
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
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Select a customer", "Choose a row before deactivating.");
            return;
        }
        if (!AccessGuard.requireAdmin("deactivate customers")) {
            return;
        }
        if (!AlertUtil.confirm("Deactivate customer", "Deactivate " + selected.getFullName()
                + "? Historical rentals are kept.")) {
            return;
        }
        try {
            customerService.deactivate(selected.getId());
            AlertUtil.info("Customer deactivated", "The account can no longer sign in or rent.");
            reload();
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML
    private void onReactivate() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Select a customer", "Choose a row before reactivating.");
            return;
        }
        if (!AccessGuard.requireAdmin("reactivate customers")) {
            return;
        }
        try {
            customerService.reactivate(selected.getId());
            AlertUtil.info("Customer reactivated", selected.getFullName() + " is active again.");
            reload();
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
        emailField.clear();
        phoneField.clear();
        licenseField.clear();
        addressArea.clear();
        dobPicker.setValue(null);
        registeredPicker.setValue(LocalDate.now());
        statusCombo.getSelectionModel().select(AppConstants.STATUS_ACTIVE);
        maleRadio.setSelected(true);
    }

    private void persist(boolean create) {
        try {
            if (create) {
                onAdd();
                return;
            }
            boolean updated = customerService.update(readForm(false));
            if (updated) {
                AlertUtil.info("Customer updated", "Changes were saved.");
                reload();
            }
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void reload() {
        UiAsync.run(customerService::findAll, rows -> {
            cache = rows;
            applyFilter();
        });
    }

    private void applyFilter() {
        table.setItems(FXCollections.observableArrayList(
                customerService.search(cache, searchField.getText())));
    }

    private void bindForm(Customer customer) {
        idField.setText(value(customer.getId()));
        nameField.setText(customer.getFullName());
        emailField.setText(customer.getEmail());
        phoneField.setText(customer.getPhone());
        licenseField.setText(customer.getDrivingLicenseNumber());
        addressArea.setText(customer.getAddress());
        dobPicker.setValue(customer.getDateOfBirth());
        registeredPicker.setValue(customer.getRegistrationDate());
        statusCombo.getSelectionModel().select(customer.getStatus());
        maleRadio.setSelected("MALE".equalsIgnoreCase(customer.getGender()));
        femaleRadio.setSelected("FEMALE".equalsIgnoreCase(customer.getGender()));
        otherRadio.setSelected("OTHER".equalsIgnoreCase(customer.getGender()));
    }

    private Customer readForm(boolean creating) {
        Customer customer = new Customer();
        if (!creating && !idField.getText().isBlank()) {
            customer.setId(Long.parseLong(idField.getText()));
        }
        customer.setFullName(nameField.getText());
        customer.setEmail(emailField.getText());
        customer.setPhone(phoneField.getText());
        customer.setDrivingLicenseNumber(licenseField.getText());
        customer.setAddress(addressArea.getText());
        customer.setDateOfBirth(dobPicker.getValue());
        customer.setRegistrationDate(registeredPicker.getValue());
        customer.setStatus(statusCombo.getValue());
        if (femaleRadio.isSelected()) {
            customer.setGender("FEMALE");
        } else if (otherRadio.isSelected()) {
            customer.setGender("OTHER");
        } else {
            customer.setGender("MALE");
        }
        return customer;
    }

    private String value(Object object) {
        return object == null ? "" : String.valueOf(object);
    }

    private void showError(RuntimeException ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            AlertUtil.warn("Check the form", ex.getMessage());
        } else {
            AlertUtil.error("Could not save customer", ex.getMessage());
        }
    }
}
