ALTER TABLE user_account
    ADD COLUMN credit_card_due_day SMALLINT NULL,
    ADD COLUMN credit_card_next_invoice_date DATE NULL,
    ADD COLUMN credit_card_closing_days_before_due SMALLINT NULL;
