-- Apply to an existing bikevault volume created before later schema upgrades.
USE bikevault;

SET @col := (
    SELECT COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = 'bikevault'
       AND table_name = 'rentals'
       AND column_name = 'active_bike_key'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE rentals ADD COLUMN active_bike_key BIGINT GENERATED ALWAYS AS (IF(status = ''ACTIVE'', bike_id, NULL)) STORED',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = 'bikevault'
       AND table_name = 'rentals'
       AND index_name = 'uk_rentals_one_active_bike'
);
SET @sql := IF(@idx = 0,
    'ALTER TABLE rentals ADD UNIQUE KEY uk_rentals_one_active_bike (active_bike_key)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE users
    MODIFY COLUMN role ENUM('ADMIN', 'STAFF', 'CUSTOMER') NOT NULL;

SET @col := (
    SELECT COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = 'bikevault'
       AND table_name = 'customers'
       AND column_name = 'user_id'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE customers ADD COLUMN user_id BIGINT NULL AFTER customer_id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = 'bikevault'
       AND table_name = 'customers'
       AND index_name = 'uk_customers_user'
);
SET @sql := IF(@idx = 0,
    'ALTER TABLE customers ADD UNIQUE KEY uk_customers_user (user_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk := (
    SELECT COUNT(*)
      FROM information_schema.table_constraints
     WHERE table_schema = 'bikevault'
       AND table_name = 'customers'
       AND constraint_name = 'fk_customers_user'
);
SET @sql := IF(@fk = 0,
    'ALTER TABLE customers ADD CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = 'bikevault'
       AND table_name = 'rentals'
       AND column_name = 'active_customer_key'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE rentals ADD COLUMN active_customer_key BIGINT GENERATED ALWAYS AS (IF(status = ''ACTIVE'', customer_id, NULL)) STORED',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = 'bikevault'
       AND table_name = 'rentals'
       AND index_name = 'uk_rentals_one_active_customer'
);
SET @sql := IF(@idx = 0,
    'ALTER TABLE rentals ADD UNIQUE KEY uk_rentals_one_active_customer (active_customer_key)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

INSERT INTO users (username, password_hash, full_name, role, status)
SELECT 'hassan', 'pbkdf2$65536$su2A/eWjPz2oNVQJzd6nWQ==$lVMbUmcd0JpijmhgFSoii041jRWA033sGZhNbQvlRrM=',
       'Hassan Khan', 'CUSTOMER', 'ACTIVE'
 WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'hassan');

UPDATE users
   SET password_hash = 'pbkdf2$65536$su2A/eWjPz2oNVQJzd6nWQ==$lVMbUmcd0JpijmhgFSoii041jRWA033sGZhNbQvlRrM=',
       role = 'CUSTOMER',
       full_name = 'Hassan Khan',
       status = 'ACTIVE'
 WHERE username = 'hassan';

INSERT INTO customers (
    user_id, full_name, email, phone, address, driving_license_number,
    date_of_birth, gender, status, registration_date
)
SELECT u.user_id, 'Hassan Khan', 'hassan.khan@example.com', '9000011122',
       '18 Brigade Road, Bengaluru', 'KA0120240099001', '1997-08-14', 'MALE', 'ACTIVE', '2026-05-01'
  FROM users u
 WHERE u.username = 'hassan'
   AND NOT EXISTS (SELECT 1 FROM customers c WHERE c.user_id = u.user_id);

ALTER TABLE rentals
    MODIFY COLUMN status ENUM('PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'REJECTED')
    NOT NULL DEFAULT 'ACTIVE';

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'rental_qr_token');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN rental_qr_token VARCHAR(80) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND index_name = 'uk_rentals_qr_token');
SET @sql := IF(@idx = 0, 'ALTER TABLE rentals ADD UNIQUE KEY uk_rentals_qr_token (rental_qr_token)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'rental_qr_status');
SET @sql := IF(@col = 0,
    'ALTER TABLE rentals ADD COLUMN rental_qr_status ENUM(''PENDING_APPROVAL'', ''APPROVED'', ''ACTIVE'', ''COMPLETED'', ''REJECTED'', ''EXPIRED'') NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'security_deposit');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN security_deposit DECIMAL(10,2) NOT NULL DEFAULT 0.00', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'approved_by');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN approved_by BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'approved_at');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN approved_at TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'rejection_reason');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN rejection_reason VARCHAR(80) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'rejection_notes');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN rejection_notes VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'return_requested');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN return_requested TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND column_name = 'return_requested_at');
SET @sql := IF(@col = 0, 'ALTER TABLE rentals ADD COLUMN return_requested_at TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk := (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = 'bikevault' AND table_name = 'rentals' AND constraint_name = 'fk_rentals_approved_by');
SET @sql := IF(@fk = 0,
    'ALTER TABLE rentals ADD CONSTRAINT fk_rentals_approved_by FOREIGN KEY (approved_by) REFERENCES users (user_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
