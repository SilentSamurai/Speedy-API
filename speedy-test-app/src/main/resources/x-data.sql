create table categories
(
    id   varchar(255) not null,
    name varchar(250) not null,
    primary key (id)
);
create table companies
(
    id                varchar(255)  not null,
    address           varchar(1024) not null,
    created_at        timestamp,
    currency          varchar(8)    not null,
    default_generator integer,
    deleted_at        timestamp,
    details_top       varchar(1024),
    email             varchar(255),
    extra             varchar(1024),
    invoice_no        integer,
    name              varchar(255)  not null,
    phone             varchar(15)   not null,
    updated_at        timestamp,
    primary key (id)
);

create table currencies
(
    id              varchar(255) not null,
    country         varchar(32),
    created_at      timestamp default CURRENT_TIMESTAMP,
    currency_abbr varchar(10) not null unique,
    currency_name   varchar(64)  not null,
    currency_symbol varchar(10)  not null,
    primary key (id)
);

create table customers
(
    id           varchar(255) not null,
    address      varchar(1024),
    alt_phone_no varchar(15),
    created_at   timestamp,
    created_by   varchar(255),
    email        varchar(255),
    name         varchar(255) not null,
    phone_no     varchar(15)  not null,
    primary key (id)
);

create table exchange_rates
(
    id                  varchar(255) not null,
    created_at          timestamp,
    exchange_rate double not null,
    inv_exchange_rate double not null,
    base_currency_id    varchar(255) not null,
    foreign_currency_id varchar(255) not null,
    primary key (id)
);

create table inventory
(
    id             varchar(255) not null,
    cost double not null,
    discount double not null,
    listing_price double not null,
    sold_price double not null,
    invoice_id     varchar(255) not null,
    procurement_id varchar(255) not null,
    product_id     varchar(255) not null,
    primary key (id)
);

create table invoices
(
    id           varchar(255) not null,
    adjustment double not null,
    created_at   timestamp,
    created_by   varchar(255),
    discount double not null,
    due_amount double not null,
    invoice_date timestamp    not null,
    modified_at  timestamp,
    modified_by  varchar(255),
    notes        varchar(1024),
    paid double not null,
    customer_id  varchar(255) not null,
    primary key (id)
);

create table orders
(
    product_id  varchar(250) not null,
    supplier_id varchar(250) not null,
    discount double,
    order_date  timestamp,
    price double,
    primary key (product_id, supplier_id)
);

create table procurements
(
    id            varchar(255) not null,
    amount double not null,
    created_at    timestamp,
    created_by    varchar(255),
    due_amount double not null,
    modified_at   timestamp,
    modified_by   varchar(255),
    purchase_date timestamp    not null,
    product_id    varchar(255) not null,
    supplier_id   varchar(255) not null,
    primary key (id)
);



create table products
(
    id          varchar(255) not null,
    description varchar(1024),
    name        varchar(255) not null,
    category_id varchar(255) not null,
    primary key (id)
);

create table suppliers
(
    id           varchar(255) not null,
    address      varchar(1024),
    alt_phone_no varchar(15)  not null,
    created_at   timestamp,
    created_by   varchar(255),
    email        varchar(255),
    name         varchar(255) not null,
    phone_no     varchar(15)  not null,
    primary key (id)
);

create table users
(
    id          varchar(255) not null,
    created_at  timestamp,
    deleted_at  timestamp,
    email       varchar(250) not null,
    name        varchar(250) not null,
    phone_no    varchar(15)  not null,
    profile_pic varchar(512) not null,
    updated_at  timestamp,
    company_id  varchar(255) not null,
    primary key (id)
);

CREATE TABLE value_test_table
(
    id              VARCHAR(255) NOT NULL,
    local_date_time datetime NULL,
    local_date      date NULL,
    local_time      time NULL,
    instant_time    datetime NULL,
    zoned_date_time TIMESTAMP WITH TIME ZONE NULL,
    boolean_value boolean NULL,
    CONSTRAINT pk_value_test_table PRIMARY KEY (id)
);

alter table categories
    add constraint categories_name_key unique (name);
alter table companies
    add constraint companies_phone_key unique (phone);
alter table customers
    add constraint customers_alt_phone_no_key unique (alt_phone_no);
alter table customers
    add constraint customers_phone_no_key unique (phone_no);

alter table products
    add constraint UK_o61fmio5yukmmiqgnxf8pnavn unique (name);
alter table suppliers
    add constraint suppliers_alt_phone_no_key unique (alt_phone_no);
alter table suppliers
    add constraint suppliers_phone_no_key unique (phone_no);
alter table users
    add constraint users_phone_no_key unique (phone_no);
alter table users
    add constraint users_email_key unique (email);
alter table exchange_rates
    add constraint FKrqnh6pk2bh3emod0btk37g0fp foreign key (base_currency_id) references currencies;
alter table exchange_rates
    add constraint FKpafehxlj8ac40i364hbg1shu2 foreign key (foreign_currency_id) references currencies;
alter table inventory
    add constraint FK3ocy6yq5a8ys904nuk2ubh8nw foreign key (invoice_id) references invoices;
alter table inventory
    add constraint FKewrm1ymgyu6eyyidx0xaj2gva foreign key (procurement_id) references procurements;
alter table inventory
    add constraint FKq2yge7ebtfuvwufr6lwfwqy9l foreign key (product_id) references products;
alter table invoices
    add constraint FKq2w4hmh6l9othnp6cepp0cfe2 foreign key (customer_id) references customers;
alter table procurements
    add constraint FK4dbsmywcrfynicyvso8b28utc foreign key (product_id) references products;
alter table procurements
    add constraint FKjff8ahqmnydxc665vew5tp7ul foreign key (supplier_id) references suppliers;
alter table products
    add constraint FKog2rp4qthbtt2lfyhfo32lsw9 foreign key (category_id) references categories;
alter table users
    add constraint FKin8gn4o1hpiwe6qe4ey7ykwq7 foreign key (company_id) references companies;

create
or replace view product_view as
SELECT *
FROM products;


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

INSERT into currencies (id, country, currency_abbr, currency_name, currency_symbol)
values ('1', 'United States', 'USD', 'US Dollar', 'U$'),
       ('2', 'United Kingdom', 'GBP', 'British Pound', '£'),
       ('3', 'Canada', 'CAD', 'Canadian Dollar', 'C$'),
       ('4', 'Australia', 'AUD', 'Australian Dollar', 'A$'),
       ('5', 'Japan', 'JPY', 'Japanese Yen', '¥'),
       ('6', 'India', 'INR', 'Indian Rupee', '₹'),
       ('7', 'China', 'CNY', 'Chinese Yuan', '¥'),
       ('8', 'Russia', 'RUB', 'Russian Ruble', '₽'),
       ('9', 'South Korea', 'KRW', 'South Korean Won', '₩'),
       ('10', 'Mexico', 'MXN', 'Mexican Peso', 'M$'),
       ('11', 'Brazil', 'BRL', 'Brazilian Real', 'R$'),
       ('12', 'South Africa', 'ZAR', 'South African Rand', 'R'),
       ('13', 'New Zealand', 'NZD', 'New Zealand Dollar', 'N$'),
       ('14', 'Singapore', 'SGD', 'Singapore', 'S$');

INSERT INTO value_test_table (id, local_date_time, local_date, local_time, instant_time, zoned_date_time)
VALUES ('1', '2022-04-30 10:00:00', '2022-04-30', '10:00:00', '2022-04-30 10:00:00', '2022-04-30 10:00:00');



