INSERT INTO categories(ID, NAME)
VALUES ('1', 'cat-1-1');
INSERT INTO categories(ID, NAME)
VALUES ('2', 'cat-2-2');
INSERT INTO categories(ID, NAME)
VALUES ('3', 'cat-3-3');
INSERT INTO categories(ID, NAME)
VALUES ('4', 'cat-4-4');
INSERT INTO categories(ID, NAME)
VALUES ('5', 'cat-5-5');
INSERT INTO categories(ID, NAME)
VALUES ('6', 'cat-6-6');
INSERT INTO categories(ID, NAME)
VALUES ('7', 'cat-7-7');
INSERT INTO categories(ID, NAME)
VALUES ('8', 'cat-8-8');
INSERT INTO categories(ID, NAME)
VALUES ('9', 'cat-9-9');
INSERT INTO categories(ID, NAME)
VALUES ('10', 'cat-10-10');
INSERT INTO categories(ID, NAME)
VALUES ('11', 'cat-11-11');
INSERT INTO categories(ID, NAME)
VALUES ('12', 'cat-12-12');
INSERT INTO categories(ID, NAME)
VALUES ('13', 'cat-13-13');

INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('1', 'Product 1', 'Description 1', '1');
INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('2', 'Product 2', 'Description 2', '1');
INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('3', 'Product 3', 'Description 3', '2');
INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('4', 'Product 4', 'Description 4', '2');
INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('5', 'Product 5', 'Description 5', '3');
INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('6', 'Product 6', 'Description 6', '3');
INSERT INTO products (ID, NAME, DESCRIPTION, CATEGORY_ID)
VALUES ('7', 'Product 7', 'Description 7', '3');


INSERT INTO suppliers (ID, NAME, ADDRESS, EMAIL, PHONE_NO, ALT_PHONE_NO, CREATED_AT, CREATED_BY)
VALUES ('1', 'Supplier 1', '123 Main St., Anytown, USA', 'supplier1@example.com', '555-1234', '555-5678',
        '2022-04-30 10:00:00.000',
        'admin');

INSERT INTO suppliers (ID, NAME, ADDRESS, EMAIL, PHONE_NO, ALT_PHONE_NO, CREATED_AT, CREATED_BY)
VALUES ('2', 'Supplier 2', '456 Maple Ave., Anytown, USA', 'supplier2@example.com', '555-2345', '555-6789',
        '2022-04-30 10:00:00.000',
        'admin');

INSERT INTO suppliers (ID, NAME, ADDRESS, EMAIL, PHONE_NO, ALT_PHONE_NO, CREATED_AT, CREATED_BY)
VALUES ('3', 'Supplier 3', '789 Oak St., Anytown, USA', 'supplier3@example.com', '555-3456', '555-7890',
        '2022-04-30 10:00:00.000',
        'admin');

-- Insert sample suppliers
INSERT INTO suppliers (id, name, address, email, phone_no, alt_phone_no, created_at, created_by)
VALUES ('5', 'ABC Electronics', '123 Tech Street, NY', 'contact@abcelectronics.com', '1234567890', '0987654321',
        '2024-01-01 00:00:00.000', 'admin'),
       ('6', 'Global Furniture', '456 Home Ave, LA', 'support@globalfurniture.com', '9876543210', '0123456789',
        '2024-01-01 00:00:00.000', 'admin'),
       ('7', 'Urban Clothing Co.', '789 Fashion Blvd, TX', 'info@urbanclothing.com', '5432109876', '6789012345',
        '2024-01-01 00:00:00.000', 'admin');



INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('1', '1', '1', 100, 50, '2022-01-01 00:00:00.000', '2022-04-30 10:00:00.000', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('2', '2', '2', 200, 100, '2022-01-02 00:00:00.000', '2022-04-30 10:00:00.000', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('3', '3', '3', 300, 150, '2022-01-03 00:00:00.000', '2022-04-30 10:00:00.000', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('4', '2', '2', 400, 200, '2022-01-04 00:00:00.000', '2022-04-30 10:00:00.000', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('5', '2', '2', 500, 250, '2022-01-05 00:00:00.000', '2022-04-30 10:00:00.000', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('6', '1', '2', 500, 250, '2022-01-05 00:00:00.000', '2022-04-30 10:00:00.000', 'Admin', NULL, NULL);

INSERT INTO customers (id, name, address, email, phone_no, alt_phone_no, created_at, created_by)
VALUES ('1', 'John Doe', '123 Main St', 'john.doe@example.com', '555-1234', NULL, '2022-04-30 10:00:00.000', 'admin'),
       ('2', 'Jane Smith', '456 Oak Ave', 'jane.smith@example.com', '555-5678', '555-9101', '2022-04-30 10:00:00.000',
        'admin'),
       ('3', 'Bob Johnson', '789 Maple St', 'bob.johnson@example.com', '555-1212', NULL, '2022-04-30 10:00:00.000',
        'admin'),
       ('4', 'Sarah Williams', '321 Elm St', 'sarah.williams@example.com', '555-4321', '555-6789',
        '2022-04-30 10:00:00.000', 'admin');

INSERT INTO invoices (id, customer_id, paid, discount, adjustment, due_amount, notes, invoice_date, created_at,
                      created_by, modified_at, modified_by)
VALUES ('1', '1', 100.00, 0.00, 0.00, 0.00, 'No notes', '2023-04-28 00:00:00.000', '2022-04-30 10:00:00.000', 'admin', NULL, NULL),
       ('2', '1', 0.00, 0.00, 0.00, 100.00, 'No notes', '2023-04-29 00:00:00.000', '2022-04-30 10:00:00.000', 'john', NULL, NULL),
       ('3', '2', 50.00, 10.00, -5.00, 45.00, '15% discount for loyal customers', '2023-04-30 00:00:00.000', '2022-04-30 10:00:00.000',
        'admin', NULL,
        NULL),
       ('4', '3', 0.00, 5.00, 0.00, 95.00, 'Early payment discount of 5%', '2022-04-30 10:00:00.000', '2022-04-30 10:00:00.000',
        'susan',
        NULL, NULL);


INSERT INTO companies (id, status, name, address, email, phone, details_top, extra, currency, invoice_no, created_at,
                       updated_at, deleted_at)
VALUES ('1', 'DRAFT', 'ABC Company', '123 Main St, Anytown USA', 'info@abccompany.com', '+1 555-1234',
        'ABC Company Invoice',
        'Some extra details', 'USD', 1000, '2022-04-30 10:00:00.000', '2022-04-30 10:00:00.000', NULL),
       ('2', 'DRAFT', 'XYZ Corporation', '456 High St, Anytown USA', 'info@xyzcorp.com', '+1 555-5678',
        'XYZ Corp Invoice',
        'Some other details', 'EUR', 2000, '2022-04-30 10:00:00.000', '2022-04-30 10:00:00.000', NULL),
       ('3', 'DRAFT', 'PQR Inc.', '789 Elm St, Anytown USA', 'info@pqrinc.com', '+1 555-9012', 'PQR Inc. Invoice',
        'Some more details', 'CAD', 3000, '2022-04-30 10:00:00.000', '2022-04-30 10:00:00.000', NULL);


INSERT INTO inventory (id, product_id, cost, listing_price, sold_price, discount, procurement_id, invoice_id)
VALUES ('1', '1', 50.00, 80.00, 100.00, 0.00, '1', '1'),
       ('2', '2', 75.00, 120.00, 150.00, 0.00, '2', '2'),
       ('3', '3', 100.00, 160.00, 200.00, 0.00, '3', '3'),
       ('4', '3', 30.00, 160.00, 200.00, 0.00, '3', '3'),
       ('5', '3', 60.00, 160.00, 200.00, 0.00, '3', '3'),
       ('6', '3', 80.00, 160.00, 200.00, 0.00, '3', '3'),
       ('7', '3', 10.00, 160.00, 200.00, 0.00, '3', '3'),
       ('8', '3', 45.00, 160.00, 200.00, 0.00, '3', '3'),
       ('9', '3', 15.00, 160.00, 200.00, 0.00, '3', '3'),
       ('10', '4', 25.00, 40.00, 50.00, 0.00, '4', '4');

INSERT INTO currencies (id, country, currency_abbr, currency_name, currency_symbol, created_at)
VALUES ('1', 'United States', 'USD', 'US Dollar', 'U$', '2024-01-01 00:00:00.000'),
       ('2', 'United Kingdom', 'GBP', 'British Pound', '£', '2024-01-01 00:00:00.000'),
       ('3', 'Canada', 'CAD', 'Canadian Dollar', 'C$', '2024-01-01 00:00:00.000'),
       ('4', 'Australia', 'AUD', 'Australian Dollar', 'A$', '2024-01-01 00:00:00.000'),
       ('5', 'Japan', 'JPY', 'Japanese Yen', '¥', '2024-01-01 00:00:00.000'),
       ('6', 'India', 'INR', 'Indian Rupee', '₹', '2024-01-01 00:00:00.000'),
       ('7', 'China', 'CNY', 'Chinese Yuan', '¥', '2024-01-01 00:00:00.000'),
       ('8', 'Russia', 'RUB', 'Russian Ruble', '₽', '2024-01-01 00:00:00.000'),
       ('9', 'South Korea', 'KRW', 'South Korean Won', '₩', '2024-01-01 00:00:00.000'),
       ('10', 'Mexico', 'MXN', 'Mexican Peso', 'M$', '2024-01-01 00:00:00.000'),
       ('11', 'Brazil', 'BRL', 'Brazilian Real', 'R$', '2024-01-01 00:00:00.000'),
       ('12', 'South Africa', 'ZAR', 'South African Rand', 'R', '2024-01-01 00:00:00.000'),
       ('13', 'New Zealand', 'NZD', 'New Zealand Dollar', 'N$', '2024-01-01 00:00:00.000'),
       ('14', 'Singapore', 'SGD', 'Singapore Dollar', 'S$', '2024-01-01 00:00:00.000');

INSERT INTO exchange_rates (id, base_currency_id, foreign_currency_id, exchange_rate, inv_exchange_rate, created_at)
VALUES ('1', '1', '2', 0.85, 1.18, '2024-01-01 00:00:00.000'),        -- USD to GBP
       ('2', '1', '3', 1.25, 0.80, '2024-01-01 00:00:00.000'),        -- USD to CAD
       ('3', '1', '4', 1.35, 0.74, '2024-01-01 00:00:00.000'),        -- USD to AUD
       ('4', '1', '5', 110.50, 0.009, '2024-01-01 00:00:00.000'),     -- USD to JPY
       ('5', '1', '6', 75.25, 0.013, '2024-01-01 00:00:00.000'),      -- USD to INR
       ('6', '2', '1', 1.18, 0.85, '2024-01-01 00:00:00.000'),        -- GBP to USD
       ('7', '2', '3', 1.47, 0.68, '2024-01-01 00:00:00.000'),        -- GBP to CAD
       ('8', '2', '4', 1.59, 0.63, '2024-01-01 00:00:00.000'),        -- GBP to AUD
       ('9', '3', '1', 0.80, 1.25, '2024-01-01 00:00:00.000'),        -- CAD to USD
       ('10', '3', '2', 0.68, 1.47, '2024-01-01 00:00:00.000'),       -- CAD to GBP
       ('11', '4', '1', 0.74, 1.35, '2024-01-01 00:00:00.000'),       -- AUD to USD
       ('12', '4', '2', 0.63, 1.59, '2024-01-01 00:00:00.000'),       -- AUD to GBP
       ('13', '5', '1', 0.009, 110.50, '2024-01-01 00:00:00.000'),    -- JPY to USD
       ('14', '6', '1', 0.013, 75.25, '2024-01-01 00:00:00.000'),     -- INR to USD
       ('15', '7', '1', 0.15, 6.67, '2024-01-01 00:00:00.000'),       -- CNY to USD
       ('16', '8', '1', 0.014, 71.43, '2024-01-01 00:00:00.000'),     -- RUB to USD
       ('17', '9', '1', 0.00075, 1333.33, '2024-01-01 00:00:00.000'), -- KRW to USD
       ('18', '10', '1', 0.05, 20.00, '2024-01-01 00:00:00.000'),     -- MXN to USD
       ('19', '11', '1', 0.20, 5.00, '2024-01-01 00:00:00.000'),      -- BRL to USD
       ('20', '12', '1', 0.055, 18.18, '2024-01-01 00:00:00.000'); -- ZAR to USD

-- value_test_table is seeded in InitData: it is the only seeded row with real `date` and `time`
-- columns, and no literal is accepted by every database. A DATE column will not take the time part
-- that SQLite's driver requires, and a bare date is what the other four want.


INSERT INTO users (id, created_at, deleted_at, email, name, phone_no, type, updated_at, last_login_at)
VALUES ('1a2b3c4d-5678-90ab-cdef-1234567890ab', '2024-02-28 12:00:00.000', NULL, 'john.doe@example.com', 'John Doe',
        '9876543210', 'ADMIN', '2024-02-28 15:00:00.000', NULL),
       ('2b3c4d5e-6789-01ab-cdef-2345678901bc', '2024-02-28 13:15:00.000', NULL, 'jane.smith@example.com', 'Jane Smith',
        '8765432109', 'USER', '2024-02-28 16:30:00.000', NULL),
       ('3c4d5e6f-7890-12ab-cdef-3456789012cd', '2024-02-27 09:45:00.000', NULL, 'michael.johnson@example.com',
        'Michael Johnson', '7654321098', 'USER', '2024-02-28 14:45:00.000', NULL),
       ('4d5e6f7g-8901-23ab-cdef-4567890123de', '2024-02-26 08:30:00.000', NULL, 'emily.brown@example.com', 'Emily Brown',
        '6543210987', 'USER', '2024-02-27 10:15:00.000', NULL),
       ('5e6f7g8h-9012-34ab-cdef-5678901234ef', '2024-02-25 17:20:00.000', '2024-02-28 12:00:00.000',
        'william.davis@example.com', 'William Davis', '5432109876', 'USER', '2024-02-26 09:00:00.000', NULL);

INSERT INTO tasks (id, title, priority, difficulty)
VALUES ('task-0000-0000-0000-000000000001', 'Fix login bug', 'HIGH', 1),
       ('task-0000-0000-0000-000000000002', 'Write docs', 'LOW', 0),
       ('task-0000-0000-0000-000000000003', 'Refactor module', 'MEDIUM', 1),
       ('task-0000-0000-0000-000000000004', 'Add tests', 'HIGH', 2),
       ('task-0000-0000-0000-000000000005', 'Deploy release', 'HIGH', 2);

-- pk_uuid_test and fk_null_entity are seeded in InitData instead: their primary key is a
-- java.util.UUID, and no SQL literal for one is portable across the databases this app runs on.
-- Hibernate maps the field to a native `uuid` column on H2 and Postgres but to `binary(16)` on
-- MySQL and HSQLDB, and HSQLDB rejects the textual form outright ("invalid character value for
-- cast"). Inserting through JPA lets each dialect bind the value its own way.

INSERT INTO auto_gen_identity (name)
VALUES ('identity-seed-1'),
       ('identity-seed-2'),
       ('identity-seed-3');



