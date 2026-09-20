package com.bikevault.controller;

import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.ReportContext;
import com.bikevault.app.ScanContext;
import com.bikevault.app.SessionManager;
import com.bikevault.dao.AuditLogDAO;
import com.bikevault.database.DatabaseConnection;
import com.bikevault.model.AuditLog;
import com.bikevault.model.DashboardSnapshot;
import com.bikevault.model.Rental;
import com.bikevault.service.DashboardService;
import com.bikevault.service.RentalService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BrandAssets;
import com.bikevault.util.IconFactory;
import com.bikevault.util.NavigationUtil;
import com.bikevault.util.UiAsync;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Shell controller for the authenticated workspace: menu bar, sidebar, dashboard home, and content host.
 */
public class DashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("EEE dd MMM yyyy  HH:mm:ss");
    private static final DateTimeFormatter AUDIT_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss");

    @FXML private StackPane contentHost;
    @FXML private VBox homeView;
    @FXML private Label sectionTitleLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label clockLabel;
    @FXML private Label statusLabel;
    @FXML private Label dbStatusLabel;
    @FXML private ComboBox<String> periodCombo;
    @FXML private DatePicker asOfDatePicker;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator busyIndicator;

    @FXML private ImageView brandMarkView;
    @FXML private ImageView totalBikesIcon;
    @FXML private ImageView availableIcon;
    @FXML private ImageView rentedIcon;
    @FXML private ImageView maintenanceIcon;
    @FXML private ImageView customersIcon;
    @FXML private ImageView activeRentalsIcon;
    @FXML private ImageView revenueIcon;
    @FXML private ImageView totalRevenueIcon;
    @FXML private ImageView pendingPayIcon;
    @FXML private ImageView pendingReturnIcon;
    @FXML private ImageView penaltyIcon;
    @FXML private ImageView incidentIcon;

    @FXML private Label totalBikesValue;
    @FXML private Label availableValue;
    @FXML private Label rentedValue;
    @FXML private Label maintenanceValue;
    @FXML private Label customersValue;
    @FXML private Label activeRentalsValue;
    @FXML private Label revenueValue;
    @FXML private Label revenueTitleLabel;
    @FXML private Label pendingApprovalsValue;
    @FXML private Label pendingPayValue;
    @FXML private Label pendingReturnValue;
    @FXML private Label penaltyValue;
    @FXML private Label incidentValue;
    @FXML private Label pendingEmptyLabel;
    @FXML private Label availabilityLabel;
    @FXML private Label revenueTrendLabel;
    @FXML private Label systemStatusLabel;
    @FXML private Label adminAvatarLabel;
    @FXML private Label fleetAvailableLabel;
    @FXML private Label fleetRentedLabel;
    @FXML private Label fleetMaintLabel;
    @FXML private TextField headerSearchField;
    @FXML private FlowPane pendingApprovalPane;

    @FXML private TableView<AuditLog> recentTable;
    @FXML private TableColumn<AuditLog, String> timeColumn;
    @FXML private TableColumn<AuditLog, String> userColumn;
    @FXML private TableColumn<AuditLog, String> actionColumn;
    @FXML private TableColumn<AuditLog, String> detailColumn;

    @FXML private Button navDashboard;
    @FXML private Button navApproval;
    @FXML private Button navCustomers;
    @FXML private Button navBikes;
    @FXML private Button navRentals;
    @FXML private Button navPayments;
    @FXML private Button navPenalties;
    @FXML private Button navReports;
    @FXML private Button navAudit;
    @FXML private Button navSettings;
    @FXML private Button headerLogoutButton;
    @FXML private ImageView headerBellView;

    private final DashboardService dashboardService = new DashboardService();
    private final RentalService rentalService = new RentalService();
    private Timeline clock;
    private Map<AppView, Button> navButtons;
    private boolean loading;

    @FXML
    private void initialize() {
        if (SessionManager.isCustomer()) {
            NavigationUtil.showCustomerShell();
            return;
        }
        NavigationUtil.bindDashboard(this, contentHost);
        navButtons = new HashMap<>();
        navButtons.put(AppView.DASHBOARD, navDashboard);
        navButtons.put(AppView.APPROVAL_CENTER, navApproval);
        navButtons.put(AppView.CUSTOMERS, navCustomers);
        navButtons.put(AppView.BIKES, navBikes);
        navButtons.put(AppView.RENTALS, navRentals);
        navButtons.put(AppView.PAYMENTS, navPayments);
        navButtons.put(AppView.PENALTIES, navPenalties);
        navButtons.put(AppView.REPORTS, navReports);
        navButtons.put(AppView.AUDIT_LOGS, navAudit);
        navButtons.put(AppView.SETTINGS, navSettings);

        decorateNav(navDashboard, IconFactory.Kind.DASHBOARD);
        decorateNav(navApproval, IconFactory.Kind.APPROVALS);
        decorateNav(navCustomers, IconFactory.Kind.CUSTOMERS);
        decorateNav(navBikes, IconFactory.Kind.BIKES);
        decorateNav(navRentals, IconFactory.Kind.RENTALS);
        decorateNav(navPayments, IconFactory.Kind.PAYMENTS);
        decorateNav(navPenalties, IconFactory.Kind.PENALTIES);
        decorateNav(navReports, IconFactory.Kind.REPORTS);
        decorateNav(navAudit, IconFactory.Kind.AUDIT);
        decorateNav(navSettings, IconFactory.Kind.SETTINGS);

        brandMarkView.setImage(BrandAssets.mark());
        headerBellView.setImage(IconFactory.glyph(IconFactory.Kind.BELL, "#9AA6B2", 18));
        ImageView logoutIcon = new ImageView(IconFactory.glyph(IconFactory.Kind.LOGOUT, "#FF8A8A", 16));
        logoutIcon.setFitWidth(16);
        logoutIcon.setFitHeight(16);
        headerLogoutButton.setGraphic(logoutIcon);
        totalBikesIcon.setImage(IconFactory.metric(IconFactory.Kind.FLEET, "#5B8DEF", 42));
        availableIcon.setImage(IconFactory.metric(IconFactory.Kind.AVAILABLE, "#3DDC97", 42));
        rentedIcon.setImage(IconFactory.metric(IconFactory.Kind.ACTIVE, "#F0B429", 42));
        maintenanceIcon.setImage(IconFactory.metric(IconFactory.Kind.MAINTENANCE, "#E85D5D", 42));
        customersIcon.setImage(IconFactory.metric(IconFactory.Kind.CUSTOMERS, "#7C6CFF", 42));
        activeRentalsIcon.setImage(IconFactory.metric(IconFactory.Kind.ACTIVE, "#4CC9F0", 42));
        revenueIcon.setImage(IconFactory.metric(IconFactory.Kind.REVENUE, "#D4A017", 42));
        totalRevenueIcon.setImage(IconFactory.metric(IconFactory.Kind.PENDING, "#D4A017", 42));
        pendingPayIcon.setImage(IconFactory.metric(IconFactory.Kind.PAYMENTS, "#F0B429", 42));
        pendingReturnIcon.setImage(IconFactory.metric(IconFactory.Kind.RENTALS, "#E85D5D", 42));
        penaltyIcon.setImage(IconFactory.metric(IconFactory.Kind.PENALTIES, "#E85D5D", 42));
        incidentIcon.setImage(IconFactory.metric(IconFactory.Kind.AVAILABLE, "#3DDC97", 42));

        userNameLabel.setText(SessionManager.displayName());
        userRoleLabel.setText(SessionManager.isAdmin() ? "ADMIN" : SessionManager.getCurrentUser().getRole());
        String name = SessionManager.displayName();
        adminAvatarLabel.setText(name == null || name.isBlank() ? "A" : name.substring(0, 1).toUpperCase());

        periodCombo.getItems().setAll("Today", "This Week", "This Month", "All Time");
        periodCombo.getSelectionModel().select("Today");
        asOfDatePicker.setValue(LocalDate.now());

        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(formatTime(data.getValue())));
        userColumn.setCellValueFactory(data -> new SimpleStringProperty(formatUser(data.getValue())));
        actionColumn.setCellValueFactory(data -> new SimpleStringProperty(blankToDash(data.getValue().getAction())));
        detailColumn.setCellValueFactory(data -> new SimpleStringProperty(blankToDash(data.getValue().getDescription())));
        recentTable.setPlaceholder(new Label("No activity yet."));
        timeColumn.setSortable(true);
        userColumn.setSortable(true);
        actionColumn.setSortable(true);
        detailColumn.setSortable(true);

        startClock();
        highlightNavigation(AppView.DASHBOARD);
        showHome();
        pingDatabaseAsync();
        LOG.info("Dashboard initialized for {}", SessionManager.displayName());
    }

    public void showHome() {
        if (!contentHost.getChildren().contains(homeView)) {
            contentHost.getChildren().setAll(homeView);
        }
        homeView.setVisible(true);
        sectionTitleLabel.setText("Dashboard");
        highlightNavigation(AppView.DASHBOARD);
        loadSnapshot();
    }

    public void highlightNavigation(AppView view) {
        navButtons.forEach((key, button) -> setNavActive(button, key == view));
    }

    public void updateSectionTitle(String title) {
        sectionTitleLabel.setText(title);
    }

    @FXML private void onNavDashboard() { NavigationUtil.showView(AppView.DASHBOARD); }
    @FXML private void onNavApproval() { NavigationUtil.showView(AppView.APPROVAL_CENTER); }
    @FXML private void onNavCustomers() { NavigationUtil.showView(AppView.CUSTOMERS); }
    @FXML private void onNavBikes() { NavigationUtil.showView(AppView.BIKES); }
    @FXML private void onNavRentals() { NavigationUtil.showView(AppView.RENTALS); }
    @FXML private void onNavReturns() { NavigationUtil.showView(AppView.RETURNS); }
    @FXML private void onNavPayments() { NavigationUtil.showView(AppView.PAYMENTS); }
    @FXML private void onNavQrScanner() {
        ScanContext.clear();
        NavigationUtil.showView(AppView.QR_SCANNER);
    }
    @FXML private void onNavCategories() { NavigationUtil.showView(AppView.CATEGORIES); }
    @FXML private void onNavPenalties() { NavigationUtil.showView(AppView.PENALTIES); }
    @FXML private void onNavAudit() { NavigationUtil.showView(AppView.AUDIT_LOGS); }
    @FXML private void onNavReports() { NavigationUtil.showView(AppView.REPORTS); }
    @FXML private void onNavSettings() { NavigationUtil.showView(AppView.SETTINGS); }

    @FXML
    private void onLogout() {
        if (AlertUtil.confirm("Logout", "Sign out of BikeVault?", "Logout")) {
            try {
                Long userId = SessionManager.getCurrentUser() == null ? null : SessionManager.getCurrentUser().getId();
                new AuditLogDAO().insert(userId, AppConstants.ACTION_LOGOUT, "User signed out");
            } catch (RuntimeException ex) {
                LOG.warn("Logout audit could not be written", ex);
            }
            stopClock();
            NavigationUtil.showLogin();
        }
    }

    @FXML private void onFileNew() {
        if (NavigationUtil.getCurrentWorkspace() != null) {
            NavigationUtil.getCurrentWorkspace().addRecord();
        } else {
            AlertUtil.info("New", "Open a data module from Master or Transactions first.");
        }
    }

    @FXML private void onFileSave() {
        if (NavigationUtil.getCurrentWorkspace() != null) {
            NavigationUtil.getCurrentWorkspace().saveRecord();
        } else {
            AlertUtil.info("Save", "Open a data module from Master or Transactions first.");
        }
    }

    @FXML private void onFileClear() {
        if (NavigationUtil.getCurrentWorkspace() != null) {
            NavigationUtil.getCurrentWorkspace().clearForm();
        } else {
            AlertUtil.info("Clear", "Open a data module from Master or Transactions first.");
        }
    }

    @FXML private void onMasterCustomers() { NavigationUtil.showView(AppView.CUSTOMERS); }
    @FXML private void onMasterBikes() { NavigationUtil.showView(AppView.BIKES); }
    @FXML private void onMasterCategories() { NavigationUtil.showView(AppView.CATEGORIES); }
    @FXML private void onTxnRental() { NavigationUtil.showView(AppView.RENTALS); }
    @FXML private void onTxnReturn() { NavigationUtil.showView(AppView.RETURNS); }
    @FXML private void onTxnPayment() { NavigationUtil.showView(AppView.PAYMENTS); }
    @FXML private void onTxnScanQr() {
        ScanContext.clear();
        NavigationUtil.showView(AppView.QR_SCANNER);
    }
    @FXML private void onTxnScanRentalQr() {
        ScanContext.requestRentalScan(AppView.APPROVAL_CENTER);
    }
    @FXML private void onReportRentals() { ReportContext.open(ReportContext.Focus.RENTALS); }
    @FXML private void onReportAvailability() { ReportContext.open(ReportContext.Focus.AVAILABILITY); }
    @FXML private void onReportCustomers() { ReportContext.open(ReportContext.Focus.CUSTOMERS); }

    @FXML
    private void onHelpAbout() {
        AlertUtil.info("About BIKEVAULT",
                AppConstants.APP_NAME + "\n" + AppConstants.APP_TAGLINE
                        + "\n\nDesktop JavaFX application connected to MySQL on localhost:3307."
                        + "\nThere is no website and no http://localhost:8080."
                        + "\n\nLive dashboard, reports, QR identity, rentals, returns, and payments.");
    }

    @FXML
    private void onHelpSystemInfo() {
        String javaVersion = System.getProperty("java.version");
        String fxVersion = System.getProperty("javafx.version", "bundled");
        AlertUtil.info("System Information",
                "Java " + javaVersion + "\nJavaFX " + fxVersion
                        + "\nUser: " + SessionManager.displayName()
                        + "\nRole: " + (SessionManager.getCurrentUser() == null ? "—" : SessionManager.getCurrentUser().getRole()));
    }

    @FXML
    private void onExit() {
        NavigationUtil.exitApplication();
    }

    @FXML
    private void onPeriodChanged() {
        loadSnapshot();
    }

    @FXML
    private void onAsOfDateChanged() {
        loadSnapshot();
    }

    @FXML
    private void onRefresh() {
        pingDatabaseAsync();
        loadSnapshot();
    }

    @FXML
    private void onHeaderSearch() {
        String needle = headerSearchField.getText() == null ? "" : headerSearchField.getText().trim().toLowerCase();
        if (needle.isBlank()) {
            loadSnapshot();
            return;
        }
        recentTable.setItems(FXCollections.observableArrayList(recentTable.getItems().stream()
                .filter(log -> (log.getAction() != null && log.getAction().toLowerCase().contains(needle))
                        || (log.getDescription() != null && log.getDescription().toLowerCase().contains(needle))
                        || (log.getUsername() != null && log.getUsername().toLowerCase().contains(needle)))
                .toList()));
    }

    private void loadSnapshot() {
        if (loading) {
            return;
        }
        loading = true;
        setBusy(true);
        statusLabel.setText("Loading live statistics from MySQL…");
        LocalDate asOf = asOfDatePicker.getValue() == null ? LocalDate.now() : asOfDatePicker.getValue();
        String period = periodCombo.getValue() == null ? "Today" : periodCombo.getValue();
        UiAsync.run(
                () -> dashboardService.load(asOf, period),
                snapshot -> {
                    applySnapshot(snapshot);
                    loading = false;
                    setBusy(false);
                    statusLabel.setText("Live statistics from MySQL as of " + asOf + " (" + period + ").");
                },
                error -> {
                    loading = false;
                    setBusy(false);
                    resetMetricPlaceholders();
                    recentTable.getItems().clear();
                    statusLabel.setText("Dashboard could not load. Check the database connection.");
                    UiAsync.runLater(() -> AlertUtil.error("Dashboard error",
                            error.getMessage() == null ? "Unable to load dashboard statistics." : error.getMessage()));
                }
        );
    }

    private void applySnapshot(DashboardSnapshot snapshot) {
        totalBikesValue.setText(String.valueOf(snapshot.getTotalBikes()));
        availableValue.setText(String.valueOf(snapshot.getAvailableBikes()));
        rentedValue.setText(String.valueOf(snapshot.getRentedBikes()));
        maintenanceValue.setText(String.valueOf(snapshot.getMaintenanceBikes()));
        customersValue.setText(String.valueOf(snapshot.getTotalCustomers()));
        activeRentalsValue.setText(String.valueOf(snapshot.getActiveRentals()));
        revenueValue.setText("₹ " + snapshot.getTotalRevenue().toPlainString());
        revenueTitleLabel.setText("TOTAL REVENUE");
        revenueTrendLabel.setText("₹ " + snapshot.getRevenue().toPlainString() + "  " + snapshot.getRevenueLabel().toLowerCase());
        pendingApprovalsValue.setText(String.valueOf(snapshot.getPendingApprovals()));
        pendingPayValue.setText(String.valueOf(snapshot.getPendingPayments()));
        pendingReturnValue.setText(String.valueOf(snapshot.getPendingReturns()));
        penaltyValue.setText(String.valueOf(snapshot.getPenalties()));
        incidentValue.setText(String.valueOf(snapshot.getIncidents()));
        availabilityLabel.setText("of " + snapshot.getTotalBikes() + " in fleet");
        fleetAvailableLabel.setText("AVAILABLE     " + snapshot.getAvailableBikes());
        fleetRentedLabel.setText("RENTED        " + snapshot.getRentedBikes());
        fleetMaintLabel.setText("MAINTENANCE   " + snapshot.getMaintenanceBikes());
        recentTable.setItems(FXCollections.observableArrayList(snapshot.getRecentActivity()));
        loadPendingCards();
    }

    private void loadPendingCards() {
        pendingApprovalPane.getChildren().clear();
        try {
            var pending = rentalService.findPendingApprovals();
            pendingEmptyLabel.setVisible(pending.isEmpty());
            pendingEmptyLabel.setManaged(pending.isEmpty());
            pending.stream().limit(4).forEach(rental -> pendingApprovalPane.getChildren().add(pendingCard(rental)));
        } catch (RuntimeException ex) {
            pendingEmptyLabel.setText("No pending approvals");
            pendingEmptyLabel.setVisible(true);
            pendingEmptyLabel.setManaged(true);
        }
    }

    private VBox pendingCard(Rental rental) {
        VBox box = new VBox(6);
        box.getStyleClass().add("stat-card");
        box.setPrefWidth(280);
        Label id = new Label(rental.displayId());
        id.getStyleClass().add("kicker-label");
        Label title = new Label(rental.getBikeLabel());
        title.getStyleClass().add("panel-title");
        Label body = new Label(rental.getCustomerName() + "\n"
                + rental.getStartDate() + " → " + rental.getExpectedReturnDate()
                + "\n₹ " + rental.getFinalAmount()
                + "\n● " + ("PAID".equalsIgnoreCase(rental.getPaymentStatus()) ? "PAYMENT VERIFIED" : rental.getPaymentStatus()));
        body.setWrapText(true);
        body.getStyleClass().add("muted-label");
        Button review = new Button("REVIEW");
        review.getStyleClass().add("secondary-button");
        review.setOnAction(event -> NavigationUtil.showView(AppView.APPROVAL_CENTER));
        Button approve = new Button("APPROVE");
        approve.getStyleClass().add("primary-button");
        approve.setOnAction(event -> NavigationUtil.showView(AppView.APPROVAL_CENTER));
        HBox actions = new HBox(8, review, approve);
        box.getChildren().addAll(id, title, body, actions);
        return box;
    }

    private void resetMetricPlaceholders() {
        totalBikesValue.setText(AppConstants.PLACEHOLDER_VALUE);
        availableValue.setText(AppConstants.PLACEHOLDER_VALUE);
        rentedValue.setText(AppConstants.PLACEHOLDER_VALUE);
        maintenanceValue.setText(AppConstants.PLACEHOLDER_VALUE);
        customersValue.setText(AppConstants.PLACEHOLDER_VALUE);
        activeRentalsValue.setText(AppConstants.PLACEHOLDER_VALUE);
        revenueValue.setText(AppConstants.PLACEHOLDER_VALUE);
        revenueTitleLabel.setText("TOTAL REVENUE");
        revenueTrendLabel.setText("");
        pendingApprovalsValue.setText(AppConstants.PLACEHOLDER_VALUE);
        pendingPayValue.setText(AppConstants.PLACEHOLDER_VALUE);
        pendingReturnValue.setText(AppConstants.PLACEHOLDER_VALUE);
        penaltyValue.setText(AppConstants.PLACEHOLDER_VALUE);
        incidentValue.setText(AppConstants.PLACEHOLDER_VALUE);
    }

    private void setBusy(boolean busy) {
        busyIndicator.setVisible(busy);
        busyIndicator.setManaged(busy);
        refreshButton.setDisable(busy);
        periodCombo.setDisable(busy);
        asOfDatePicker.setDisable(busy);
    }

    private void pingDatabaseAsync() {
        dbStatusLabel.setText("Database: checking…");
        UiAsync.run(
                () -> DatabaseConnection.getInstance().isReachable(),
                up -> {
                    dbStatusLabel.setText(up ? "Database: connected" : "Database: offline");
                    dbStatusLabel.getStyleClass().removeAll("ok", "down");
                    dbStatusLabel.getStyleClass().add(up ? "ok" : "down");
                    systemStatusLabel.setText(up ? "● SYSTEM ONLINE" : "● SYSTEM OFFLINE");
                    systemStatusLabel.getStyleClass().removeAll("system-online", "system-offline");
                    systemStatusLabel.getStyleClass().add(up ? "system-online" : "system-offline");
                    if (!up) {
                        statusLabel.setText("MySQL is offline. Start Docker with docker compose up -d.");
                    }
                },
                error -> {
                    LOG.error("Database ping failed", error);
                    dbStatusLabel.setText("Database: error");
                    dbStatusLabel.getStyleClass().removeAll("ok", "down");
                    dbStatusLabel.getStyleClass().add("down");
                    statusLabel.setText("Could not check the database. See the log for details.");
                }
        );
    }

    private void startClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
        clock = new Timeline(new KeyFrame(Duration.seconds(1),
                event -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void stopClock() {
        if (clock != null) {
            clock.stop();
        }
    }

    private void setNavActive(Button button, boolean active) {
        button.getStyleClass().remove("nav-active");
        if (active) {
            button.getStyleClass().add("nav-active");
        }
        Node graphic = button.getGraphic();
        if (graphic instanceof ImageView imageView && button.getUserData() instanceof IconFactory.Kind kind) {
            imageView.setImage(IconFactory.nav(kind, active, 18));
        }
    }

    private void decorateNav(Button button, IconFactory.Kind kind) {
        button.setUserData(kind);
        ImageView view = new ImageView(IconFactory.nav(kind, false, 18));
        view.setFitWidth(18);
        view.setFitHeight(18);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setPickOnBounds(true);
        button.setGraphic(view);
    }

    private String formatTime(AuditLog log) {
        if (log == null || log.getTimestamp() == null) {
            return AppConstants.PLACEHOLDER_VALUE;
        }
        return log.getTimestamp().format(AUDIT_FORMAT);
    }

    private String formatUser(AuditLog log) {
        if (log == null || log.getUsername() == null || log.getUsername().isBlank()) {
            return "system";
        }
        return log.getUsername();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? AppConstants.PLACEHOLDER_VALUE : value;
    }
}
