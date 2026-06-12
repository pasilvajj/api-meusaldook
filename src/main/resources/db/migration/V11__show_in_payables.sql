ALTER TABLE finance_transaction
    ADD COLUMN show_in_payables BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE finance_recurring_transaction
    ADD COLUMN show_in_payables BOOLEAN NOT NULL DEFAULT FALSE;
