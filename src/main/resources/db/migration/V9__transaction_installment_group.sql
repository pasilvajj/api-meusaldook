ALTER TABLE finance_transaction
    ADD COLUMN installment_group_id VARCHAR(36);

CREATE INDEX idx_finance_transaction_installment_group
    ON finance_transaction (user_id, installment_group_id)
    WHERE installment_group_id IS NOT NULL;
