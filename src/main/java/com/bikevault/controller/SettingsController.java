package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.SessionManager;
import com.bikevault.app.WorkspaceView;
import com.bikevault.database.DatabaseConfig;
import com.bikevault.database.DatabaseConnection;
import com.bikevault.service.AuthenticationService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.UiAsync;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Connection test, QR folder, and optional password change.
 */
public class SettingsController implements WorkspaceView {

    private final AuthenticationService authenticationService = new AuthenticationService();

    @FXML private ProgressIndicator busyIndicator;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField databaseField;
    @FXML private TextField userField;
    @FXML private Label connectionStatus;
    @FXML private TextField qrFolderField;
    @FXML private Label qrFolderStatus;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox confirmChangeCheck;
    @FXML private Label sessionNameLabel;
    @FXML private Label sessionRoleLabel;
    @FXML private Label sessionUserLabel;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        DatabaseConfig config = DatabaseConfig.load();
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        databaseField.setText(config.getDatabase());
        userField.setText(config.getUsername());

        Path qrFolder = Path.of(AppConstants.QR_DIRECTORY).toAbsolutePath();
        qrFolderField.setText(qrFolder.toString());
        qrFolderStatus.setText(Files.isDirectory(qrFolder)
                ? "Folder is ready for QR PNG files."
                : "Folder will be created the first time a QR image is generated.");

        sessionNameLabel.setText(SessionManager.displayName());
        sessionRoleLabel.setText(SessionManager.isAdmin() ? "Role: ADMIN" : "Role: STAFF");
        sessionUserLabel.setText(SessionManager.getCurrentUser() == null
                ? ""
                : "Username: " + SessionManager.getCurrentUser().getUsername());
    }

    @FXML
    private void onTestConnection() {
        setBusy(true);
        connectionStatus.setText("Testing MySQL…");
        UiAsync.run(
                () -> DatabaseConnection.getInstance().isReachable(),
                up -> {
                    setBusy(false);
                    if (Boolean.TRUE.equals(up)) {
                        connectionStatus.setText("Connected to " + hostField.getText() + ":" + portField.getText()
                                + "/" + databaseField.getText());
                        AlertUtil.info("Database connected", "MySQL accepted the configured credentials.");
                    } else {
                        connectionStatus.setText("MySQL is offline. Start Docker with docker compose up -d.");
                        AlertUtil.warn("Database offline", "Could not reach MySQL on port " + portField.getText() + ".");
                    }
                },
                error -> {
                    setBusy(false);
                    connectionStatus.setText("Connection test failed.");
                    AlertUtil.error("Database error", error.getMessage());
                }
        );
    }

    @FXML
    private void onOpenQrFolder() {
        try {
            Path folder = Path.of(AppConstants.QR_DIRECTORY).toAbsolutePath();
            Files.createDirectories(folder);
            qrFolderStatus.setText("Folder is ready for QR PNG files.");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
            } else {
                AlertUtil.info("QR folder", folder.toString());
            }
        } catch (Exception ex) {
            AlertUtil.error("Could not open folder", ex.getMessage());
        }
    }

    @FXML
    private void onChangePassword() {
        if (!confirmChangeCheck.isSelected()) {
            AlertUtil.warn("Confirm the change", "Select the confirmation checkbox before updating the password.");
            return;
        }
        String next = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();
        if (next == null || !next.equals(confirm)) {
            AlertUtil.warn("Passwords do not match", "Re-enter the new password in both fields.");
            return;
        }
        try {
            authenticationService.changeOwnPassword(currentPasswordField.getText(), next);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            confirmChangeCheck.setSelected(false);
            AlertUtil.info("Password updated", "Use the new password the next time you sign in.");
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                AlertUtil.warn("Cannot update password", ex.getMessage());
            } else {
                AlertUtil.error("Could not update password", ex.getMessage());
            }
        }
    }

    private void setBusy(boolean busy) {
        busyIndicator.setVisible(busy);
        busyIndicator.setManaged(busy);
    }
}
