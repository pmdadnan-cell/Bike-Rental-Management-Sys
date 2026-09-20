package com.bikevault.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

/**
 * Colored status text for fleet, rental, and payment tables.
 */
public final class StatusCells {

    private StatusCells() {
    }

    public static <S> void apply(TableColumn<S, String> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "status-available", "status-rented", "status-maintenance", "status-inactive",
                        "status-active", "status-completed", "status-cancelled",
                        "status-pending", "status-failed", "status-refunded");
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add(styleFor(item));
            }
        });
    }

    public static String styleFor(String status) {
        if (status == null) {
            return "status-inactive";
        }
        return switch (status.toUpperCase()) {
            case "AVAILABLE", "ACTIVE" -> "status-available";
            case "RENTED", "PENDING" -> "status-rented";
            case "MAINTENANCE", "CANCELLED", "FAILED" -> "status-maintenance";
            case "COMPLETED" -> "status-completed";
            case "REFUNDED", "INACTIVE" -> "status-inactive";
            default -> "status-inactive";
        };
    }
}
