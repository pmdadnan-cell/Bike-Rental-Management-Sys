USE bikevault;

INSERT INTO bikes (qr_code, registration_number, model, brand, category_id, color, daily_rate, status, purchase_date, description)
SELECT * FROM (
    SELECT 'BIKE-QR-R15-001' AS qr_code, 'KA-01-R1-1501' AS registration_number, 'R15' AS model, 'Yamaha' AS brand, 1 AS category_id, 'Racing Blue' AS color, 1400.00 AS daily_rate, 'AVAILABLE' AS status, DATE '2025-03-01' AS purchase_date, 'Sport faired Yamaha' AS description
    UNION ALL SELECT 'BIKE-QR-FZ-002', 'KA-01-FZ-1502', 'FZ', 'Yamaha', 1, 'Matte Grey', 900.00, 'AVAILABLE', DATE '2024-08-12', 'Street naked commuter-sport'
    UNION ALL SELECT 'BIKE-QR-SP125-003', 'KA-02-SP-1503', 'SP 125', 'Honda', 2, 'Athletic Blue', 550.00, 'AVAILABLE', DATE '2025-02-10', 'Fuel-efficient commuter'
    UNION ALL SELECT 'BIKE-QR-HORNET-004', 'KA-02-HN-1504', 'Hornet 2.0', 'Honda', 1, 'Matte Axis Grey', 1100.00, 'AVAILABLE', DATE '2025-04-18', 'Streetfighter Hornet'
    UNION ALL SELECT 'BIKE-QR-CB350-005', 'KA-03-CB-1505', 'CB350', 'Honda', 3, 'DLX Pro Green', 1600.00, 'AVAILABLE', DATE '2025-01-22', 'Modern classic Honda'
    UNION ALL SELECT 'BIKE-QR-HUNTER-006', 'KA-04-HU-1506', 'Hunter 350', 'Royal Enfield', 3, 'Dapper Grey', 1450.00, 'AVAILABLE', DATE '2025-05-09', 'Urban RE hunter'
    UNION ALL SELECT 'BIKE-QR-METEOR-007', 'KA-04-MT-1507', 'Meteor 350', 'Royal Enfield', 3, 'Fireball Red', 1550.00, 'AVAILABLE', DATE '2024-12-01', 'Cruiser meteor'
    UNION ALL SELECT 'BIKE-QR-DUKE200-008', 'KA-05-D2-1508', 'Duke 200', 'KTM', 1, 'Orange', 1300.00, 'AVAILABLE', DATE '2025-03-14', 'Naked KTM 200'
    UNION ALL SELECT 'BIKE-QR-DUKE250-009', 'KA-05-D2-1509', 'Duke 250', 'KTM', 1, 'White', 1500.00, 'AVAILABLE', DATE '2025-06-02', 'Naked KTM 250'
    UNION ALL SELECT 'BIKE-QR-RC200-010', 'KA-05-RC-1510', 'RC 200', 'KTM', 1, 'Orange', 1450.00, 'AVAILABLE', DATE '2024-10-20', 'Faired KTM RC'
    UNION ALL SELECT 'BIKE-QR-RTR200-011', 'KA-06-AP-1511', 'Apache RTR 200', 'TVS', 1, 'Knight Black', 950.00, 'AVAILABLE', DATE '2025-01-08', 'Apache 200 street'
    UNION ALL SELECT 'BIKE-QR-RAIDER-012', 'KA-06-RD-1512', 'Raider', 'TVS', 2, 'Blazing Blue', 700.00, 'AVAILABLE', DATE '2025-07-11', 'Sporty TVS commuter'
    UNION ALL SELECT 'BIKE-QR-GIXXER-013', 'KA-07-GX-1513', 'Gixxer', 'Suzuki', 1, 'Metallic Matte Black', 1000.00, 'AVAILABLE', DATE '2024-09-19', 'Suzuki street sport'
    UNION ALL SELECT 'BIKE-QR-ACCESS-014', 'KA-07-AC-1514', 'Access 125', 'Suzuki', 2, 'Pearl Mirage White', 480.00, 'AVAILABLE', DATE '2025-02-28', 'Family scooter'
    UNION ALL SELECT 'BIKE-QR-P150-015', 'KA-08-P1-1515', 'Pulsar 150', 'Bajaj', 2, 'Neon Red', 650.00, 'AVAILABLE', DATE '2024-07-07', 'Everyday Pulsar'
    UNION ALL SELECT 'BIKE-QR-NS200-016', 'KA-08-NS-1516', 'Pulsar NS200', 'Bajaj', 1, 'Graphite Black', 1050.00, 'AVAILABLE', DATE '2025-03-30', 'Naked sports Pulsar'
    UNION ALL SELECT 'BIKE-QR-DOM-017', 'KA-08-DM-1517', 'Dominar 400', 'Bajaj', 3, 'Aurora Green', 1700.00, 'AVAILABLE', DATE '2024-11-11', 'Touring Dominar'
    UNION ALL SELECT 'BIKE-QR-XPULSE-018', 'KA-09-XP-1518', 'Xpulse 200', 'Hero', 2, 'Sports Red', 850.00, 'AVAILABLE', DATE '2025-04-04', 'Adventure commuter'
    UNION ALL SELECT 'BIKE-QR-SPL-019', 'KA-09-SP-1519', 'Splendor', 'Hero', 2, 'Heavy Grey', 400.00, 'AVAILABLE', DATE '2023-10-15', 'Classic commuter'
    UNION ALL SELECT 'BIKE-QR-SPD400-020', 'KA-10-TS-1520', 'Speed 400', 'Triumph', 1, 'Carnival Red', 2200.00, 'AVAILABLE', DATE '2025-08-01', 'Modern Triumph roadster'
    UNION ALL SELECT 'BIKE-QR-SCR-021', 'KA-10-SC-1521', 'Scrambler', 'Triumph', 3, 'Khaki Green', 2300.00, 'AVAILABLE', DATE '2025-08-08', 'Triumph scrambler'
    UNION ALL SELECT 'BIKE-QR-G310R-022', 'KA-11-BM-1522', 'G 310 R', 'BMW', 1, 'Cosmic Black', 2400.00, 'AVAILABLE', DATE '2025-05-21', 'BMW roadster'
    UNION ALL SELECT 'BIKE-QR-G310GS-023', 'KA-11-GS-1523', 'G 310 GS', 'BMW', 3, 'Polar White', 2500.00, 'MAINTENANCE', DATE '2025-05-22', 'Adventure BMW service'
    UNION ALL SELECT 'BIKE-QR-NINJA-024', 'KA-12-NJ-1524', 'Ninja 300', 'Kawasaki', 1, 'Lime Green', 2100.00, 'AVAILABLE', DATE '2024-06-16', 'Faired Ninja'
    UNION ALL SELECT 'BIKE-QR-Z650-025', 'KA-12-Z6-1525', 'Z650', 'Kawasaki', 1, 'Metallic Spark Black', 2800.00, 'AVAILABLE', DATE '2025-01-30', 'Naked middleweight'
    UNION ALL SELECT 'BIKE-QR-OLA-026', 'KA-13-OL-1526', 'S1 Pro', 'Ola', 4, 'Jet Black', 950.00, 'AVAILABLE', DATE '2025-09-01', 'Electric scooter'
    UNION ALL SELECT 'BIKE-QR-IQUBE-027', 'KA-13-IQ-1527', 'iQube', 'TVS', 4, 'Mint Blue', 800.00, 'AVAILABLE', DATE '2025-06-18', 'TVS electric'
    UNION ALL SELECT 'BIKE-QR-HIMA-028', 'KA-14-HI-1528', 'Himalayan', 'Royal Enfield', 3, 'Granite Black', 1750.00, 'AVAILABLE', DATE '2024-05-05', 'Adventure Himalayan'
    UNION ALL SELECT 'BIKE-QR-AEROX-029', 'KA-01-AX-1529', 'Aerox 155', 'Yamaha', 2, 'Cyan Storm', 750.00, 'RENTED', DATE '2025-07-20', 'Maxi scooter'
) AS fleet
WHERE NOT EXISTS (
    SELECT 1 FROM bikes b WHERE b.registration_number = fleet.registration_number OR b.qr_code = fleet.qr_code
);
