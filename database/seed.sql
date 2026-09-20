-- BIKEVAULT demonstration data.
-- Safe to run once against an empty schema. Passwords are PBKDF2 hashes of admin123 / staff123 / hassan123.

USE bikevault;

INSERT INTO users (username, password_hash, full_name, role, status) VALUES
    ('admin', 'pbkdf2$65536$3C1+GqGPS16wHhNj5+ZIkA==$v0yXKxAhSQa9+VXzMqQM55o28VAhUqIyqppeCh2l/TE=', 'System Administrator', 'ADMIN', 'ACTIVE'),
    ('staff', 'pbkdf2$65536$4TytBEy3B/ELvgWbZp7TQg==$rz8hhsNaqtzxcPo9i16JfiCiVkYwE4i1nNecsJSbIgU=', 'Front Desk Staff', 'STAFF', 'ACTIVE'),
    ('hassan', 'pbkdf2$65536$su2A/eWjPz2oNVQJzd6nWQ==$lVMbUmcd0JpijmhgFSoii041jRWA033sGZhNbQvlRrM=', 'Hassan Khan', 'CUSTOMER', 'ACTIVE');

INSERT INTO bike_categories (name, description, late_fee_per_day, status) VALUES
    ('Sport', 'Performance motorcycles for short urban and highway rentals', 350.00, 'ACTIVE'),
    ('Commuter', 'Lightweight daily-use scooters and commuter bikes', 150.00, 'ACTIVE'),
    ('Cruiser', 'Comfort-oriented touring and cruiser motorcycles', 250.00, 'ACTIVE'),
    ('Electric', 'Zero-emission electric scooters and motorcycles', 200.00, 'ACTIVE');

INSERT INTO customers (
    user_id, full_name, email, phone, address, driving_license_number,
    date_of_birth, gender, status, registration_date
) VALUES
    (NULL, 'Aditya Rao', 'aditya.rao@example.com', '9876543210', '12 MG Road, Bengaluru', 'KA0120210001234', '1996-04-18', 'MALE', 'ACTIVE', '2026-01-12'),
    (NULL, 'Meera Nair', 'meera.nair@example.com', '9812345678', '44 Marine Drive, Kochi', 'KL0720190045678', '1994-11-02', 'FEMALE', 'ACTIVE', '2026-02-03'),
    (NULL, 'Rahul Verma', 'rahul.verma@example.com', '9900112233', '7 Civil Lines, Jaipur', 'RJ1420200098765', '1990-07-21', 'MALE', 'ACTIVE', '2026-03-15'),
    (NULL, 'Sana Sheikh', 'sana.sheikh@example.com', '9765432109', '21 Banjara Hills, Hyderabad', 'TS0920220011223', '1998-01-09', 'FEMALE', 'ACTIVE', '2026-04-08'),
    (3, 'Hassan Khan', 'hassan.khan@example.com', '9000011122', '18 Brigade Road, Bengaluru', 'KA0120240099001', '1997-08-14', 'MALE', 'ACTIVE', '2026-05-01');

INSERT INTO bikes (
    qr_code, registration_number, model, brand, category_id, color,
    daily_rate, status, purchase_date, description
) VALUES
    ('BIKE-QR-A83F92D1', 'KA-01-AB-1234', 'MT-15', 'Yamaha', 1, 'Metallic Black', 1200.00, 'AVAILABLE', '2025-06-01', 'A2-friendly sport naked with ABS'),
    ('BIKE-QR-C12E44B0', 'KA-03-CD-7788', 'Activa 6G', 'Honda', 2, 'Pearl White', 450.00, 'RENTED', '2024-11-20', 'City commuter scooter'),
    ('BIKE-QR-7F92A81C', 'KA-05-EF-4411', 'Classic 350', 'Royal Enfield', 3, 'Stealth Black', 1500.00, 'AVAILABLE', '2025-01-15', 'Cruiser with dual-channel ABS'),
    ('BIKE-QR-E91B03AA', 'KA-02-GH-9900', '450X', 'Ather', 4, 'Space Grey', 900.00, 'AVAILABLE', '2025-08-10', 'Connected electric scooter'),
    ('BIKE-QR-55D0C4E2', 'KA-04-IJ-3344', 'Duke 390', 'KTM', 1, 'Orange', 1800.00, 'MAINTENANCE', '2024-09-05', 'Awaiting chain and brake service'),
    ('BIKE-550E8400-E29B-41D4-A716-446655440000', 'KA-01-KL-2211', 'Apache RTR 160', 'TVS', 1, 'Racing Red', 800.00, 'AVAILABLE', '2023-12-12', 'Legacy UUID-style QR identity');

INSERT INTO rentals (
    customer_id, bike_id, user_id, start_date, expected_return_date, actual_return_date,
    duration_days, daily_rate, total_amount, late_fee, final_amount, status, notes
) VALUES
    (2, 2, 2, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), NULL,
     3, 450.00, 1350.00, 0.00, 1350.00, 'ACTIVE', 'Seed active rental for Honda Activa'),
    (1, 3, 1, DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY),
     3, 1500.00, 4500.00, 0.00, 4500.00, 'COMPLETED', 'Returned on time');

INSERT INTO returns (
    rental_id, bike_id, customer_id, user_id, return_date,
    actual_duration_days, late_days, late_fee, final_amount, condition_notes
) VALUES
    (2, 3, 1, 1, DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), 3, 0, 0.00, 4500.00, 'No damage');

INSERT INTO payments (
    rental_id, user_id, amount, method, status, reference_number, notes
) VALUES
    (1, 2, 1350.00, 'UPI', 'COMPLETED', 'PAY-SEED-ACTIVA-001', 'Advance payment for active rental'),
    (2, 1, 4500.00, 'CARD', 'COMPLETED', 'PAY-SEED-RE-001', 'Settlement for completed cruiser rental');

INSERT INTO audit_logs (user_id, action, description) VALUES
    (1, 'LOGIN', 'Seed administrator account provisioned'),
    (1, 'ADD_BIKE', 'Demonstration fleet loaded from seed.sql'),
    (2, 'CREATE_RENTAL', 'Seed active rental created for Honda Activa 6G'),
    (3, 'REGISTER', 'Demo customer Hassan provisioned');

INSERT INTO penalties (rental_id, customer_id, bike_id, penalty_type, description, amount, status)
VALUES (2, 1, 3, 'LATE_RETURN', 'Demonstration late-return charge on a completed rental', 500.00, 'PENDING');

INSERT INTO incidents (customer_id, rental_id, bike_id, incident_type, description, incident_date, location, details, status)
VALUES (2, 1, 2, 'MECHANICAL_ISSUE', 'Customer reported a brake squeal during the active rental', CURRENT_DATE, 'MG Road', 'Optional reference: seed incident', 'REPORTED');

INSERT INTO challans (rental_id, customer_id, bike_id, challan_date, description, amount, status)
VALUES (1, 2, 2, CURRENT_DATE, 'Signal jump while riding the rented Activa', 1000.00, 'PENDING');
