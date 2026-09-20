package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.ReportContext;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Bike;
import com.bikevault.model.Customer;
import com.bikevault.model.Payment;
import com.bikevault.model.Rental;
import com.bikevault.service.ReportService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.StatusCells;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filterable reports backed by live MySQL data and stream-based queries.
 */
public class ReportsController implements WorkspaceView {

    private static final String ALL = "ALL";

    private final ReportService reportService = new ReportService();

    private List<Rental> rentals = new ArrayList<>();
    private List<Bike> bikes = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private Map<Long, BigDecimal> paidByRental = new HashMap<>();

    @FXML private TabPane reportTabs;
    @FXML private Tab rentalsTab;
    @FXML private Tab availabilityTab;
    @FXML private Tab customersTab;
    @FXML private ProgressIndicator busyIndicator;
    @FXML private Label summaryLabel;

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private ComboBox<String> customerFilter;
    @FXML private ComboBox<String> bikeFilter;
    @FXML private ComboBox<String> rentalStatusFilter;
    @FXML private TableView<Rental> rentalTable;
    @FXML private TableColumn<Rental, String> rentalIdColumn;
    @FXML private TableColumn<Rental, String> rentalCustomerColumn;
    @FXML private TableColumn<Rental, String> rentalBikeColumn;
    @FXML private TableColumn<Rental, String> rentalStartColumn;
    @FXML private TableColumn<Rental, String> rentalExpectedColumn;
    @FXML private TableColumn<Rental, String> rentalStatusColumn;
    @FXML private TableColumn<Rental, String> rentalAmountColumn;
    @FXML private TableColumn<Rental, String> rentalPaidColumn;

    @FXML private ComboBox<String> bikeStatusFilter;
    @FXML private CheckBox fleetOnlyCheck;
    @FXML private Label availabilitySummary;
    @FXML private TableView<Bike> bikeTable;
    @FXML private TableColumn<Bike, String> bikeIdColumn;
    @FXML private TableColumn<Bike, String> bikeQrColumn;
    @FXML private TableColumn<Bike, String> bikeNameColumn;
    @FXML private TableColumn<Bike, String> bikeRegColumn;
    @FXML private TableColumn<Bike, String> bikeCatColumn;
    @FXML private TableColumn<Bike, String> bikeStatusColumn;
    @FXML private TableColumn<Bike, String> bikeRateColumn;

    @FXML private TextField customerSearchField;
    @FXML private Label customerSummary;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> custIdColumn;
    @FXML private TableColumn<Customer, String> custNameColumn;
    @FXML private TableColumn<Customer, String> custPhoneColumn;
    @FXML private TableColumn<Customer, String> custEmailColumn;
    @FXML private TableColumn<Customer, String> custLicenseColumn;
    @FXML private TableColumn<Customer, String> custStatusColumn;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        rentalStatusFilter.getItems().setAll(ALL, AppConstants.STATUS_ACTIVE, AppConstants.STATUS_COMPLETED, AppConstants.STATUS_CANCELLED);
        rentalStatusFilter.getSelectionModel().select(ALL);
        bikeStatusFilter.getItems().setAll(ALL, AppConstants.BIKE_AVAILABLE, AppConstants.BIKE_RENTED,
                AppConstants.BIKE_MAINTENANCE, AppConstants.BIKE_INACTIVE);
        bikeStatusFilter.getSelectionModel().select(ALL);

        rentalIdColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getId())));
        rentalCustomerColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getCustomerName())));
        rentalBikeColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getBikeLabel())));
        rentalStartColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getStartDate())));
        rentalExpectedColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getExpectedReturnDate())));
        rentalStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getStatus())));
        rentalAmountColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getFinalAmount() != null
                ? data.getValue().getFinalAmount() : data.getValue().getTotalAmount())));
        rentalPaidColumn.setCellValueFactory(data -> new SimpleStringProperty(text(
                paidByRental.getOrDefault(data.getValue().getId(), BigDecimal.ZERO))));
        StatusCells.apply(rentalStatusColumn);
        StatusCells.apply(bikeStatusColumn);
        StatusCells.apply(custStatusColumn);

        bikeIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayId()));
        bikeQrColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getQrCode())));
        bikeNameColumn.setCellValueFactory(data -> new SimpleStringProperty(bikeName(data.getValue())));
        bikeRegColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getRegistrationNumber())));
        bikeCatColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getCategoryName())));
        bikeStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getStatus())));
        bikeRateColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getDailyRate())));

        custIdColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getId())));
        custNameColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getFullName())));
        custPhoneColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getPhone())));
        custEmailColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getEmail())));
        custLicenseColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getDrivingLicenseNumber())));
        custStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(text(data.getValue().getStatus())));

        rentalTable.setPlaceholder(new Label("No rentals match the current filters."));
        bikeTable.setPlaceholder(new Label("No bikes match the current filters."));
        customerTable.setPlaceholder(new Label("No customers match the current search."));

        focusRequestedTab();
        reload();
    }

    @FXML private void onRefresh() { reload(); }
    @FXML private void onApplyRentalFilters() { applyRentalFilters(); }
    @FXML private void onApplyAvailabilityFilters() { applyAvailabilityFilters(); }
    @FXML private void onApplyCustomerFilters() { applyCustomerFilters(); }

    private void reload() {
        setBusy(true);
        summaryLabel.setText("Loading reports from MySQL…");
        UiAsync.run(() -> new Snapshot(reportService.rentals(), reportService.bikes(),
                reportService.customers(), reportService.payments()), snapshot -> {
            rentals = snapshot.rentals();
            bikes = snapshot.bikes();
            customers = snapshot.customers();
            paidByRental = reportService.completedPaidByRental(snapshot.payments());
            fillLookups();
            applyRentalFilters();
            applyAvailabilityFilters();
            applyCustomerFilters();
            setBusy(false);
            summaryLabel.setText(rentals.size() + " rentals · " + bikes.size() + " bikes · " + customers.size() + " customers");
        }, error -> {
            setBusy(false);
            summaryLabel.setText("Reports could not be loaded.");
            AlertUtil.error("Reports error", error.getMessage() == null ? "Unable to load reports." : error.getMessage());
        });
    }

    private void fillLookups() {
        List<String> customerNames = new ArrayList<>();
        customerNames.add(ALL);
        customers.stream().map(Customer::getFullName).distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(customerNames::add);
        String previousCustomer = customerFilter.getValue();
        customerFilter.getItems().setAll(customerNames);
        customerFilter.getSelectionModel().select(customerNames.contains(previousCustomer) ? previousCustomer : ALL);

        List<String> bikeLabels = new ArrayList<>();
        bikeLabels.add(ALL);
        bikes.stream().map(this::bikeChoice).forEach(bikeLabels::add);
        String previousBike = bikeFilter.getValue();
        bikeFilter.getItems().setAll(bikeLabels);
        bikeFilter.getSelectionModel().select(bikeLabels.contains(previousBike) ? previousBike : ALL);
    }

    private void applyRentalFilters() {
        Long bikeId = selectedBikeId();
        rentalTable.setItems(FXCollections.observableArrayList(
                reportService.filterRentals(rentals, fromPicker.getValue(), toPicker.getValue(),
                        customerFilter.getValue(), bikeId, rentalStatusFilter.getValue())));
    }

    private void applyAvailabilityFilters() {
        String status = bikeStatusFilter.getValue();
        List<Bike> filtered = reportService.filterAvailability(bikes, status);
        if (fleetOnlyCheck.isSelected()) {
            filtered = filtered.stream()
                    .filter(bike -> !AppConstants.BIKE_INACTIVE.equalsIgnoreCase(bike.getStatus()))
                    .toList();
        }
        bikeTable.setItems(FXCollections.observableArrayList(filtered));
        long available = reportService.countByBikeStatus(filtered, AppConstants.BIKE_AVAILABLE);
        long rented = reportService.countByBikeStatus(filtered, AppConstants.BIKE_RENTED);
        long maintenance = reportService.countByBikeStatus(filtered, AppConstants.BIKE_MAINTENANCE);
        availabilitySummary.setText(available + " available · " + rented + " rented · " + maintenance + " maintenance");
    }

    private void applyCustomerFilters() {
        List<Customer> filtered = reportService.filterCustomers(customers, customerSearchField.getText());
        customerTable.setItems(FXCollections.observableArrayList(filtered));
        customerSummary.setText(filtered.size() + " customer" + (filtered.size() == 1 ? "" : "s"));
    }

    private Long selectedBikeId() {
        String selected = bikeFilter.getValue();
        if (selected == null || ALL.equals(selected)) {
            return null;
        }
        return bikes.stream()
                .filter(bike -> bikeChoice(bike).equals(selected))
                .map(Bike::getId)
                .findFirst()
                .orElse(null);
    }

    private void focusRequestedTab() {
        ReportContext.Focus focus = ReportContext.take();
        if (focus == ReportContext.Focus.AVAILABILITY) {
            reportTabs.getSelectionModel().select(availabilityTab);
        } else if (focus == ReportContext.Focus.CUSTOMERS) {
            reportTabs.getSelectionModel().select(customersTab);
        } else {
            reportTabs.getSelectionModel().select(rentalsTab);
        }
    }

    private void setBusy(boolean busy) {
        busyIndicator.setVisible(busy);
        busyIndicator.setManaged(busy);
    }

    private String bikeChoice(Bike bike) {
        return bike.displayId() + " — " + bikeName(bike);
    }

    private String bikeName(Bike bike) {
        String brand = bike.getBrand() == null ? "" : bike.getBrand();
        String model = bike.getModel() == null ? "" : bike.getModel();
        return (brand + " " + model).trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record Snapshot(List<Rental> rentals, List<Bike> bikes, List<Customer> customers, List<Payment> payments) {
    }
}
