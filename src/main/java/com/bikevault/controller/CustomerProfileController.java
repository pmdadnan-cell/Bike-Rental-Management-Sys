package com.bikevault.controller;

import com.bikevault.model.Customer;
import com.bikevault.service.CustomerService;
import com.bikevault.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class CustomerProfileController {

    private final CustomerService customerService = new CustomerService();
    private Customer current;

    @FXML private Label idLabel;
    @FXML private Label statusLabel;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField licenseField;
    @FXML private DatePicker dobPicker;

    @FXML
    private void initialize() {
        current = customerService.findOwn();
        idLabel.setText("Customer #" + current.getId());
        statusLabel.setText("Account status: " + current.getStatus() + " (read-only)");
        nameField.setText(current.getFullName());
        emailField.setText(current.getEmail());
        phoneField.setText(current.getPhone());
        addressField.setText(current.getAddress());
        licenseField.setText(current.getDrivingLicenseNumber());
        dobPicker.setValue(current.getDateOfBirth());
    }

    @FXML
    private void onSave() {
        try {
            Customer patch = new Customer();
            patch.setFullName(nameField.getText());
            patch.setEmail(emailField.getText());
            patch.setPhone(phoneField.getText());
            patch.setAddress(addressField.getText());
            patch.setDrivingLicenseNumber(licenseField.getText());
            patch.setDateOfBirth(dobPicker.getValue());
            customerService.updateOwnProfile(patch);
            AlertUtil.info("Profile updated", "Your permitted details were saved.");
        } catch (RuntimeException ex) {
            AlertUtil.error("Could not update profile", ex.getMessage());
        }
    }
}
