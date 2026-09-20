package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.model.CustomerHomeSnapshot;
import com.bikevault.service.DashboardService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BikeCardFactory;
import com.bikevault.util.BrandAssets;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.UiAsync;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Premium customer marketplace shell.
 */
public class CustomerShellController {

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("EEE dd MMM yyyy  HH:mm");

    @FXML private StackPane contentHost;
    @FXML private VBox homeView;
    @FXML private Label sectionTitleLabel;
    @FXML private Label userNameLabel;
    @FXML private Label clockLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label availableValue;
    @FXML private Label outstandingValue;
    @FXML private Label penaltyValue;
    @FXML private Label activeRentalLabel;
    @FXML private Button returnBikeButton;
    @FXML private FlowPane featuredPane;
    @FXML private ImageView brandMarkView;
    @FXML private Button navHome;
    @FXML private Button navBrowse;
    @FXML private Button navTrips;
    @FXML private Button navIncident;
    @FXML private Button navProfile;
    @FXML private Button navQr;
    @FXML private Button navPenalties;
    @FXML private Button navLogout;

    private final DashboardService dashboardService = new DashboardService();
    private Map<AppView, Button> navButtons;
    private Timeline clock;

    @FXML
    private void initialize() {
        if (!SessionManager.isCustomer()) {
            NavigationUtil.showDashboard();
            return;
        }
        NavigationUtil.bindCustomerShell(this, contentHost);
        navButtons = new HashMap<>();
        navButtons.put(AppView.CUSTOMER_HOME, navHome);
        navButtons.put(AppView.BROWSE_BIKES, navBrowse);
        navButtons.put(AppView.MY_TRIPS, navTrips);
        navButtons.put(AppView.REPORT_INCIDENT, navIncident);
        navButtons.put(AppView.MY_PROFILE, navProfile);
        navButtons.put(AppView.CUSTOMER_QR, navQr);
        navButtons.put(AppView.QR_SCANNER, navQr);
        navButtons.put(AppView.MY_PENALTIES, navPenalties);
        brandMarkView.setImage(BrandAssets.mark());
        userNameLabel.setText(SessionManager.displayName());
        startClock();
        showHome();
    }

    public void showHome() {
        if (!contentHost.getChildren().contains(homeView)) {
            contentHost.getChildren().setAll(homeView);
        }
        sectionTitleLabel.setText("Home");
        highlightNavigation(AppView.CUSTOMER_HOME);
        long customerId = SessionManager.requireCustomerId();
        UiAsync.run(() -> dashboardService.loadCustomerHome(customerId), this::applyHome);
    }

    public void highlightNavigation(AppView view) {
        AppView mapped = switch (view) {
            case MY_RENTALS, MY_PAYMENTS, CUSTOMER_TRIP, CUSTOMER_PASS, CUSTOMER_PAY, CUSTOMER_RETURN ->
                    AppView.MY_TRIPS;
            case CUSTOMER_PENALTY_PAY -> AppView.MY_PENALTIES;
            case CUSTOMER_QR, QR_SCANNER -> AppView.CUSTOMER_QR;
            default -> view;
        };
        navButtons.forEach((key, button) -> {
            button.getStyleClass().remove("nav-active");
            if (key == mapped) {
                button.getStyleClass().add("nav-active");
            }
        });
    }

    public void updateSectionTitle(String title) {
        sectionTitleLabel.setText(title);
    }

    @FXML private void onNavHome() { NavigationUtil.showView(AppView.CUSTOMER_HOME); }
    @FXML private void onNavBrowse() { NavigationUtil.showView(AppView.BROWSE_BIKES); }
    @FXML private void onNavTrips() { NavigationUtil.showView(AppView.MY_TRIPS); }
    @FXML private void onNavIncident() { NavigationUtil.showView(AppView.REPORT_INCIDENT); }
    @FXML private void onNavProfile() { NavigationUtil.showView(AppView.MY_PROFILE); }
    @FXML private void onNavQr() {
        NavigationUtil.showView(AppView.CUSTOMER_QR);
    }
    @FXML private void onNavPenalties() { NavigationUtil.showView(AppView.MY_PENALTIES); }

    @FXML
    private void onBrowse() {
        NavigationUtil.showView(AppView.BROWSE_BIKES);
    }

    @FXML
    private void onReturnBike() {
        NavigationUtil.showView(AppView.CUSTOMER_RETURN);
    }

    @FXML
    private void onLogout() {
        if (AlertUtil.confirm("Logout", "Sign out of BIKEVAULT?")) {
            try {
                Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
                new AuditLogDAO().insert(userId, AppConstants.ACTION_LOGOUT, "Customer signed out");
            } catch (RuntimeException ignored) {
                // logout still proceeds
            }
            if (clock != null) {
                clock.stop();
            }
            NavigationUtil.showLogin();
        }
    }

    private void applyHome(CustomerHomeSnapshot snapshot) {
        welcomeLabel.setText("Welcome, " + snapshot.getCustomerName());
        availableValue.setText(String.valueOf(snapshot.getAvailableBikes()));
        outstandingValue.setText("₹ " + snapshot.getOutstandingAmount().toPlainString());
        penaltyValue.setText(String.valueOf(snapshot.getActivePenalties()));
        featuredPane.getChildren().clear();
        snapshot.getFeaturedBikes().forEach(bike -> featuredPane.getChildren().add(
                BikeCardFactory.create(bike,
                        selected -> showBikeDetails(selected),
                        selected -> startRent(selected))));
        if (snapshot.getCurrentRental() == null) {
            activeRentalLabel.setText("No active rental. Browse the fleet and book your next ride.");
            returnBikeButton.setDisable(true);
        } else {
            activeRentalLabel.setText("ACTIVE RENTAL  ·  " + snapshot.getCurrentRental().getBikeLabel()
                    + "  ·  return " + snapshot.getCurrentRental().getExpectedReturnDate()
                    + "  ·  ₹ " + snapshot.getCurrentRental().getFinalAmount());
            returnBikeButton.setDisable(false);
        }
    }

    private void showBikeDetails(com.bikevault.model.Bike bike) {
        com.bikevault.app.BookingContext.selectBike(bike);
        AlertUtil.info(bike.getBrand() + " " + bike.getModel(),
                "Category: " + bike.getCategoryName()
                        + "\nColor: " + bike.getColor()
                        + "\nDaily rate: ₹ " + bike.getDailyRate()
                        + "\nAvailability: " + bike.getStatus()
                        + "\n\n" + (bike.getDescription() == null ? "Ready for a premium rental." : bike.getDescription())
                        + "\n\nRental rules: helmet recommended, valid license required, late returns attract a category late fee.");
    }

    private void startRent(com.bikevault.model.Bike bike) {
        com.bikevault.app.BookingContext.selectBike(bike);
        NavigationUtil.showView(AppView.CUSTOMER_RENT);
    }

    private void startClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
        clock = new Timeline(new KeyFrame(Duration.seconds(30),
                event -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
}
