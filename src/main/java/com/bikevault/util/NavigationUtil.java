package com.bikevault.util;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.BookingContext;
import com.bikevault.app.ScanContext;
import com.bikevault.app.TripContext;
import com.bikevault.app.SessionManager;
import com.bikevault.app.WorkspaceView;
import com.bikevault.controller.CustomerShellController;
import com.bikevault.controller.DashboardController;
import com.bikevault.exception.UnauthorizedException;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Single-window navigation: login, operator dashboard, customer shell, and in-shell content swapping.
 */
public final class NavigationUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationUtil.class);

    private static Stage primaryStage;
    private static StackPane contentHost;
    private static DashboardController dashboardController;
    private static CustomerShellController customerShellController;
    private static WorkspaceView currentWorkspace;
    private static boolean customerShell;

    private NavigationUtil() {
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle(AppConstants.APP_WINDOW_TITLE);
        primaryStage.setOnCloseRequest(event -> {
            if (!confirmExit()) {
                event.consume();
            }
        });
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static WorkspaceView getCurrentWorkspace() {
        return currentWorkspace;
    }

    public static void bindDashboard(DashboardController controller, StackPane host) {
        dashboardController = controller;
        customerShellController = null;
        contentHost = host;
        customerShell = false;
    }

    public static void bindCustomerShell(CustomerShellController controller, StackPane host) {
        customerShellController = controller;
        dashboardController = null;
        contentHost = host;
        customerShell = true;
    }

    public static void showLogin() {
        leaveCurrent();
        ScanContext.clear();
        BookingContext.clear();
        TripContext.clear();
        SessionManager.clear();
        contentHost = null;
        dashboardController = null;
        customerShellController = null;
        customerShell = false;
        Parent root = loadRequired(AppConstants.LOGIN_FXML);
        Scene scene = applyStyles(new Scene(root, AppConstants.LOGIN_WIDTH, AppConstants.LOGIN_HEIGHT), false);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showRegister() {
        Parent root = loadRequired(AppConstants.REGISTER_FXML);
        Scene scene = applyStyles(new Scene(root, AppConstants.LOGIN_WIDTH, AppConstants.LOGIN_HEIGHT), false);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showHomeAfterLogin() {
        if (SessionManager.isCustomer()) {
            showCustomerShell();
        } else {
            showDashboard();
        }
    }

    public static void showDashboard() {
        if (!SessionManager.isAuthenticated()) {
            showLogin();
            return;
        }
        if (SessionManager.isCustomer()) {
            showCustomerShell();
            return;
        }
        Parent root = loadRequired(AppConstants.DASHBOARD_FXML);
        Scene scene = applyStyles(new Scene(root, AppConstants.DASHBOARD_WIDTH, AppConstants.DASHBOARD_HEIGHT), false);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1180);
        primaryStage.setMinHeight(740);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showCustomerShell() {
        if (!SessionManager.isAuthenticated()) {
            showLogin();
            return;
        }
        if (!SessionManager.isCustomer()) {
            showDashboard();
            return;
        }
        Parent root = loadRequired(AppConstants.CUSTOMER_SHELL_FXML);
        Scene scene = applyStyles(new Scene(root, AppConstants.DASHBOARD_WIDTH, AppConstants.DASHBOARD_HEIGHT), true);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1180);
        primaryStage.setMinHeight(740);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showView(AppView view) {
        if (!canOpen(view)) {
            AlertUtil.warn("Access denied", "This screen is not available for the signed-in role.");
            return;
        }
        if (contentHost == null) {
            LOG.warn("Cannot navigate to {} because no shell is bound", view);
            return;
        }
        if (dashboardController != null) {
            dashboardController.highlightNavigation(view);
            dashboardController.updateSectionTitle(view.getTitle());
        }
        if (customerShellController != null) {
            customerShellController.highlightNavigation(view);
            customerShellController.updateSectionTitle(view.getTitle());
        }

        if (view == AppView.DASHBOARD && dashboardController != null) {
            leaveCurrent();
            dashboardController.showHome();
            return;
        }
        if (view == AppView.CUSTOMER_HOME && customerShellController != null) {
            leaveCurrent();
            customerShellController.showHome();
            return;
        }

        if (!view.isDelivered() || view.getFxmlPath() == null) {
            leaveCurrent();
            contentHost.getChildren().setAll(createComingSoon(view));
            return;
        }

        try {
            leaveCurrent();
            Parent module = loadRequired(view.getFxmlPath());
            contentHost.getChildren().setAll(module);
            UiMotion.fadeIn(module);
        } catch (UnauthorizedException ex) {
            contentHost.getChildren().setAll(createAccessDenied(ex.getMessage()));
        } catch (IllegalStateException ex) {
            LOG.info("View {} is not available yet, showing placeholder", view, ex);
            contentHost.getChildren().setAll(createComingSoon(view));
        }
    }

    public static boolean confirmExit() {
        return AlertUtil.confirm("Exit Application", "Close BIKEVAULT and end this session?");
    }

    public static void exitApplication() {
        if (confirmExit()) {
            LOG.info("Application exit requested");
            leaveCurrent();
            primaryStage.close();
        }
    }

    private static boolean canOpen(AppView view) {
        if (view == null || !SessionManager.isAuthenticated()) {
            return false;
        }
        if (view.getKind() == AppView.Kind.SHARED) {
            return true;
        }
        if (SessionManager.isCustomer()) {
            return view.isCustomerView() && view.getKind() != AppView.Kind.OPERATOR;
        }
        return SessionManager.isOperator() && view.isOperatorView() && view.getKind() != AppView.Kind.CUSTOMER;
    }

    private static void leaveCurrent() {
        if (currentWorkspace != null) {
            try {
                currentWorkspace.onLeave();
            } catch (RuntimeException ex) {
                LOG.warn("Workspace onLeave failed", ex);
            }
            currentWorkspace = null;
        }
    }

    private static Parent loadRequired(String resourcePath) {
        URL resource = NavigationUtil.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("FXML not found: " + resourcePath);
        }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof WorkspaceView workspaceView) {
                currentWorkspace = workspaceView;
            }
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load " + resourcePath, ex);
        }
    }

    private static Scene applyStyles(Scene scene, boolean customerTheme) {
        String css = Objects.requireNonNull(
                NavigationUtil.class.getResource(AppConstants.STYLESHEET),
                "style.css is missing").toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
        if (customerTheme) {
            String extra = Objects.requireNonNull(
                    NavigationUtil.class.getResource(AppConstants.CUSTOMER_STYLESHEET),
                    "customer.css is missing").toExternalForm();
            if (!scene.getStylesheets().contains(extra)) {
                scene.getStylesheets().add(extra);
            }
        }
        return scene;
    }

    private static Parent createComingSoon(AppView view) {
        Label kicker = new Label("MODULE SCHEDULED");
        kicker.getStyleClass().add("kicker-label");
        Label title = new Label(view.getTitle());
        title.getStyleClass().add("coming-soon-title");
        Label body = new Label("This workspace is part of the navigation framework.");
        body.getStyleClass().add("muted-label");
        VBox card = new VBox(12, kicker, title, body);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("coming-soon-card");
        card.setMaxWidth(640);
        StackPane wrapper = new StackPane(card);
        wrapper.getStyleClass().add("coming-soon-host");
        return wrapper;
    }

    private static Parent createAccessDenied(String message) {
        Label kicker = new Label("ACCESS DENIED");
        kicker.getStyleClass().add("kicker-label");
        Label title = new Label("Not authorized");
        title.getStyleClass().add("coming-soon-title");
        Label body = new Label(message);
        body.getStyleClass().add("muted-label");
        body.setWrapText(true);
        VBox card = new VBox(12, kicker, title, body);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("coming-soon-card");
        card.setMaxWidth(640);
        return new StackPane(card);
    }
}
