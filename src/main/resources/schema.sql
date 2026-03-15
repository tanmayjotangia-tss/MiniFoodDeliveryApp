CREATE TABLE IF NOT EXISTS users (
    id       VARCHAR(36)  PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    phone    VARCHAR(20)  NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(30)  NOT NULL
);

CREATE TABLE IF NOT EXISTS admins (
    user_id VARCHAR(36) PRIMARY KEY
            REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS customers (
    user_id                  VARCHAR(36) PRIMARY KEY
                             REFERENCES users(id) ON DELETE CASCADE,
    address                  TEXT,
    notification_preferences VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS delivery_partners (
    user_id              VARCHAR(36)   PRIMARY KEY
                         REFERENCES users(id) ON DELETE CASCADE,
    available            BOOLEAN       NOT NULL DEFAULT TRUE,
    basic_pay            NUMERIC(10,2) NOT NULL DEFAULT 5000,
    incentive_percentage NUMERIC(5,2)  NOT NULL DEFAULT 5
);

CREATE TABLE IF NOT EXISTS notifications (
    id         VARCHAR(36) PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    is_read    BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS menus (
    id              VARCHAR(36)  PRIMARY KEY,
    restaurant_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS menu_categories (
    id                 VARCHAR(36)  PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    menu_id            VARCHAR(36)  NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    parent_category_id VARCHAR(36)  REFERENCES menu_categories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS menu_items (
    id          VARCHAR(36)   PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    price       NUMERIC(10,2) NOT NULL,
    category_id VARCHAR(36)   NOT NULL
                REFERENCES menu_categories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS discount_slabs (
    id         BIGINT        PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    threshold  NUMERIC(10,2) NOT NULL UNIQUE,
    percentage NUMERIC(5,2)  NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id                  VARCHAR(36)   PRIMARY KEY,
    customer_id         VARCHAR(36)   NOT NULL,
    customer_name       VARCHAR(255)  NOT NULL,
    delivery_partner_id VARCHAR(36),
    status              VARCHAR(30)   NOT NULL,
    payment_mode        VARCHAR(20),
    discount            NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT        PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    order_id     VARCHAR(36)   NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id VARCHAR(36),
    item_name    VARCHAR(255)  NOT NULL,
    item_price   NUMERIC(10,2) NOT NULL,
    quantity     INT           NOT NULL
);

CREATE TABLE IF NOT EXISTS carts (
    id          VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL UNIQUE
                REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart_items (
    id           BIGINT      PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    cart_id      VARCHAR(36) NOT NULL REFERENCES carts(id)      ON DELETE CASCADE,
    menu_item_id VARCHAR(36) NOT NULL REFERENCES menu_items(id)  ON DELETE CASCADE,
    quantity     INT         NOT NULL,
    UNIQUE (cart_id, menu_item_id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user  ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_menu_cats_menu      ON menu_categories(menu_id);
CREATE INDEX IF NOT EXISTS idx_menu_cats_parent    ON menu_categories(parent_category_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_category ON menu_items(category_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer     ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_partner      ON orders(delivery_partner_id);
CREATE INDEX IF NOT EXISTS idx_orders_created      ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_cart     ON cart_items(cart_id);