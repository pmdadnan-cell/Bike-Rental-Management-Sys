package com.bikevault.app;

/**
 * Application-wide constants. Avoids magic strings in UI, SQL mapping, and services.
 */
public final class AppConstants {

    public static final String APP_NAME = "BIKEVAULT";
    public static final String APP_TAGLINE = "Premium Bike Rental Management";
    public static final String APP_WINDOW_TITLE = "BIKEVAULT — Premium Bike Rental Management";

    public static final String LOGIN_FXML = "/com/bikevault/view/Login.fxml";
    public static final String DASHBOARD_FXML = "/com/bikevault/view/Dashboard.fxml";
    public static final String STYLESHEET = "/com/bikevault/css/style.css";

    public static final String CUSTOMER_SHELL_FXML = "/com/bikevault/view/CustomerShell.fxml";
    public static final String REGISTER_FXML = "/com/bikevault/view/Register.fxml";
    public static final String CUSTOMER_STYLESHEET = "/com/bikevault/css/customer.css";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    public static final String BIKE_AVAILABLE = "AVAILABLE";
    public static final String BIKE_RENTED = "RENTED";
    public static final String BIKE_MAINTENANCE = "MAINTENANCE";
    public static final String BIKE_INACTIVE = "INACTIVE";

    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_ADD_BIKE = "ADD_BIKE";
    public static final String ACTION_UPDATE_BIKE = "UPDATE_BIKE";
    public static final String ACTION_DELETE_BIKE = "DELETE_BIKE";
    public static final String ACTION_GENERATE_QR = "GENERATE_QR";
    public static final String ACTION_SCAN_QR = "SCAN_QR";
    public static final String ACTION_CREATE_RENTAL = "CREATE_RENTAL";
    public static final String ACTION_RETURN_BIKE = "RETURN_BIKE";
    public static final String ACTION_PAYMENT = "PAYMENT";
    public static final String ACTION_CHANGE_PASSWORD = "CHANGE_PASSWORD";
    public static final String ACTION_CUSTOMER_LOGIN = "CUSTOMER_LOGIN";
    public static final String ACTION_ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String ACTION_STAFF_LOGIN = "STAFF_LOGIN";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_BIKE_RENTED = "BIKE_RENTED";
    public static final String ACTION_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String ACTION_INCIDENT_REPORTED = "INCIDENT_REPORTED";
    public static final String ACTION_PENALTY_CREATED = "PENALTY_CREATED";
    public static final String ACTION_CHALLAN_CREATED = "CHALLAN_CREATED";
    public static final String STATUS_OVERDUE = "OVERDUE";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_WAIVED = "WAIVED";
    public static final String STATUS_DISPUTED = "DISPUTED";
    public static final String STATUS_REPORTED = "REPORTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String ACTION_RENTAL_REQUESTED = "RENTAL_REQUESTED";
    public static final String ACTION_RENTAL_CREATED = "RENTAL_CREATED";
    public static final String ACTION_RENTAL_QR_GENERATED = "RENTAL_QR_GENERATED";
    public static final String ACTION_RENTAL_QR_SCANNED = "RENTAL_QR_SCANNED";
    public static final String ACTION_PAYMENT_VERIFIED = "PAYMENT_VERIFIED";
    public static final String ACTION_RENTAL_APPROVED = "RENTAL_APPROVED";
    public static final String ACTION_RENTAL_REJECTED = "RENTAL_REJECTED";
    public static final String ACTION_BIKE_COLLECTED = "BIKE_COLLECTED";
    public static final String ACTION_RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String ACTION_RETURN_QR_SCANNED = "RETURN_QR_SCANNED";
    public static final String ACTION_RETURN_ACCEPTED = "RETURN_ACCEPTED";
    public static final String ACTION_PENALTY_PAYMENT_STARTED = "PENALTY_PAYMENT_STARTED";
    public static final String ACTION_PENALTY_PAYMENT_COMPLETED = "PENALTY_PAYMENT_COMPLETED";
    public static final String ACTION_PENALTY_PAYMENT_FAILED = "PENALTY_PAYMENT_FAILED";

    public static final String PLACEHOLDER_VALUE = "—";
    public static final String QR_DIRECTORY = "generated_qr";

    public static final String PAYMENT_CASH = "CASH";
    public static final String PAYMENT_CARD = "CARD";
    public static final String PAYMENT_UPI = "UPI";
    public static final String PAYMENT_NET_BANKING = "NET_BANKING";

    public static final double LOGIN_WIDTH = 980;
    public static final double LOGIN_HEIGHT = 640;
    public static final double DASHBOARD_WIDTH = 1440;
    public static final double DASHBOARD_HEIGHT = 860;

    private AppConstants() {
    }
}
