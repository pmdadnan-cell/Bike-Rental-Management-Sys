package com.bikevault;

import com.bikevault.database.DatabaseConnection;
import com.bikevault.util.PasswordUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One-off loader that fills BikeVault with ~200 customers and months of desk history.
 * Safe to re-run: it skips when {@code demo_c001} already exists.
 */
public final class LivedataSeeder {

    private static final String PASSWORD = "hassan123";
    private static final String[] FIRST = {
            "Arjun", "Rohan", "Vikram", "Kabir", "Aditya", "Rahul", "Karan", "Dev", "Ishaan", "Nikhil",
            "Amit", "Suresh", "Farhan", "Imran", "Sameer", "Varun", "Manish", "Pranav", "Yash", "Harsh",
            "Kunal", "Abhay", "Naveen", "Deepak", "Mohit", "Ananya", "Meera", "Priya", "Isha", "Diya",
            "Kavya", "Sneha", "Pooja", "Riya", "Nisha", "Aisha", "Fatima", "Zara", "Aditi", "Neha",
            "Shreya", "Tanvi", "Anjali", "Divya", "Swati", "Lakshmi", "Myra", "Ira", "Avni", "Sara"
    };
    private static final String[] LAST = {
            "Sharma", "Patel", "Khan", "Reddy", "Nair", "Iyer", "Joshi", "Gupta", "Mehta", "Das",
            "Bose", "Rao", "Menon", "Pillai", "Singh", "Kapoor", "Malhotra", "Shetty", "Kulkarni", "Pawar",
            "Choudhary", "Verma", "Saxena", "Agarwal", "Bansal", "Deshpande", "Jadhav", "Bhat", "Banerjee", "Sheikh"
    };
    private static final String[][] CITIES = {
            {"12 MG Road, Bengaluru", "KA"}, {"44 Church Street, Bengaluru", "KA"}, {"8 Dewan's Road, Mysuru", "KA"},
            {"19 Panambur, Mangaluru", "KA"}, {"21 Banjara Hills, Hyderabad", "TS"}, {"5 Jubilee Hills, Hyderabad", "TS"},
            {"33 Anna Nagar, Chennai", "TN"}, {"18 T Nagar, Chennai", "TN"}, {"9 Marine Drive, Kochi", "KL"},
            {"4 Kowdiar, Thiruvananthapuram", "KL"}, {"72 Linking Road, Mumbai", "MH"}, {"16 FC Road, Pune", "MH"},
            {"7 Civil Lines, Jaipur", "RJ"}, {"28 CG Road, Ahmedabad", "GJ"}, {"11 Khan Market, Delhi", "DL"},
            {"22 Hazratganj, Lucknow", "UP"}, {"14 Sector 17, Chandigarh", "CH"}, {"6 Park Street, Kolkata", "WB"},
            {"31 Vijay Nagar, Indore", "MP"}, {"15 Beach Road, Visakhapatnam", "AP"}
    };
    private static final String[] METHODS = {"UPI", "CARD", "CASH", "NET_BANKING"};
    private static final String[] RETURN_NOTES = {
            "Returned clean, tank three-quarters full",
            "Minor scratch on the tank, noted at desk",
            "Helmet and papers returned with the bike",
            "Front tyre worn, flagged for workshop",
            "On-time return, no damage",
            "Customer reported a rattle; sent to service"
    };
    private static final String[] REJECT_REASONS = {
            "Incomplete documents", "License photo unclear", "Duplicate booking",
            "Customer cancelled after payment hold", "Bike reserved for service"
    };

    private LivedataSeeder() {
    }

    public static void main(String[] args) {
        String result = DatabaseConnection.getInstance().inTransaction(LivedataSeeder::seed);
        System.out.println(result);
    }

    private static String seed(Connection connection) throws SQLException {
        if (exists(connection, "SELECT 1 FROM users WHERE username = 'demo_c001'")) {
            return "Livedata already loaded — demo_c001 exists. No changes made.";
        }

        long adminId = scalarLong(connection, "SELECT user_id FROM users WHERE username = 'admin'");
        long staffId = scalarLong(connection, "SELECT user_id FROM users WHERE username = 'staff'");
        String hash = PasswordUtil.hash(PASSWORD);

        List<Long> customerIds = insertCustomers(connection, hash);
        List<BikeRow> bikes = loadBikes(connection);
        if (bikes.isEmpty()) {
            throw new SQLException("No bikes in the fleet — load seed/fleet first.");
        }

        List<Long> busyBikes = queryIds(connection,
                "SELECT DISTINCT bike_id FROM rentals WHERE status IN ('PENDING_APPROVAL','APPROVED','ACTIVE','RETURN_REQUESTED')");
        List<Long> busyCustomers = queryIds(connection,
                "SELECT DISTINCT customer_id FROM rentals WHERE status IN ('PENDING_APPROVAL','APPROVED','ACTIVE','RETURN_REQUESTED')");

        int completed = insertCompletedHistory(connection, customerIds, bikes, adminId, staffId);
        int open = insertOpenWork(connection, customerIds, bikes, busyBikes, busyCustomers, adminId, staffId);
        enrichExistingCustomers(connection, bikes, adminId, staffId);
        backdateFreshRows(connection);
        syncBikeStatuses(connection);

        return "Loaded " + customerIds.size() + " customers, " + completed + " completed trips, "
                + open + " live desk jobs. Extra customer password: " + PASSWORD;
    }

    private static List<Long> insertCustomers(Connection connection, String hash) throws SQLException {
        String userSql = """
                INSERT INTO users (username, password_hash, full_name, role, status, created_at, updated_at)
                VALUES (?, ?, ?, 'CUSTOMER', ?, ?, ?)
                """;
        String customerSql = """
                INSERT INTO customers (
                    user_id, full_name, email, phone, address, driving_license_number,
                    date_of_birth, gender, status, registration_date, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        List<Long> ids = new ArrayList<>(200);
        try (PreparedStatement users = connection.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement customers = connection.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= 200; i++) {
                String first = FIRST[(i - 1) % FIRST.length];
                String last = LAST[(i * 7) % LAST.length];
                String fullName = first + " " + last;
                String gender = genderFor(first, i);
                String status = i % 23 == 0 ? "INACTIVE" : "ACTIVE";
                String[] city = CITIES[(i * 3) % CITIES.length];
                LocalDate registered = LocalDate.of(2024, 1, 8).plusDays((i * 4L) % 610);
                LocalDateTime created = registered.atTime(8 + (i % 10), (i * 13) % 60);
                LocalDate dob = LocalDate.of(1982 + (i % 22), 1 + (i % 12), 1 + (i % 27));

                users.setString(1, "demo_c" + pad(i));
                users.setString(2, hash);
                users.setString(3, fullName);
                users.setString(4, status);
                users.setTimestamp(5, Timestamp.valueOf(created));
                users.setTimestamp(6, Timestamp.valueOf(created.plusHours(3)));
                users.executeUpdate();
                long userId = generatedId(users);

                customers.setLong(1, userId);
                customers.setString(2, fullName);
                customers.setString(3, "demo.c" + pad(i) + "@bikevault.demo");
                customers.setString(4, String.valueOf(9800000000L + i));
                customers.setString(5, ((12 + i) % 88 + 1) + " " + city[0].substring(city[0].indexOf(' ') + 1));
                customers.setString(6, city[1] + "14" + String.format("%07d", 210000 + i));
                customers.setObject(7, dob);
                customers.setString(8, gender);
                customers.setString(9, status);
                customers.setObject(10, registered);
                customers.setTimestamp(11, Timestamp.valueOf(created));
                customers.setTimestamp(12, Timestamp.valueOf(created.plusHours(3)));
                customers.executeUpdate();
                ids.add(generatedId(customers));

                audit(connection, userId, "REGISTER",
                        fullName + " created a customer account from the desk / register screen", created.plusMinutes(4));
                if (i % 4 == 0) {
                    audit(connection, userId, "CUSTOMER_LOGIN",
                            fullName + " signed in to browse bikes", created.plusDays(2).plusHours(1));
                }
            }
        }
        return ids;
    }

    private static int insertCompletedHistory(Connection connection, List<Long> customers, List<BikeRow> bikes,
                                              long adminId, long staffId) throws SQLException {
        int count = 0;
        LocalDate today = LocalDate.now();
        for (int n = 0; n < 260; n++) {
            int customerIndex = customerForHistory(n, customers.size());
            BikeRow bike = bikes.get(n % bikes.size());
            long customerId = customers.get(customerIndex);
            long deskUser = n % 3 == 0 ? staffId : adminId;
            int days = 1 + (n % 6);
            int lateDays = n % 8 == 0 ? 1 + (n % 3) : 0;
            LocalDate start = today.minusDays(18 + (n * 2L));
            if (start.isBefore(LocalDate.of(2024, 2, 1))) {
                start = LocalDate.of(2024, 2, 1).plusDays(n % 40);
            }
            LocalDate expected = start.plusDays(days);
            LocalDate actual = expected.plusDays(lateDays);
            BigDecimal total = bike.dailyRate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lateFee = bike.lateFee.multiply(BigDecimal.valueOf(lateDays)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalAmount = total.add(lateFee);
            LocalDateTime created = start.atTime(9 + (n % 8), (n * 7) % 60);
            String token = token();

            long rentalId = insertRental(connection, customerId, bike.id, deskUser, start, expected, actual,
                    days, bike.dailyRate, total, lateFee, finalAmount, "COMPLETED",
                    lateDays > 0 ? "Returned late — late fee applied at desk" : "Closed trip, papers filed",
                    token, "COMPLETED", adminId, start.atTime(10, 15),
                    0, null, created);

            insertReturn(connection, rentalId, bike.id, customerId, deskUser, actual,
                    days + lateDays, lateDays, lateFee, finalAmount, RETURN_NOTES[n % RETURN_NOTES.length],
                    actual.atTime(17, 10));

            String method = METHODS[n % METHODS.length];
            insertPayment(connection, rentalId, deskUser, finalAmount, method, "COMPLETED",
                    "PAY-HX-" + String.format("%05d", n + 1),
                    method + " settlement for completed rental",
                    start.atTime(9, 40));
            if (n % 17 == 0) {
                insertPayment(connection, rentalId, deskUser, new BigDecimal("100.00"), "UPI", "FAILED",
                        "PAY-FAIL-" + String.format("%05d", n + 1),
                        "First UPI attempt failed, retried at desk", start.atTime(9, 32));
            }

            if (lateDays > 0 || n % 14 == 0) {
                String type = lateDays > 0 ? "LATE_RETURN" : (n % 2 == 0 ? "DAMAGE" : "TRAFFIC_CHALLAN");
                String pStatus = n % 5 == 0 ? "PENDING" : (n % 9 == 0 ? "WAIVED" : "PAID");
                String desc = switch (type) {
                    case "LATE_RETURN" -> "Returned " + lateDays + " day(s) after the booked slot";
                    case "DAMAGE" -> "Cosmetic scuff recorded during return inspection";
                    default -> "Traffic challan attached to this rental by the city portal";
                };
                BigDecimal amount = type.equals("LATE_RETURN") ? lateFee.max(new BigDecimal("250.00"))
                        : new BigDecimal(type.equals("DAMAGE") ? "1500.00" : "1000.00");
                LocalDateTime when = actual.atTime(18, 5);
                long penaltyId = insertPenalty(connection, rentalId, customerId, bike.id, type, desc, amount, pStatus, when);
                if ("PAID".equals(pStatus)) {
                    insertPayment(connection, rentalId, deskUser, amount, METHODS[(n + 1) % METHODS.length], "COMPLETED",
                            "PAY-PEN-" + String.format("%05d", n + 1),
                            "PENALTY PAYMENT #" + penaltyId + " · " + type, when.plusHours(2));
                    audit(connection, customerUser(connection, customerId), "PENALTY_PAYMENT_COMPLETED",
                            "Penalty #" + penaltyId + " paid for rental #" + rentalId, when.plusHours(2));
                } else if ("PENDING".equals(pStatus)) {
                    audit(connection, deskUser, "PENALTY_CREATED",
                            "Penalty #" + penaltyId + " opened for rental #" + rentalId, when);
                }
            }

            if (n % 19 == 0) {
                insertIncident(connection, customerId, rentalId, bike.id,
                        n % 2 == 0 ? "MECHANICAL_ISSUE" : "BIKE_DAMAGE",
                        "Customer called the desk during the trip",
                        start.plusDays(1), "City loop", "Logged from the shop phone",
                        n % 3 == 0 ? "RESOLVED" : "UNDER_REVIEW", start.plusDays(1).atTime(15, 20));
            }
            if (n % 21 == 0) {
                insertChallan(connection, rentalId, customerId, bike.id, start.plusDays(1),
                        "Signal / helmet challan forwarded by the customer",
                        new BigDecimal("1000.00"), n % 2 == 0 ? "PAID" : "PENDING",
                        start.plusDays(1).atTime(16, 0));
            }

            audit(connection, customerUser(connection, customerId), "RENTAL_REQUESTED",
                    "Rental #" + rentalId + " requested for bike #" + bike.id, created);
            audit(connection, deskUser, "PAYMENT_COMPLETED",
                    "Payment captured for rental #" + rentalId, created.plusMinutes(12));
            audit(connection, adminId, "RENTAL_APPROVED",
                    "Admin approved rental #" + rentalId, created.plusMinutes(20));
            audit(connection, deskUser, "BIKE_COLLECTED",
                    "Customer collected bike #" + bike.id, start.atTime(10, 30));
            audit(connection, deskUser, "RETURN_ACCEPTED",
                    "Return accepted for rental #" + rentalId, actual.atTime(17, 12));
            count++;
        }
        return count;
    }

    private static int insertOpenWork(Connection connection, List<Long> customers, List<BikeRow> bikes,
                                      List<Long> busyBikes, List<Long> busyCustomers,
                                      long adminId, long staffId) throws SQLException {
        List<BikeRow> freeBikes = bikes.stream()
                .filter(bike -> !busyBikes.contains(bike.id) && !"MAINTENANCE".equals(bike.status))
                .toList();
        List<Long> freeCustomers = customers.stream()
                .filter(id -> !busyCustomers.contains(id))
                .toList();
        int usedBike = 0;
        int usedCustomer = 0;
        int created = 0;
        LocalDate today = LocalDate.now();

        created += openBatch(connection, "ACTIVE", 12, freeBikes, freeCustomers, usedBike, usedCustomer,
                today.minusDays(2), 4, adminId, staffId, true);
        usedBike += 12;
        usedCustomer += 12;
        created += openBatch(connection, "RETURN_REQUESTED", 5, freeBikes, freeCustomers, usedBike, usedCustomer,
                today.minusDays(5), 3, adminId, staffId, true);
        usedBike += 5;
        usedCustomer += 5;
        created += openBatch(connection, "PENDING_APPROVAL", 6, freeBikes, freeCustomers, usedBike, usedCustomer,
                today, 2, adminId, staffId, false);
        usedBike += 6;
        usedCustomer += 6;

        for (int i = 0; i < 8 && usedCustomer + i < freeCustomers.size(); i++) {
            BikeRow bike = freeBikes.get((usedBike + i) % freeBikes.size());
            long customerId = freeCustomers.get(usedCustomer + i);
            LocalDate start = today.minusDays(12 + i);
            BigDecimal total = bike.dailyRate.multiply(BigDecimal.valueOf(2));
            LocalDateTime createdAt = start.atTime(11, 20);
            String status = i < 5 ? "REJECTED" : "CANCELLED";
            long rentalId = insertRental(connection, customerId, bike.id, staffId, start, start.plusDays(2),
                    null, 2, bike.dailyRate, total, BigDecimal.ZERO, total, status,
                    status.equals("REJECTED") ? REJECT_REASONS[i % REJECT_REASONS.length] : "Customer called to cancel",
                    token(), status.equals("REJECTED") ? "REJECTED" : "EXPIRED",
                    status.equals("REJECTED") ? adminId : null,
                    status.equals("REJECTED") ? start.atTime(12, 0) : null,
                    0, null, createdAt);
            insertPayment(connection, rentalId, staffId, total, "UPI",
                    status.equals("CANCELLED") ? "REFUNDED" : "COMPLETED",
                    "PAY-OPEN-" + String.format("%03d", i + 1),
                    status.equals("CANCELLED") ? "Refund after cancellation" : "Paid then rejected at approval",
                    createdAt.plusMinutes(8));
            audit(connection, adminId, status.equals("REJECTED") ? "RENTAL_REJECTED" : "CREATE_RENTAL",
                    "Rental #" + rentalId + " marked " + status, createdAt.plusHours(1));
            created++;
        }
        return created;
    }

    private static int openBatch(Connection connection, String status, int howMany, List<BikeRow> bikes,
                                 List<Long> customers, int bikeOffset, int customerOffset,
                                 LocalDate start, int days, long adminId, long staffId, boolean approved)
            throws SQLException {
        int created = 0;
        for (int i = 0; i < howMany; i++) {
            if (bikeOffset + i >= bikes.size() || customerOffset + i >= customers.size()) {
                break;
            }
            BikeRow bike = bikes.get(bikeOffset + i);
            long customerId = customers.get(customerOffset + i);
            long deskUser = i % 2 == 0 ? adminId : staffId;
            BigDecimal total = bike.dailyRate.multiply(BigDecimal.valueOf(days));
            LocalDateTime createdAt = start.atTime(10, 5 + i);
            boolean returnRequested = "RETURN_REQUESTED".equals(status);
            String qrStatus = "PENDING_APPROVAL".equals(status) ? "PENDING_APPROVAL" : "ACTIVE";
            long rentalId = insertRental(connection, customerId, bike.id, deskUser, start, start.plusDays(days),
                    null, days, bike.dailyRate, total, BigDecimal.ZERO, total, status,
                    switch (status) {
                        case "ACTIVE" -> "Collected from the shop floor";
                        case "RETURN_REQUESTED" -> "Customer asked to close the trip";
                        default -> "Waiting for admin QR approval";
                    },
                    token(), qrStatus, approved ? adminId : null, approved ? start.atTime(10, 40) : null,
                    returnRequested ? 1 : 0, returnRequested ? start.plusDays(days - 1).atTime(16, 30) : null,
                    createdAt);
            insertPayment(connection, rentalId, deskUser, total, METHODS[i % METHODS.length], "COMPLETED",
                    "PAY-LIVE-" + status.charAt(0) + String.format("%02d", i + 1),
                    "Prepaid rental " + status.toLowerCase().replace('_', ' '), createdAt.plusMinutes(6));
            audit(connection, customerUser(connection, customerId), "RENTAL_REQUESTED",
                    "Rental #" + rentalId + " is " + status, createdAt);
            if (approved) {
                audit(connection, adminId, "RENTAL_APPROVED", "Approved rental #" + rentalId, createdAt.plusMinutes(25));
                audit(connection, deskUser, "BIKE_COLLECTED", "Handed over bike #" + bike.id, start.atTime(11, 0));
            }
            if (returnRequested) {
                audit(connection, customerUser(connection, customerId), "RETURN_REQUESTED",
                        "Customer requested return for rental #" + rentalId, start.plusDays(days - 1).atTime(16, 31));
            }
            created++;
        }
        return created;
    }

    private static void enrichExistingCustomers(Connection connection, List<BikeRow> bikes, long adminId, long staffId)
            throws SQLException {
        List<Long> existing = queryIds(connection, """
                SELECT customer_id FROM customers
                 WHERE email NOT LIKE '%@bikevault.demo'
                 ORDER BY customer_id
                """);
        LocalDate today = LocalDate.now();
        int n = 0;
        for (Long customerId : existing) {
            for (int trip = 0; trip < 3; trip++) {
                BikeRow bike = bikes.get((n + trip) % bikes.size());
                int days = 2 + trip;
                LocalDate start = today.minusDays(40L + n * 11L + trip * 8L);
                LocalDate end = start.plusDays(days);
                BigDecimal total = bike.dailyRate.multiply(BigDecimal.valueOf(days));
                LocalDateTime created = start.atTime(9, 50);
                long rentalId = insertRental(connection, customerId, bike.id, adminId, start, end, end,
                        days, bike.dailyRate, total, BigDecimal.ZERO, total, "COMPLETED",
                        "Repeat customer — closed on time", token(), "COMPLETED", adminId,
                        start.atTime(10, 10), 0, null, created);
                insertReturn(connection, rentalId, bike.id, customerId, staffId, end, days, 0, BigDecimal.ZERO, total,
                        "Repeat customer, no remarks", end.atTime(16, 45));
                insertPayment(connection, rentalId, adminId, total, METHODS[(n + trip) % METHODS.length], "COMPLETED",
                        "PAY-OLD-" + customerId + "-" + trip, "Repeat-customer settlement", created.plusMinutes(15));
            }
            n++;
        }
        audit(connection, adminId, "ADMIN_LOGIN", "Administrator reviewed the morning board",
                LocalDateTime.now().minusHours(5));
        audit(connection, staffId, "STAFF_LOGIN", "Front desk opened the shop",
                LocalDateTime.now().minusHours(6));
    }

    private static void backdateFreshRows(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE bikes
                       SET created_at = TIMESTAMP(COALESCE(purchase_date, '2024-06-01'), '10:18:00'),
                           updated_at = TIMESTAMP(COALESCE(purchase_date, '2024-06-01'), '10:18:00')
                     WHERE created_at >= DATE_SUB(NOW(), INTERVAL 3 DAY)
                    """);
            statement.executeUpdate("""
                    UPDATE users
                       SET created_at = '2024-01-05 08:40:00', updated_at = '2025-11-12 09:05:00'
                     WHERE username IN ('admin', 'staff')
                    """);
        }
    }

    private static void syncBikeStatuses(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE bikes b
                       SET status = 'RENTED'
                     WHERE EXISTS (
                        SELECT 1 FROM rentals r
                         WHERE r.bike_id = b.bike_id
                           AND r.status IN ('ACTIVE', 'RETURN_REQUESTED'))
                    """);
            statement.executeUpdate("""
                    UPDATE bikes b
                       SET status = 'AVAILABLE'
                     WHERE b.status = 'RENTED'
                       AND NOT EXISTS (
                        SELECT 1 FROM rentals r
                         WHERE r.bike_id = b.bike_id
                           AND r.status IN ('ACTIVE', 'RETURN_REQUESTED'))
                    """);
        }
        markMaintenance(connection);
    }

    private static void markMaintenance(Connection connection) throws SQLException {
        List<Long> candidates = queryIds(connection, """
                SELECT bike_id FROM bikes
                 WHERE status = 'AVAILABLE'
                 ORDER BY bike_id DESC
                 LIMIT 2
                """);
        if (candidates.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bikes SET status = 'MAINTENANCE', updated_at = ? WHERE bike_id = ?")) {
            for (Long bikeId : candidates) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(4)));
                statement.setLong(2, bikeId);
                statement.executeUpdate();
            }
        }
    }

    private static long insertRental(Connection connection, long customerId, long bikeId, long userId,
                                     LocalDate start, LocalDate expected, LocalDate actual, int days,
                                     BigDecimal dailyRate, BigDecimal total, BigDecimal lateFee, BigDecimal finalAmount,
                                     String status, String notes, String token, String qrStatus,
                                     Long approvedBy, LocalDateTime approvedAt, int returnRequested,
                                     LocalDateTime returnRequestedAt, LocalDateTime created) throws SQLException {
        String sql = """
                INSERT INTO rentals (
                    customer_id, bike_id, user_id, start_date, expected_return_date, actual_return_date,
                    duration_days, daily_rate, total_amount, late_fee, security_deposit, final_amount,
                    status, notes, rental_qr_token, rental_qr_status, approved_by, approved_at,
                    rejection_reason, rejection_notes, return_requested, return_requested_at,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0.00, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setLong(2, bikeId);
            statement.setLong(3, userId);
            statement.setObject(4, start);
            statement.setObject(5, expected);
            statement.setObject(6, actual);
            statement.setInt(7, days);
            statement.setBigDecimal(8, dailyRate);
            statement.setBigDecimal(9, total);
            statement.setBigDecimal(10, lateFee);
            statement.setBigDecimal(11, finalAmount);
            statement.setString(12, status);
            statement.setString(13, notes);
            statement.setString(14, token);
            statement.setString(15, qrStatus);
            if (approvedBy == null) {
                statement.setObject(16, null);
                statement.setTimestamp(17, null);
            } else {
                statement.setLong(16, approvedBy);
                statement.setTimestamp(17, Timestamp.valueOf(approvedAt));
            }
            if ("REJECTED".equals(status)) {
                statement.setString(18, "DOCS");
                statement.setString(19, notes);
            } else {
                statement.setObject(18, null);
                statement.setObject(19, null);
            }
            statement.setInt(20, returnRequested);
            statement.setTimestamp(21, returnRequestedAt == null ? null : Timestamp.valueOf(returnRequestedAt));
            statement.setTimestamp(22, Timestamp.valueOf(created));
            statement.setTimestamp(23, Timestamp.valueOf(created.plusHours(2)));
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static void insertReturn(Connection connection, long rentalId, long bikeId, long customerId, long userId,
                                     LocalDate returnDate, int actualDays, int lateDays, BigDecimal lateFee,
                                     BigDecimal finalAmount, String notes, LocalDateTime created) throws SQLException {
        String sql = """
                INSERT INTO returns (
                    rental_id, bike_id, customer_id, user_id, return_date,
                    actual_duration_days, late_days, late_fee, final_amount, condition_notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            statement.setLong(2, bikeId);
            statement.setLong(3, customerId);
            statement.setLong(4, userId);
            statement.setObject(5, returnDate);
            statement.setInt(6, Math.max(actualDays, 1));
            statement.setInt(7, lateDays);
            statement.setBigDecimal(8, lateFee);
            statement.setBigDecimal(9, finalAmount);
            statement.setString(10, notes);
            statement.setTimestamp(11, Timestamp.valueOf(created));
            statement.executeUpdate();
        }
    }

    private static void insertPayment(Connection connection, long rentalId, long userId, BigDecimal amount,
                                      String method, String status, String reference, String notes,
                                      LocalDateTime paidAt) throws SQLException {
        String sql = """
                INSERT INTO payments (rental_id, user_id, amount, method, status, reference_number, notes, paid_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            statement.setLong(2, userId);
            statement.setBigDecimal(3, amount.max(new BigDecimal("1.00")));
            statement.setString(4, method);
            statement.setString(5, status);
            statement.setString(6, reference);
            statement.setString(7, notes);
            statement.setTimestamp(8, Timestamp.valueOf(paidAt));
            statement.setTimestamp(9, Timestamp.valueOf(paidAt));
            statement.executeUpdate();
        }
    }

    private static long insertPenalty(Connection connection, long rentalId, long customerId, long bikeId,
                                      String type, String description, BigDecimal amount, String status,
                                      LocalDateTime created) throws SQLException {
        String sql = """
                INSERT INTO penalties (rental_id, customer_id, bike_id, penalty_type, description, amount, status,
                                       created_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, rentalId);
            statement.setLong(2, customerId);
            statement.setLong(3, bikeId);
            statement.setString(4, type);
            statement.setString(5, description);
            statement.setBigDecimal(6, amount);
            statement.setString(7, status);
            statement.setTimestamp(8, Timestamp.valueOf(created));
            statement.setTimestamp(9, "PENDING".equals(status) ? null : Timestamp.valueOf(created.plusDays(1)));
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static void insertIncident(Connection connection, long customerId, long rentalId, long bikeId,
                                       String type, String description, LocalDate date, String location,
                                       String details, String status, LocalDateTime created) throws SQLException {
        String sql = """
                INSERT INTO incidents (customer_id, rental_id, bike_id, incident_type, description, incident_date,
                                       location, details, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setLong(2, rentalId);
            statement.setLong(3, bikeId);
            statement.setString(4, type);
            statement.setString(5, description);
            statement.setObject(6, date);
            statement.setString(7, location);
            statement.setString(8, details);
            statement.setString(9, status);
            statement.setTimestamp(10, Timestamp.valueOf(created));
            statement.setTimestamp(11, Timestamp.valueOf(created.plusHours(6)));
            statement.executeUpdate();
        }
    }

    private static void insertChallan(Connection connection, long rentalId, long customerId, long bikeId,
                                      LocalDate date, String description, BigDecimal amount, String status,
                                      LocalDateTime created) throws SQLException {
        String sql = """
                INSERT INTO challans (rental_id, customer_id, bike_id, challan_date, description, amount, status,
                                      created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, rentalId);
            statement.setLong(2, customerId);
            statement.setLong(3, bikeId);
            statement.setObject(4, date);
            statement.setString(5, description);
            statement.setBigDecimal(6, amount);
            statement.setString(7, status);
            statement.setTimestamp(8, Timestamp.valueOf(created));
            statement.setTimestamp(9, Timestamp.valueOf(created));
            statement.executeUpdate();
        }
    }

    private static void audit(Connection connection, Long userId, String action, String description,
                              LocalDateTime when) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, description, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setObject(1, null);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, action);
            statement.setString(3, description);
            statement.setTimestamp(4, Timestamp.valueOf(when));
            statement.executeUpdate();
        }
    }

    private static List<BikeRow> loadBikes(Connection connection) throws SQLException {
        String sql = """
                SELECT b.bike_id, b.daily_rate, b.status, COALESCE(c.late_fee_per_day, 200.00)
                  FROM bikes b
                  LEFT JOIN bike_categories c ON c.category_id = b.category_id
                 ORDER BY b.bike_id
                """;
        List<BikeRow> bikes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                bikes.add(new BikeRow(
                        resultSet.getLong(1),
                        resultSet.getBigDecimal(2),
                        resultSet.getString(3),
                        resultSet.getBigDecimal(4)));
            }
        }
        return bikes;
    }

    private static Long customerUser(Connection connection, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id FROM customers WHERE customer_id = ?")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long userId = resultSet.getLong(1);
                    return resultSet.wasNull() ? null : userId;
                }
            }
        }
        return null;
    }

    private static int customerForHistory(int n, int size) {
        int usable = Math.max(size - 15, 20);
        if (n % 10 <= 2) {
            return n % Math.min(20, usable);
        }
        if (n % 10 <= 6) {
            return 20 + (n % Math.max(usable - 20, 1));
        }
        return Math.min(usable - 1, 40 + (n % Math.max(usable - 40, 1)));
    }

    private static String genderFor(String first, int i) {
        if (i % 41 == 0) {
            return "OTHER";
        }
        int index = 0;
        for (int n = 0; n < FIRST.length; n++) {
            if (FIRST[n].equals(first)) {
                index = n;
                break;
            }
        }
        return index >= 25 ? "FEMALE" : "MALE";
    }

    private static boolean exists(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    private static long scalarLong(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("Missing required row for: " + sql);
            }
            return resultSet.getLong(1);
        }
    }

    private static List<Long> queryIds(Connection connection, String sql) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getLong(1));
            }
        }
        return ids;
    }

    private static long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("No generated key");
            }
            return keys.getLong(1);
        }
    }

    private static String token() {
        return "RVQR-" + UUID.randomUUID().toString().toUpperCase();
    }

    private static String pad(int value) {
        return String.format("%03d", value);
    }

    private record BikeRow(long id, BigDecimal dailyRate, String status, BigDecimal lateFee) {
    }
}
