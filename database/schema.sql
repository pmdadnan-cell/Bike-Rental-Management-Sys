-- BIKEVAULT schema
-- Normalized MySQL 8+ catalog for premium bike rental management.

CREATE DATABASE IF NOT EXISTS bikevault
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bikevault;

CREATE TABLE IF NOT EXISTS users (
    user_id         BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(120)    NOT NULL,
    role            ENUM('ADMIN', 'STAFF', 'CUSTOMER') NOT NULL,
    status          ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username),
    KEY idx_users_role_status (role, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customers (
    customer_id             BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NULL,
    full_name               VARCHAR(120)    NOT NULL,
    email                   VARCHAR(120)    NOT NULL,
    phone                   VARCHAR(20)     NOT NULL,
    address                 VARCHAR(255)    NOT NULL,
    driving_license_number  VARCHAR(50)     NOT NULL,
    date_of_birth           DATE            NOT NULL,
    gender                  ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
    status                  ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    registration_date       DATE            NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uk_customers_email (email),
    UNIQUE KEY uk_customers_phone (phone),
    UNIQUE KEY uk_customers_license (driving_license_number),
    UNIQUE KEY uk_customers_user (user_id),
    KEY idx_customers_name (full_name),
    KEY idx_customers_status (status),
    CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bike_categories (
    category_id         BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(80)     NOT NULL,
    description         VARCHAR(255)    NULL,
    late_fee_per_day    DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    status              ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_categories_name (name),
    CONSTRAINT chk_categories_late_fee CHECK (late_fee_per_day >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bikes (
    bike_id                 BIGINT          NOT NULL AUTO_INCREMENT,
    qr_code                 VARCHAR(80)     NOT NULL,
    registration_number     VARCHAR(40)     NOT NULL,
    model                   VARCHAR(80)     NOT NULL,
    brand                   VARCHAR(80)     NOT NULL,
    category_id             BIGINT          NOT NULL,
    color                   VARCHAR(40)     NOT NULL,
    daily_rate              DECIMAL(10,2)   NOT NULL,
    status                  ENUM('AVAILABLE', 'RENTED', 'MAINTENANCE', 'INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
    purchase_date           DATE            NULL,
    description             TEXT            NULL,
    image_path              VARCHAR(500)    NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bike_id),
    UNIQUE KEY uk_bikes_qr_code (qr_code),
    UNIQUE KEY uk_bikes_registration (registration_number),
    KEY idx_bikes_status (status),
    KEY idx_bikes_category (category_id),
    KEY idx_bikes_brand_model (brand, model),
    CONSTRAINT fk_bikes_category
        FOREIGN KEY (category_id) REFERENCES bike_categories (category_id),
    CONSTRAINT chk_bikes_daily_rate CHECK (daily_rate > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rentals (
    rental_id               BIGINT          NOT NULL AUTO_INCREMENT,
    customer_id             BIGINT          NOT NULL,
    bike_id                 BIGINT          NOT NULL,
    user_id                 BIGINT          NOT NULL,
    start_date              DATE            NOT NULL,
    expected_return_date    DATE            NOT NULL,
    actual_return_date      DATE            NULL,
    duration_days           INT             NOT NULL,
    daily_rate              DECIMAL(10,2)   NOT NULL,
    total_amount            DECIMAL(10,2)   NOT NULL,
    late_fee                DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    security_deposit        DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    final_amount            DECIMAL(10,2)   NOT NULL,
    status                  ENUM('PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED', 'COMPLETED', 'CANCELLED', 'REJECTED')
                            NOT NULL DEFAULT 'PENDING_APPROVAL',
    rental_qr_token         VARCHAR(80)     NULL,
    rental_qr_status        ENUM('PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'COMPLETED', 'REJECTED', 'EXPIRED')
                            NULL,
    approved_by             BIGINT          NULL,
    approved_at             TIMESTAMP       NULL,
    rejection_reason        VARCHAR(80)     NULL,
    rejection_notes         VARCHAR(500)    NULL,
    return_requested        TINYINT(1)      NOT NULL DEFAULT 0,
    return_requested_at     TIMESTAMP       NULL,
    notes                   VARCHAR(255)    NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_bike_key         BIGINT          GENERATED ALWAYS AS (IF(status IN ('ACTIVE', 'RETURN_REQUESTED'), bike_id, NULL)) STORED,
    active_customer_key     BIGINT          GENERATED ALWAYS AS (IF(status IN ('ACTIVE', 'RETURN_REQUESTED'), customer_id, NULL)) STORED,
    PRIMARY KEY (rental_id),
    UNIQUE KEY uk_rentals_one_active_bike (active_bike_key),
    UNIQUE KEY uk_rentals_one_active_customer (active_customer_key),
    UNIQUE KEY uk_rentals_qr_token (rental_qr_token),
    KEY idx_rentals_customer (customer_id),
    KEY idx_rentals_bike (bike_id),
    KEY idx_rentals_status (status),
    KEY idx_rentals_dates (start_date, expected_return_date),
    CONSTRAINT fk_rentals_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_rentals_bike
        FOREIGN KEY (bike_id) REFERENCES bikes (bike_id),
    CONSTRAINT fk_rentals_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_rentals_approved_by
        FOREIGN KEY (approved_by) REFERENCES users (user_id),
    CONSTRAINT chk_rentals_dates CHECK (expected_return_date >= start_date),
    CONSTRAINT chk_rentals_duration CHECK (duration_days > 0),
    CONSTRAINT chk_rentals_daily_rate CHECK (daily_rate > 0),
    CONSTRAINT chk_rentals_total CHECK (total_amount >= 0),
    CONSTRAINT chk_rentals_late_fee CHECK (late_fee >= 0),
    CONSTRAINT chk_rentals_final CHECK (final_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS returns (
    return_id               BIGINT          NOT NULL AUTO_INCREMENT,
    rental_id               BIGINT          NOT NULL,
    bike_id                 BIGINT          NOT NULL,
    customer_id             BIGINT          NOT NULL,
    user_id                 BIGINT          NOT NULL,
    return_date             DATE            NOT NULL,
    actual_duration_days    INT             NOT NULL,
    late_days               INT             NOT NULL DEFAULT 0,
    late_fee                DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    final_amount            DECIMAL(10,2)   NOT NULL,
    condition_notes         VARCHAR(255)    NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (return_id),
    UNIQUE KEY uk_returns_rental (rental_id),
    KEY idx_returns_bike (bike_id),
    KEY idx_returns_date (return_date),
    CONSTRAINT fk_returns_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_returns_bike
        FOREIGN KEY (bike_id) REFERENCES bikes (bike_id),
    CONSTRAINT fk_returns_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_returns_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_returns_duration CHECK (actual_duration_days > 0),
    CONSTRAINT chk_returns_late_days CHECK (late_days >= 0),
    CONSTRAINT chk_returns_late_fee CHECK (late_fee >= 0),
    CONSTRAINT chk_returns_final CHECK (final_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    payment_id          BIGINT          NOT NULL AUTO_INCREMENT,
    rental_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    amount              DECIMAL(10,2)   NOT NULL,
    method              ENUM('CASH', 'CARD', 'UPI', 'NET_BANKING') NOT NULL,
    status              ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'COMPLETED',
    reference_number    VARCHAR(80)     NULL,
    paid_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes               VARCHAR(255)    NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payments_reference (reference_number),
    KEY idx_payments_rental (rental_id),
    KEY idx_payments_status_paid (status, paid_at),
    CONSTRAINT fk_payments_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_payments_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    log_id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NULL,
    action          VARCHAR(50)     NOT NULL,
    description     VARCHAR(500)    NOT NULL,
    timestamp       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_audit_user (user_id),
    KEY idx_audit_action (action),
    KEY idx_audit_timestamp (timestamp),
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS penalties (
    penalty_id      BIGINT          NOT NULL AUTO_INCREMENT,
    rental_id       BIGINT          NULL,
    customer_id     BIGINT          NOT NULL,
    bike_id         BIGINT          NULL,
    penalty_type    ENUM('LATE_RETURN', 'DAMAGE', 'ACCIDENT', 'TRAFFIC_CHALLAN', 'MISSING_DOCUMENTS', 'OTHER') NOT NULL,
    description     VARCHAR(500)    NOT NULL,
    amount          DECIMAL(10,2)   NOT NULL,
    status          ENUM('PENDING', 'PAID', 'WAIVED', 'DISPUTED') NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP       NULL,
    PRIMARY KEY (penalty_id),
    KEY idx_penalties_customer (customer_id),
    KEY idx_penalties_rental (rental_id),
    KEY idx_penalties_status (status),
    CONSTRAINT fk_penalties_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_penalties_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_penalties_bike
        FOREIGN KEY (bike_id) REFERENCES bikes (bike_id),
    CONSTRAINT chk_penalties_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS incidents (
    incident_id     BIGINT          NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT          NOT NULL,
    rental_id       BIGINT          NULL,
    bike_id         BIGINT          NULL,
    incident_type   ENUM('ACCIDENT', 'BIKE_DAMAGE', 'THEFT', 'MECHANICAL_ISSUE', 'TRAFFIC_CHALLAN', 'OTHER') NOT NULL,
    description     VARCHAR(800)    NOT NULL,
    incident_date   DATE            NOT NULL,
    location        VARCHAR(255)    NULL,
    details         VARCHAR(800)    NULL,
    status          ENUM('REPORTED', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'REPORTED',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (incident_id),
    KEY idx_incidents_customer (customer_id),
    KEY idx_incidents_status (status),
    KEY idx_incidents_date (incident_date),
    CONSTRAINT fk_incidents_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_incidents_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_incidents_bike
        FOREIGN KEY (bike_id) REFERENCES bikes (bike_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS challans (
    challan_id      BIGINT          NOT NULL AUTO_INCREMENT,
    rental_id       BIGINT          NULL,
    customer_id     BIGINT          NOT NULL,
    bike_id         BIGINT          NULL,
    challan_date    DATE            NOT NULL,
    description     VARCHAR(500)    NOT NULL,
    amount          DECIMAL(10,2)   NOT NULL,
    status          ENUM('PENDING', 'PAID', 'DISPUTED') NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (challan_id),
    KEY idx_challans_customer (customer_id),
    KEY idx_challans_status (status),
    CONSTRAINT fk_challans_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_challans_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_challans_bike
        FOREIGN KEY (bike_id) REFERENCES bikes (bike_id),
    CONSTRAINT chk_challans_amount CHECK (amount >= 0)
) ENGINE=InnoDB;
