# Academic requirements — BIKEVAULT

College JavaFX checklist mapped to the implementation. Every item below is present in the project.

## JavaFX GUI

| Requirement | Where |
|---|---|
| Login screen | `Login.fxml` + `LoginController` |
| Main dashboard | `Dashboard.fxml` + `DashboardController` (live MySQL cards) |
| Customer data entry | `Customer.fxml` + `CustomerController` |
| Bike data entry | `Bike.fxml` + `BikeController` |
| Rental screen | `Rental.fxml` + `RentalController` |
| Return screen | `Return.fxml` + `ReturnController` |
| Payment screen | `Payment.fxml` + `PaymentController` |
| QR scanner | `QRScanner.fxml` + `QRScannerController` |
| TableView records | Every master/transaction module plus dashboard audit and reports |
| Reports | `Reports.fxml` + `ReportsController` |

## Required controls (all used with a real purpose)

| Control | Purpose |
|---|---|
| Label | Titles, metric values, status bar, identity card |
| TextField | Forms, search, QR paste, connection details |
| PasswordField | Login and Settings password change |
| TextArea | Address, notes, descriptions |
| Button | Login, CRUD, Scan, Generate QR, Refresh, Test connection |
| ComboBox | Status, period, payment method, report filters |
| DatePicker | DOB, rental dates, dashboard as-of, report range |
| RadioButton | Customer gender |
| CheckBox | Remember username; collect payment/late fee; hide inactive bikes; confirm password change |
| TableView | Customers, bikes, rentals, returns, payments, audit, reports |
| MenuBar | File / Master / Transactions / Reports / Help / Exit |
| Alert dialog | Validation, confirmations, errors (`AlertUtil`) |
| ImageView | Brand mark, metric icons, QR PNG preview |

## Required layouts

| Layout | Purpose |
|---|---|
| BorderPane | Dashboard chrome (menu, sidebar, workspace, status) |
| GridPane | Forms and metric cards |
| VBox | Sidebar, module roots, form cards |
| HBox | Toolbars, status bar, stat cards |
| AnchorPane | Login ambient pane; Customer Report tab |
| StackPane | Login root; dashboard `contentHost`; bike QR frame |

## MenuBar — exact menus

`File`, `Master`, `Transactions`, `Reports`, `Help`, `Exit` with the required submenu items in `Dashboard.fxml`.

## Database / JDBC

- MySQL 8.4 via Docker (`docker-compose.yml`)
- JDBC singleton `DatabaseConnection` with transactions
- Every DAO uses `PreparedStatement` + `ResultSet` (see `DaoSupport`)
- Tables: `users`, `customers`, `bike_categories`, `bikes`, `rentals`, `returns`, `payments`, `audit_logs`
- Primary keys, foreign keys, UNIQUE / NOT NULL / CHECK (ENUM + CHECK where used), indexes in `database/schema.sql`
- Bike QR identity is **not** the auto-increment id (`bikes.qr_code UNIQUE NOT NULL`)

## Streams API

Used in `DashboardService`, `ReportService`, `RentalService.search`, `CustomerService.search`, `BikeService.search`, and `PaymentService.search` for filter / sort / count / revenue aggregation.

## MVC

```text
FXML → Controller → Service → DAO → Database
```

Controllers stay thin. Pricing lives in `PricingCalculator`. QR generate/decode live in `QRGenerator` / `QRDecoder` / `QRService`.

## Phase 8 QA map

### JavaFX

- [x] JavaFX starts (`Main` → `NavigationUtil.showLogin`)
- [x] FXML loads (login, dashboard, all modules)
- [x] CSS loads (`/com/bikevault/css/style.css`)
- [x] Navigation works (sidebar + menus swap `contentHost`)

### Database

- [x] MySQL connects (status bar + Settings test)
- [x] Insert / update / delete / search on master and transaction modules
- [x] TableView loads from DAO `findAll` on a background thread (`UiAsync`)

### QR

- [x] Unique QR generated (`QRGenerator.newIdentity`)
- [x] QR stored in MySQL (`bikes.qr_code`)
- [x] QR image generated under `generated_qr/` (never overwritten)
- [x] QR displayed on the bike identity card (`BIKE-025`, QR ID, brand, model, registration, category, rate, status)
- [x] Webcam starts in the scanner (paste fallback if no camera)
- [x] QR detected / parsed (`QRDecoder`)
- [x] QR verified against MySQL (`QRService`)
- [x] Bike details populated, then handed to Rental / Return via `ScanContext`

### Rental

- [x] Available bike can be rented
- [x] Rented bike cannot be rented again (`RentalService`)
- [x] Rental amount calculated (`PricingCalculator`)
- [x] Bike status becomes `RENTED` in the same transaction

### Return

- [x] QR identifies the bike
- [x] Active rental found
- [x] Late fee calculated
- [x] Bike becomes `AVAILABLE`

### Academic extras

- [x] 10+ controls, 3+ layouts, exact MenuBar, event handling
- [x] JDBC + PreparedStatement + ResultSet + MySQL
- [x] 3+ related tables, PKs, FKs, constraints, TableView, Streams API
