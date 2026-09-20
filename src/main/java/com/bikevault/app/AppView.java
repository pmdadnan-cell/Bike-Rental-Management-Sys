package com.bikevault.app;

/**
 * Named application views used by the navigation framework.
 */
public enum AppView {
    DASHBOARD("Dashboard", "/com/bikevault/view/Dashboard.fxml", true, Kind.OPERATOR),
    APPROVAL_CENTER("Approvals", "/com/bikevault/view/ApprovalCenter.fxml", true, Kind.OPERATOR),
    CUSTOMERS("Customers", "/com/bikevault/view/Customer.fxml", true, Kind.OPERATOR),
    BIKES("Bikes", "/com/bikevault/view/Bike.fxml", true, Kind.OPERATOR),
    CATEGORIES("Categories", "/com/bikevault/view/Category.fxml", true, Kind.OPERATOR),
    RENTALS("Rentals", "/com/bikevault/view/Rental.fxml", true, Kind.OPERATOR),
    RETURNS("Returns", "/com/bikevault/view/Return.fxml", true, Kind.OPERATOR),
    PAYMENTS("Payments", "/com/bikevault/view/Payment.fxml", true, Kind.OPERATOR),
    QR_SCANNER("QR Verification", "/com/bikevault/view/QRScanner.fxml", true, Kind.OPERATOR),
    REPORTS("Reports", "/com/bikevault/view/Reports.fxml", true, Kind.OPERATOR),
    SETTINGS("Settings", "/com/bikevault/view/Settings.fxml", true, Kind.OPERATOR),
    PENALTIES("Penalties", "/com/bikevault/view/Penalty.fxml", true, Kind.OPERATOR),
    INCIDENTS("Incidents", "/com/bikevault/view/Incident.fxml", true, Kind.OPERATOR),
    AUDIT_LOGS("Audit Log", "/com/bikevault/view/Audit.fxml", true, Kind.OPERATOR),
    CUSTOMER_HOME("Home", "/com/bikevault/view/CustomerHome.fxml", true, Kind.CUSTOMER),
    BROWSE_BIKES("Browse Bikes", "/com/bikevault/view/CustomerBrowse.fxml", true, Kind.CUSTOMER),
    CUSTOMER_RENT("Rent Bike", "/com/bikevault/view/CustomerRent.fxml", true, Kind.CUSTOMER),
    CUSTOMER_PAY("Payment", "/com/bikevault/view/CustomerPay.fxml", true, Kind.CUSTOMER),
    CUSTOMER_RETURN("Return Bike", "/com/bikevault/view/CustomerReturn.fxml", true, Kind.CUSTOMER),
    MY_TRIPS("My Trips", "/com/bikevault/view/CustomerTrips.fxml", true, Kind.CUSTOMER),
    CUSTOMER_TRIP("Trip Details", "/com/bikevault/view/CustomerTrip.fxml", true, Kind.CUSTOMER),
    CUSTOMER_PASS("Rental Pass", "/com/bikevault/view/CustomerPass.fxml", true, Kind.CUSTOMER),
    CUSTOMER_QR("Rental QR", "/com/bikevault/view/CustomerQr.fxml", true, Kind.CUSTOMER),
    MY_RENTALS("My Trips", "/com/bikevault/view/CustomerTrips.fxml", true, Kind.CUSTOMER),
    MY_PAYMENTS("My Trips", "/com/bikevault/view/CustomerTrips.fxml", true, Kind.CUSTOMER),
    MY_PENALTIES("My Penalties", "/com/bikevault/view/CustomerPenalties.fxml", true, Kind.CUSTOMER),
    CUSTOMER_PENALTY_PAY("Pay Penalty", "/com/bikevault/view/CustomerPenaltyPay.fxml", true, Kind.CUSTOMER),
    REPORT_INCIDENT("Report Incident", "/com/bikevault/view/CustomerIncident.fxml", true, Kind.CUSTOMER),
    MY_PROFILE("My Profile", "/com/bikevault/view/CustomerProfile.fxml", true, Kind.CUSTOMER);

    public enum Kind {
        OPERATOR,
        CUSTOMER,
        SHARED
    }

    private final String title;
    private final String fxmlPath;
    private final boolean delivered;
    private final Kind kind;

    AppView(String title, String fxmlPath, boolean delivered, Kind kind) {
        this.title = title;
        this.fxmlPath = fxmlPath;
        this.delivered = delivered;
        this.kind = kind;
    }

    public String getTitle() {
        return title;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isOperatorView() {
        return kind == Kind.OPERATOR || kind == Kind.SHARED;
    }

    public boolean isCustomerView() {
        return kind == Kind.CUSTOMER || kind == Kind.SHARED;
    }
}
