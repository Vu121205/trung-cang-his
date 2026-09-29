-- Demo data only: reset TOTAL and AVAILABLE stock to 1000 per medicine/supply.
-- Medicines and supplies share the medicines / medicine_batches tables.
-- Existing batch dates and prices are preserved; their quantities become zero.
-- The synthetic demo batch has no expiry date and is available for dispensing.
-- Run manually after catalog imports. Rerunning resets stock already dispensed.
-- Back up medicine_batches before running against an existing database.
USE trung_cang_his;
START TRANSACTION;

UPDATE medicine_batches SET quantity = 0;

INSERT INTO medicine_batches
    (medicine_id, batch_number, manufacture_date, expiry_date, quantity, unit_price, created_at)
SELECT id, 'DEMO-STOCK-1000', NULL, NULL, 1000, 0, CURRENT_TIMESTAMP
FROM medicines
ON DUPLICATE KEY UPDATE
    quantity = 1000,
    manufacture_date = NULL,
    expiry_date = NULL;

COMMIT;

-- Expected: every catalog item has total_stock = available_stock = 1000.
SELECT m.code, m.name, SUM(b.quantity) AS total_stock,
       SUM(CASE WHEN b.quantity > 0
                     AND (b.expiry_date IS NULL OR b.expiry_date >= CURRENT_DATE)
                THEN b.quantity ELSE 0 END) AS available_stock
FROM medicines m
LEFT JOIN medicine_batches b ON b.medicine_id = m.id
GROUP BY m.id, m.code, m.name
ORDER BY m.code;
