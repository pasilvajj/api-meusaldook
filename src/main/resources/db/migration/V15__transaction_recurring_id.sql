ALTER TABLE finance_transaction
    ADD COLUMN recurring_id BIGINT NULL
        REFERENCES finance_recurring_transaction (id) ON DELETE SET NULL;

UPDATE finance_transaction t
SET recurring_id = p.recurring_id
FROM finance_recurring_occurrence_payment p
WHERE p.materialized_transaction_id = t.id
  AND p.recurring_id IS NOT NULL;
