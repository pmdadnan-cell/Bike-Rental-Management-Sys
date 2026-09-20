package com.bikevault.controller;

import com.bikevault.app.AccessGuard;
import com.bikevault.app.AppConstants;
import com.bikevault.app.AppView;
import com.bikevault.app.ScanContext;
import com.bikevault.app.SessionManager;
import com.bikevault.app.WorkspaceView;
import com.bikevault.model.Bike;
import com.bikevault.model.BikeCategory;
import com.bikevault.service.BikeService;
import com.bikevault.service.CategoryService;
import com.bikevault.util.AlertUtil;
import com.bikevault.util.BikeImageLoader;
import com.bikevault.util.StatusCells;
import com.bikevault.util.UiAsync;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BikeController implements WorkspaceView {

    private final BikeService bikeService = new BikeService();
    private final CategoryService categoryService = new CategoryService();
    private List<Bike> cache = new ArrayList<>();

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField idField;
    @FXML private TextField qrField;
    @FXML private TextField brandField;
    @FXML private TextField modelField;
    @FXML private TextField registrationField;
    @FXML private ComboBox<BikeCategory> categoryCombo;
    @FXML private TextField colorField;
    @FXML private TextField rateField;
    @FXML private DatePicker purchasePicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea descriptionArea;
    @FXML private ImageView qrImageView;
    @FXML private javafx.scene.control.Label qrPlaceholder;
    @FXML private javafx.scene.control.Label cardIdLabel;
    @FXML private javafx.scene.control.Label cardQrLabel;
    @FXML private javafx.scene.control.Label cardBrandLabel;
    @FXML private javafx.scene.control.Label cardModelLabel;
    @FXML private javafx.scene.control.Label cardRegLabel;
    @FXML private javafx.scene.control.Label cardCategoryLabel;
    @FXML private javafx.scene.control.Label cardRateLabel;
    @FXML private javafx.scene.control.Label cardStatusLabel;
    @FXML private FlowPane bikeCardPane;
    @FXML private HBox editorRow;
    @FXML private TableView<Bike> table;
    @FXML private TableColumn<Bike, String> idColumn;
    @FXML private TableColumn<Bike, String> qrColumn;
    @FXML private TableColumn<Bike, String> brandColumn;
    @FXML private TableColumn<Bike, String> modelColumn;
    @FXML private TableColumn<Bike, String> regColumn;
    @FXML private TableColumn<Bike, String> categoryColumn;
    @FXML private TableColumn<Bike, String> rateColumn;
    @FXML private TableColumn<Bike, String> statusColumn;
    @FXML private Button deleteButton;

    @FXML
    private void initialize() {
        AccessGuard.assertOperator();
        statusFilter.getItems().setAll("ALL", AppConstants.BIKE_AVAILABLE, AppConstants.BIKE_RENTED,
                AppConstants.BIKE_MAINTENANCE, AppConstants.BIKE_INACTIVE);
        statusFilter.getSelectionModel().select("ALL");
        statusCombo.getItems().setAll(AppConstants.BIKE_AVAILABLE, AppConstants.BIKE_RENTED,
                AppConstants.BIKE_MAINTENANCE, AppConstants.BIKE_INACTIVE);
        statusCombo.getSelectionModel().select(AppConstants.BIKE_AVAILABLE);

        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayId()));
        qrColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQrCode()));
        brandColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBrand()));
        modelColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModel()));
        regColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRegistrationNumber()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        rateColumn.setCellValueFactory(data -> new SimpleStringProperty(value(data.getValue().getDailyRate())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        StatusCells.apply(statusColumn);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                bindForm(selected);
            }
        });
        table.setPlaceholder(new javafx.scene.control.Label("No bikes found."));
        if (!SessionManager.isAdmin()) {
            deleteButton.setDisable(true);
        }

        UiAsync.run(categoryService::findActive, categories -> {
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
        });
        reload();
        onGridView();
        ScanContext.takePending().ifPresent(this::acceptScannedBike);
    }

    @FXML private void onSearch() { applyFilter(); }
    @FXML private void onRefresh() { reload(); }

    @FXML
    private void onGridView() {
        if (bikeCardPane != null) {
            bikeCardPane.setVisible(true);
            bikeCardPane.setManaged(true);
        }
        if (editorRow != null) {
            editorRow.setVisible(false);
            editorRow.setManaged(false);
        }
    }

    @FXML
    private void onTableView() {
        if (bikeCardPane != null) {
            bikeCardPane.setVisible(false);
            bikeCardPane.setManaged(false);
        }
        if (editorRow != null) {
            editorRow.setVisible(true);
            editorRow.setManaged(true);
        }
    }
    @FXML private void onScanQr() { ScanContext.requestScan(AppView.BIKES); }

    @FXML
    private void onGenerateQr() {
        try {
            Bike current = readForm(idField.getText() != null && !idField.getText().isBlank());
            if (current.getId() == null && (qrField.getText() == null || qrField.getText().isBlank())) {
                AlertUtil.warn("Save first", "Add the bike so a unique QR identity can be stored in MySQL.");
                return;
            }
            Path path = bikeService.generateQrImage(table.getSelectionModel().getSelectedItem() == null
                    ? current : table.getSelectionModel().getSelectedItem());
            showIdentity(table.getSelectionModel().getSelectedItem() == null
                    ? current : table.getSelectionModel().getSelectedItem());
            AlertUtil.info("QR generated", "QR image saved to " + path.toAbsolutePath());
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML
    private void onAdd() {
        try {
            Bike saved = bikeService.add(readForm(true));
            AlertUtil.info("Bike saved", saved.displayId() + " was registered with QR " + saved.getQrCode() + ".");
            reload();
            bindForm(saved);
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML private void onSave() { persist(idField.getText() == null || idField.getText().isBlank()); }
    @FXML private void onUpdate() { persist(false); }

    @FXML
    private void onDelete() {
        Bike selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.warn("Select a bike", "Choose a row before deleting.");
            return;
        }
        if (AppConstants.BIKE_RENTED.equalsIgnoreCase(selected.getStatus())) {
            AlertUtil.warn("Bike is rented", "This bike is currently rented.");
            return;
        }
        if (!AccessGuard.requireAdmin("delete bikes")) {
            return;
        }
        if (!AlertUtil.confirm("Delete bike", "Are you sure you want to delete " + selected.displayId() + "?")) {
            return;
        }
        try {
            bikeService.delete(selected.getId());
            AlertUtil.info("Bike deleted", "The bike was removed.");
            reload();
            clearForm();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    @FXML public void onClear() { clearForm(); }

    @Override public void addRecord() { onAdd(); }
    @Override public void saveRecord() { onSave(); }

    @Override
    public void clearForm() {
        table.getSelectionModel().clearSelection();
        idField.clear();
        qrField.clear();
        brandField.clear();
        modelField.clear();
        registrationField.clear();
        colorField.clear();
        rateField.clear();
        descriptionArea.clear();
        purchasePicker.setValue(null);
        statusCombo.getSelectionModel().select(AppConstants.BIKE_AVAILABLE);
        categoryCombo.getSelectionModel().clearSelection();
        idField.setUserData(null);
        qrImageView.setImage(null);
        qrPlaceholder.setVisible(true);
        cardIdLabel.setText("BIKE-—");
        cardQrLabel.setText("QR ID");
        cardBrandLabel.setText("Brand");
        cardModelLabel.setText("Model");
        cardRegLabel.setText("Registration");
        cardCategoryLabel.setText("Category");
        cardRateLabel.setText("Daily rate");
        cardStatusLabel.setText("STATUS");
        cardStatusLabel.getStyleClass().removeAll("status-available", "status-rented", "status-maintenance", "status-inactive");
    }

    private void persist(boolean create) {
        try {
            if (create) {
                onAdd();
                return;
            }
            if (bikeService.update(readForm(false))) {
                AlertUtil.info("Bike updated", "Changes were saved. The QR identity was not changed.");
                reload();
            }
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void acceptScannedBike(Bike bike) {
        AlertUtil.info("QR code successfully verified.",
                "Bike " + bike.displayId() + " — " + bike.getBrand() + " " + bike.getModel());
        bindForm(bike);
        cache.stream().filter(item -> item.getId().equals(bike.getId())).findFirst()
                .ifPresent(item -> table.getSelectionModel().select(item));
    }

    private void reload() {
        UiAsync.run(bikeService::findAll, rows -> {
            cache = rows;
            applyFilter();
        });
    }

    private void applyFilter() {
        List<Bike> rows = bikeService.search(cache, searchField.getText(), statusFilter.getValue());
        table.setItems(FXCollections.observableArrayList(rows));
        renderCards(rows);
    }

    private void renderCards(List<Bike> rows) {
        if (bikeCardPane == null) {
            return;
        }
        bikeCardPane.getChildren().clear();
        for (Bike bike : rows) {
            VBox card = new VBox(8);
            card.getStyleClass().add("admin-bike-card");
            card.setPrefWidth(240);
            card.setMinWidth(240);
            card.getChildren().add(BikeImageLoader.framed(bike, 240, 135));
            VBox body = new VBox(4);
            body.setPadding(new javafx.geometry.Insets(0, 12, 0, 12));
            javafx.scene.control.Label brand = new javafx.scene.control.Label(bike.getBrand());
            brand.getStyleClass().add("kicker-label");
            javafx.scene.control.Label model = new javafx.scene.control.Label(bike.getModel());
            model.getStyleClass().add("panel-title");
            javafx.scene.control.Label meta = new javafx.scene.control.Label(
                    (bike.getCategoryName() == null ? "Fleet" : bike.getCategoryName())
                            + " · " + (bike.getColor() == null ? "" : bike.getColor())
                            + "\n" + bike.getRegistrationNumber()
                            + "\n₹ " + bike.getDailyRate() + "/day");
            meta.getStyleClass().add("muted-label");
            javafx.scene.control.Label status = new javafx.scene.control.Label("● " + bike.getStatus());
            status.getStyleClass().addAll("action-badge",
                    AppConstants.BIKE_AVAILABLE.equalsIgnoreCase(bike.getStatus()) ? "badge-success"
                            : AppConstants.BIKE_RENTED.equalsIgnoreCase(bike.getStatus()) ? "badge-warning" : "badge-muted");
            Button view = new Button("VIEW");
            view.getStyleClass().add("ghost-button");
            view.setMinWidth(88);
            view.setOnAction(event -> {
                onTableView();
                bindForm(bike);
                table.getSelectionModel().select(bike);
            });
            Button edit = new Button("EDIT");
            edit.getStyleClass().add("secondary-button");
            edit.setMinWidth(88);
            edit.setOnAction(event -> {
                onTableView();
                bindForm(bike);
                table.getSelectionModel().select(bike);
            });
            HBox actions = new HBox(8, view, edit);
            actions.setPadding(new javafx.geometry.Insets(4, 12, 0, 12));
            body.getChildren().addAll(brand, model, meta, status);
            card.getChildren().addAll(body, actions);
            bikeCardPane.getChildren().add(card);
        }
    }

    private void bindForm(Bike bike) {
        idField.setText(bike.displayId());
        qrField.setText(bike.getQrCode());
        brandField.setText(bike.getBrand());
        modelField.setText(bike.getModel());
        registrationField.setText(bike.getRegistrationNumber());
        colorField.setText(bike.getColor());
        rateField.setText(value(bike.getDailyRate()));
        purchasePicker.setValue(bike.getPurchaseDate());
        statusCombo.getSelectionModel().select(bike.getStatus());
        descriptionArea.setText(bike.getDescription());
        if (bike.getCategoryId() != null) {
            categoryCombo.getItems().stream()
                    .filter(category -> bike.getCategoryId().equals(category.getId()))
                    .findFirst()
                    .ifPresent(category -> categoryCombo.getSelectionModel().select(category));
        }
        idField.setUserData(bike.getId());
        showIdentity(bike);
    }

    private Bike readForm(boolean creating) {
        Bike bike = new Bike();
        if (!creating && idField.getUserData() instanceof Long id) {
            bike.setId(id);
        }
        bike.setQrCode(qrField.getText());
        bike.setBrand(brandField.getText());
        bike.setModel(modelField.getText());
        bike.setRegistrationNumber(registrationField.getText());
        BikeCategory category = categoryCombo.getValue();
        if (category != null) {
            bike.setCategoryId(category.getId());
            bike.setCategoryName(category.getName());
        }
        bike.setColor(colorField.getText());
        bike.setPurchaseDate(purchasePicker.getValue());
        bike.setStatus(statusCombo.getValue());
        bike.setDescription(descriptionArea.getText());
        if (idField.getUserData() instanceof Long id) {
            cache.stream().filter(item -> id.equals(item.getId())).findFirst()
                    .ifPresent(existing -> bike.setImagePath(existing.getImagePath()));
        }
        if (bike.getImagePath() == null || bike.getImagePath().isBlank()) {
            bike.setImagePath(BikeImageLoader.pathFor(bike.getBrand(), bike.getModel()));
        }
        try {
            bike.setDailyRate(new BigDecimal(rateField.getText() == null || rateField.getText().isBlank()
                    ? "0" : rateField.getText().trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Daily rental rate must be a number.");
        }
        return bike;
    }

    private void showIdentity(Bike bike) {
        cardIdLabel.setText(bike.displayId().isEmpty() ? "BIKE-—" : bike.displayId());
        cardQrLabel.setText(bike.getQrCode() == null || bike.getQrCode().isBlank() ? "QR ID" : bike.getQrCode());
        cardBrandLabel.setText(bike.getBrand() == null ? "Brand" : bike.getBrand());
        cardModelLabel.setText(bike.getModel() == null ? "Model" : bike.getModel());
        cardRegLabel.setText(bike.getRegistrationNumber() == null ? "Registration" : bike.getRegistrationNumber());
        cardCategoryLabel.setText(bike.getCategoryName() == null ? "Category" : bike.getCategoryName());
        cardRateLabel.setText(bike.getDailyRate() == null ? "Daily rate" : "₹ " + bike.getDailyRate());
        cardStatusLabel.setText(bike.getStatus() == null ? "STATUS" : bike.getStatus());
        cardStatusLabel.getStyleClass().removeAll("status-available", "status-rented", "status-maintenance", "status-inactive");
        cardStatusLabel.getStyleClass().add(StatusCells.styleFor(bike.getStatus()));
        if (bike.getQrCode() == null || bike.getQrCode().isBlank()) {
            qrImageView.setImage(null);
            qrPlaceholder.setVisible(true);
            return;
        }
        Path path = bikeService.qrImagePath(bike.getQrCode());
        if (!Files.exists(path)) {
            try {
                path = bikeService.generateQrImage(bike);
            } catch (RuntimeException ex) {
                qrImageView.setImage(null);
                qrPlaceholder.setVisible(true);
                return;
            }
        }
        qrImageView.setImage(new Image(path.toUri().toString(), true));
        qrPlaceholder.setVisible(false);
    }

    private String value(Object object) {
        return object == null ? "" : String.valueOf(object);
    }

    private void showError(RuntimeException ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            AlertUtil.warn("Check the form", ex.getMessage());
        } else {
            AlertUtil.error("Could not save bike", ex.getMessage());
        }
    }
}
