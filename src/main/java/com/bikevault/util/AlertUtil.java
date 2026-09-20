package com.bikevault.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Centralized JavaFX dialogs. Controllers should not construct raw {@link Alert} instances.
 */
public final class AlertUtil {

    private AlertUtil() {
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void warn(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static boolean confirm(String title, String message) {
        return confirm(title, message, "OK");
    }

    public static boolean confirm(String title, String message, String confirmText) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, title, message);
        ButtonType confirm = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(cancel, confirm);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    public static Optional<String> promptReason(String title, String message, String confirmText) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(message);
        ButtonType confirm = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(cancel, confirm);
        TextArea area = new TextArea();
        area.setPromptText("Reason for rejection");
        area.setPrefRowCount(4);
        area.setWrapText(true);
        dialog.getDialogPane().setContent(area);
        Window owner = NavigationUtil.getPrimaryStage();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResultConverter(type -> type == confirm ? area.getText() : null);
        return dialog.showAndWait()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty());
    }

    private static void show(Alert.AlertType type, String title, String message) {
        create(type, title, message).showAndWait();
    }

    private static Alert create(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Window owner = NavigationUtil.getPrimaryStage();
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert;
    }
}
