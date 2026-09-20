package com.bikevault.controller;

import com.bikevault.app.SessionManager;
import com.bikevault.exception.DatabaseException;
import com.bikevault.model.User;
import com.bikevault.service.AuthenticationService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BrandAssets;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.ValidationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Login screen. Controllers stay thin: validation and alerts live here, authentication lives in the service.
 */
public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private static final String PREF_USERNAME = "rememberedUsername";
    private static final String PREF_REMEMBER = "rememberUsername";

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final Preferences preferences = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    private ImageView logoView;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberCheck;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    @FXML
    private void initialize() {
        logoView.setImage(BrandAssets.mark());
        boolean remember = preferences.getBoolean(PREF_REMEMBER, false);
        rememberCheck.setSelected(remember);
        if (remember) {
            usernameField.setText(preferences.get(PREF_USERNAME, ""));
            Platform.runLater(passwordField::requestFocus);
        } else {
            Platform.runLater(usernameField::requestFocus);
        }
        setStatus("Secure rental management");
    }

    @FXML
    private void onLogin() {
        String username = ValidationUtil.trimToEmpty(usernameField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (!ValidationUtil.hasText(username) || !ValidationUtil.hasText(password)) {
            setStatus("Username and password are required.");
            AlertUtil.warn("Missing credentials", "Enter both username and password to continue.");
            return;
        }

        loginButton.setDisable(true);
        try {
            Optional<User> user = authenticationService.authenticate(username, password);
            if (user.isEmpty()) {
                setStatus("Invalid username or password.");
                AlertUtil.error("Authentication failed", "The username or password is incorrect.");
                return;
            }
            persistUsernamePreference(username);
            LOG.info("User '{}' signed in with role {}", user.get().getUsername(), user.get().getRole());
            NavigationUtil.showHomeAfterLogin();
        } catch (DatabaseException ex) {
            LOG.error("Login could not reach the database", ex);
            setStatus("Database unavailable.");
            AlertUtil.error("Database error", ex.getMessage());
        } finally {
            loginButton.setDisable(false);
        }
    }

    @FXML
    private void onRegister() {
        NavigationUtil.showRegister();
    }

    @FXML
    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            onLogin();
        }
    }

    private void persistUsernamePreference(String username) {
        preferences.putBoolean(PREF_REMEMBER, rememberCheck.isSelected());
        if (rememberCheck.isSelected()) {
            preferences.put(PREF_USERNAME, username);
        } else {
            preferences.remove(PREF_USERNAME);
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
