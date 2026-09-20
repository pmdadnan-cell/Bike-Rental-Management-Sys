# BIKEVAULT — Premium Bike Rental Management System

JavaFX desktop application for a bike rental desk. Architecture is **FXML → Controller → Service → DAO → MySQL**. The signature feature is a unique QR identity on every bike: generate, store, scan, verify against MySQL, then rent or return.

## BIKEVAULT — DESKTOP APPLICATION

This is a **JavaFX desktop window**, not a website. Do not open a browser. There is no `http://localhost:8080` and no web server.

```text
JavaFX UI  →  Java services / controllers  →  JDBC  →  MySQL Docker (localhost:3307)
```

Start database:

```powershell
docker compose up -d
```

Start application (from the project folder in PowerShell):

```powershell
.\mvnw.cmd javafx:run
```

The BikeVault login window will open automatically. Sign in there — not in a browser.

Seed logins: `admin` / `admin123` (admin), `hassan` / `hassan123` (customer).

## Features

- Login for ADMIN and STAFF (PBKDF2 password hashes)
- Live dashboard: fleet counts, customers, active rentals, period revenue, recent audit log
- Customers, categories, and bikes with search, filter, sort, and CRUD
- Unique QR identity per bike (`UNIQUE` in MySQL, never the auto-increment id)
- QR PNG generation (ZXing), identity card, webcam scan (Sarxos) with paste fallback
- Rentals: scan bike → availability check → days × daily rate → optional payment
- Returns: scan bike → active rental → late days/fee → bike becomes AVAILABLE
- Payments linked to rentals, with unpaid-balance protection against accidental double payment
- Reports: rentals (including paid totals), bike availability, customers
- Settings: MySQL connection test, QR folder, password change
- Role rule: only ADMIN can delete customers, bikes, categories, and payments

## Architecture

```text
src/main/java/com/bikevault/
  controller/   thin JavaFX controllers
  service/      business rules, Streams, transactions
  dao/          JDBC PreparedStatement access
  model/        entities
  database/     connection + config
  util/         QR, validation, navigation, alerts
  exception/
src/main/resources/com/bikevault/
  view/         FXML
  css/          style.css
database/       schema.sql, seed.sql, migrate.sql
```

## Technologies

- Java 17 (`--release 17`; JDK 21+ or 26 is fine)
- JavaFX 21.0.9, FXML, CSS
- Maven Wrapper / portable Maven 3.9.9
- MySQL 8.4 (Docker)
- JDBC (`mysql-connector-j` 8.4.0)
- ZXing 3.5.3, webcam-capture 0.3.12
- JUnit 5

## Database structure

Normalized tables: `users`, `customers`, `bike_categories`, `bikes`, `rentals`, `returns`, `payments`, `audit_logs`.

Important constraints:

- `bikes.qr_code` UNIQUE NOT NULL
- `bikes.registration_number` UNIQUE NOT NULL
- one ACTIVE rental per bike (`rentals.active_bike_key`)
- one return per rental
- payment reference UNIQUE
- foreign keys on customer, bike, rental, user, category

If the Docker volume was created before that rental guard, apply:

```bat
docker exec -i bikevault-mysql mysql -ubikevault -pbikevault < database\migrate.sql
```

## Docker / MySQL

This machine already used port **3306**, so the app and Compose use **3307**.

```bat
docker compose up -d
```

Wait until `bikevault-mysql` is healthy. Schema and seed load on the first empty volume.

JDBC settings (environment overrides `application.properties`):

| Variable | Default |
|---|---|
| `DB_HOST` | localhost |
| `DB_PORT` | 3307 |
| `DB_NAME` | bikevault |
| `DB_USER` | bikevault |
| `DB_PASSWORD` | bikevault |

## Maven / run JavaFX

From the project folder in PowerShell:

```powershell
.\mvnw.cmd javafx:run
```

The login window opens on the desktop. This command does not start a web server.

Tests:

```powershell
.\mvnw.cmd test
```

## Login credentials (seed)

| Username | Password | Role | Opens |
|---|---|---|---|
| admin | admin123 | ADMIN | Enterprise admin dashboard |
| staff | staff123 | STAFF | Operations dashboard (staff) |
| hassan | hassan123 | CUSTOMER | Premium customer marketplace |

Use **REGISTER** on the login screen to create additional customer accounts.

The admin and customer interfaces share the same MySQL backend, QR identity, rental, return, payment, and audit services. Customers never see admin navigation and cannot load staff controllers. Sensitive writes are also blocked in the service layer.

## QR generation and scanning

1. Open **Bikes**, add a bike or select a seed bike.
2. A unique identity such as `BIKE-550E8400-…` is stored in MySQL.
3. **Generate QR** writes `generated_qr/bike_<qr>.png` and never overwrites an existing file.
4. The identity card shows BIKEVAULT, the QR image, Bike ID (`BIKE-025`), QR ID, brand, model, registration, category, rate, and status.
5. **Scan Bike QR** opens the camera (or paste `BIKEVAULT|BIKE_ID=n|QR=…`).
6. Decode → MySQL lookup by `qr_code` → verify. Invalid QR is rejected. The numeric id in the payload is never trusted alone.
7. A verified bike is handed to Bikes, Rentals, or Returns.

## Rental workflow

Customer + scanned AVAILABLE bike + start/expected dates. Amount = days × daily rate (same-day = 1 day). RENTED / MAINTENANCE / INACTIVE bikes are blocked. On success the bike becomes RENTED and the rental (plus optional payment) is stored in one transaction.

## Return workflow

Scan the rented bike QR → load the active rental → actual return date → late days × category late fee → final amount. Completing the return writes the return row, marks the rental COMPLETED, and sets the bike AVAILABLE in one transaction.

## Payment workflow

Payments always belong to a rental. The Payments screen records only the unpaid balance (rental `final_amount` minus completed payments). A fully paid rental cannot receive another completed payment.

## Reports and audit

**Reports** filters rentals by date/customer/bike/status and shows amount plus paid totals. Availability and customer reports use the Streams API. The **Dashboard** audit table shows the latest `audit_logs` rows (login, QR, rental, return, payment, and bike changes).

## Academic requirements

See [ACADEMIC_REQUIREMENTS.md](ACADEMIC_REQUIREMENTS.md) for the mapped JavaFX / JDBC / Streams / MenuBar checklist.

## College demo path

1. Login as `admin` / `admin123`
2. Dashboard — live counts and recent activity
3. Customers — view or add a customer
4. Bikes — select Yamaha MT-15 (`BIKE-QR-A83F92D1`) or add a new bike
5. Generate QR / show the identity card
6. Scan QR (camera or paste the payload from the QR field)
7. Rentals — customer + verified AVAILABLE bike → Rent Bike
8. Payments — confirm the rental payment
9. Returns — scan the same QR → Return Bike
10. Bikes — status is AVAILABLE again
11. Reports — rental / availability / customer
12. Dashboard — updated stats and audit rows
