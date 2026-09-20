package com.bikevault.controller;

import com.bikevault.model.Customer;
import com.bikevault.service.AuthenticationService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BrandAssets;
import com.bikevault.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.time.LocalDate;

/**
 * Customer self-registration. Creates a CUSTOMER user and linked customer profile.
 */
public class RegisterController {

    private final AuthenticationService authenticationService = new AuthenticationService();

    @FXML private ImageView logoView;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField licenseField;
    @FXML private TextField addressField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> genderCombo;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        logoView.setImage(BrandAssets.mark());
        genderCombo.getItems().setAll("MALE", "FEMALE", "OTHER");
        genderCombo.getSelectionModel().select("MALE");
        dobPicker.setValue(LocalDate.now().minusYears(21));
        statusLabel.setText("Create a customer account to browse and rent bikes.");
    }

    @FXML
    private void onRegister() {
        try {
            Customer profile = new Customer();
            profile.setFullName(nameField.getText());
            profile.setEmail(emailField.getText());
            profile.setPhone(phoneField.getText());
            profile.setDrivingLicenseNumber(licenseField.getText());
            profile.setAddress(addressField.getText());
            profile.setDateOfBirth(dobPicker.getValue());
            profile.setGender(genderCombo.getValue());
            authenticationService.registerCustomer(usernameField.getText(), passwordField.getText(), profile);
            AlertUtil.info("Welcome to BIKEVAULT", "Your account was created. Sign in to start browsing bikes.");
            NavigationUtil.showLogin();
        } catch (RuntimeException ex) {
            statusLabel.setText(ex.getMessage());
            AlertUtil.warn("Could not register", ex.getMessage());
        }
    }

    @FXML
    private void onBack() {
        NavigationUtil.showLogin();
    }
}
