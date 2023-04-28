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


INSERT INTO suppliers (ID, NAME, ADDRESS, EMAIL, PHONE_NO, ALT_PHONE_NO, CREATED_AT, CREATED_BY)
VALUES ('1', 'Supplier 1', '123 Main St., Anytown, USA', 'supplier1@example.com', '555-1234', '555-5678',
        '2022-04-30 10:00:00',
        'admin');

INSERT INTO suppliers (ID, NAME, ADDRESS, EMAIL, PHONE_NO, ALT_PHONE_NO, CREATED_AT, CREATED_BY)
VALUES ('2', 'Supplier 2', '456 Maple Ave., Anytown, USA', 'supplier2@example.com', '555-2345', '555-6789',
        '2022-04-30 10:00:00',
        'admin');

INSERT INTO suppliers (ID, NAME, ADDRESS, EMAIL, PHONE_NO, ALT_PHONE_NO, CREATED_AT, CREATED_BY)
VALUES ('3', 'Supplier 3', '789 Oak St., Anytown, USA', 'supplier3@example.com', '555-3456', '555-7890',
        '2022-04-30 10:00:00',
        'admin');


INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('1', '1', '1', 100, 50, '2022-01-01', '2022-04-30 10:00:00', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('2', '2', '2', 200, 100, '2022-01-02', '2022-04-30 10:00:00', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('3', '3', '3', 300, 150, '2022-01-03', '2022-04-30 10:00:00', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('4', '2', '2', 400, 200, '2022-01-04', '2022-04-30 10:00:00', 'Admin', NULL, NULL);

INSERT INTO procurements (ID, PRODUCT_ID, supplier_id, amount, due_amount, purchase_date, created_at, created_by,
                          modified_at, modified_by)
VALUES ('5', '2', '2', 500, 250, '2022-01-05', '2022-04-30 10:00:00', 'Admin', NULL, NULL);

INSERT INTO customers (id, name, address, email, phone_no, alt_phone_no, created_at, created_by)
VALUES ('1', 'John Doe', '123 Main St', 'john.doe@example.com', '555-1234', NULL, '2022-04-30 10:00:00', 'admin'),
       ('2', 'Jane Smith', '456 Oak Ave', 'jane.smith@example.com', '555-5678', '555-9101', '2022-04-30 10:00:00',
        'admin'),
       ('3', 'Bob Johnson', '789 Maple St', 'bob.johnson@example.com', '555-1212', NULL, '2022-04-30 10:00:00',
        'admin'),
       ('4', 'Sarah Williams', '321 Elm St', 'sarah.williams@example.com', '555-4321', '555-6789',
        '2022-04-30 10:00:00', 'admin');

INSERT INTO invoices (id, customer_id, paid, discount, adjustment, due_amount, notes, invoice_date, created_at,
                      created_by, modified_at, modified_by)
VALUES ('1', '1', 100.00, 0.00, 0.00, 0.00, 'No notes', '2023-04-28', '2022-04-30 10:00:00', 'admin', NULL, NULL),
       ('2', '1', 0.00, 0.00, 0.00, 100.00, 'No notes', '2023-04-29', '2022-04-30 10:00:00', 'john', NULL, NULL),
       ('3', '2', 50.00, 10.00, -5.00, 45.00, '15% discount for loyal customers', '2023-04-30', '2022-04-30 10:00:00',
        'admin', NULL,
        NULL),
       ('4', '3', 0.00, 5.00, 0.00, 95.00, 'Early payment discount of 5%', '2023-05-01', '2022-04-30 10:00:00', 'susan',
        NULL, NULL);


INSERT INTO companies (id, name, address, email, phone, details_top, extra, currency, invoice_no, created_at,
                       updated_at, deleted_at)
VALUES ('1', 'ABC Company', '123 Main St, Anytown USA', 'info@abccompany.com', '+1 555-1234', 'ABC Company Invoice',
        'Some extra details', 'USD', 1000, '2022-04-30 10:00:00', '2022-04-30 10:00:00', NULL),
       ('2', 'XYZ Corporation', '456 High St, Anytown USA', 'info@xyzcorp.com', '+1 555-5678', 'XYZ Corp Invoice',
        'Some other details', 'EUR', 2000, '2022-04-30 10:00:00', '2022-04-30 10:00:00', NULL),
       ('3', 'PQR Inc.', '789 Elm St, Anytown USA', 'info@pqrinc.com', '+1 555-9012', 'PQR Inc. Invoice',
        'Some more details', 'CAD', 3000, '2022-04-30 10:00:00', '2022-04-30 10:00:00', NULL);


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



