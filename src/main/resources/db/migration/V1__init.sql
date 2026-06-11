CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    parent_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE SET NULL
);

CREATE INDEX idx_category_user ON category (user_id);

CREATE TABLE finance_transaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    description VARCHAR(1024) NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT
);

CREATE INDEX idx_tx_user_occurred ON finance_transaction (user_id, occurred_at);
CREATE INDEX idx_tx_category ON finance_transaction (category_id);
