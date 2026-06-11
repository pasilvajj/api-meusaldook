CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    public_key VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    active BOOLEAN NOT NULL,
    initial_balance_date DATE NOT NULL,
    initial_balance_amount DECIMAL(19, 4) NOT NULL,
    saldo_creditor_debtor VARCHAR(16) NOT NULL,
    consider_balance_mode VARCHAR(32) NOT NULL,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_account_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_account_user_public UNIQUE (user_id, public_key)
);

CREATE INDEX idx_user_account_user ON user_account (user_id);

INSERT INTO user_account (
    user_id,
    public_key,
    name,
    account_type,
    currency,
    active,
    initial_balance_date,
    initial_balance_amount,
    saldo_creditor_debtor,
    consider_balance_mode,
    notes,
    created_at,
    updated_at
)
SELECT
    u.id,
    'principal',
    'Conta principal',
    'CHECKING',
    'BRL',
    TRUE,
    CURRENT_DATE,
    0.0000,
    'CREDITOR',
    'IMMEDIATE',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM app_user u
WHERE NOT EXISTS (SELECT 1 FROM user_account a WHERE a.user_id = u.id);
