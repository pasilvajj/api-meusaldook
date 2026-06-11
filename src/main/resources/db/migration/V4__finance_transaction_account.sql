-- Liga cada transação a uma conta do utilizador (chave pública estável na API).
ALTER TABLE finance_transaction
    ADD COLUMN account_id BIGINT NULL;

UPDATE finance_transaction ft
SET account_id = ua.id
FROM user_account ua
WHERE ua.user_id = ft.user_id
  AND ua.public_key = 'principal'
  AND ft.account_id IS NULL;

ALTER TABLE finance_transaction
    ALTER COLUMN account_id SET NOT NULL,
    ADD CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES user_account (id) ON DELETE RESTRICT;

CREATE INDEX idx_tx_user_account_occurred ON finance_transaction (user_id, account_id, occurred_at);
