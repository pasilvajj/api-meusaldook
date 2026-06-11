CREATE INDEX idx_tx_user_kind_occurred ON finance_transaction (user_id, kind, occurred_at);

CREATE INDEX idx_tx_user_account_kind_occurred ON finance_transaction (user_id, account_id, kind, occurred_at);

CREATE INDEX idx_category_user_kind ON category (user_id, kind);
