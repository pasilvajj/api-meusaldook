CREATE TABLE finance_recurring_transaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    description VARCHAR(1024) NULL,
    start_at TIMESTAMP(6) NOT NULL,
    periodicity VARCHAR(32) NOT NULL,
    every_n INTEGER NOT NULL DEFAULT 1,
    max_occurrences INTEGER NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recurring_account FOREIGN KEY (account_id) REFERENCES user_account (id) ON DELETE RESTRICT
);

CREATE INDEX idx_recurring_user_active ON finance_recurring_transaction (user_id, active);
