ALTER TABLE finance_recurring_occurrence_payment
    ADD COLUMN materialized_transaction_id BIGINT NULL
        REFERENCES finance_transaction (id) ON DELETE SET NULL;

DO $$
DECLARE
    r RECORD;
    tx_id BIGINT;
    occ_at TIMESTAMP(6);
BEGIN
    FOR r IN
        SELECT p.id AS payment_id,
               p.user_id,
               p.occurrence_date,
               p.paid_at,
               COALESCE(rec.category_id, lt.category_id) AS category_id,
               COALESCE(rec.account_id, lt.account_id) AS account_id,
               COALESCE(rec.amount, lt.amount) AS amount,
               COALESCE(rec.kind, lt.kind) AS kind,
               COALESCE(rec.description, lt.description) AS description,
               COALESCE(rec.start_at, lt.occurred_at) AS anchor_at
        FROM finance_recurring_occurrence_payment p
        LEFT JOIN finance_recurring_transaction rec ON rec.id = p.recurring_id
        LEFT JOIN finance_transaction lt ON lt.id = p.legacy_transaction_id
        WHERE p.materialized_transaction_id IS NULL
          AND COALESCE(rec.category_id, lt.category_id) IS NOT NULL
    LOOP
        occ_at := r.occurrence_date::timestamp + r.anchor_at::time;
        INSERT INTO finance_transaction (
            user_id,
            category_id,
            account_id,
            amount,
            kind,
            description,
            occurred_at,
            created_at,
            show_in_payables,
            paid_at
        ) VALUES (
            r.user_id,
            r.category_id,
            r.account_id,
            r.amount,
            r.kind,
            r.description,
            occ_at,
            r.paid_at,
            FALSE,
            r.paid_at
        ) RETURNING id INTO tx_id;

        UPDATE finance_recurring_occurrence_payment
        SET materialized_transaction_id = tx_id
        WHERE id = r.payment_id;
    END LOOP;
END $$;
