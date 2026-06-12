ALTER TABLE finance_transaction
    ADD COLUMN paid_at TIMESTAMP(6) NULL;

CREATE TABLE finance_recurring_occurrence_payment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recurring_id BIGINT NULL,
    legacy_transaction_id BIGINT NULL,
    occurrence_date DATE NOT NULL,
    paid_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_occ_payment_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_occ_payment_recurring FOREIGN KEY (recurring_id) REFERENCES finance_recurring_transaction (id) ON DELETE CASCADE,
    CONSTRAINT fk_occ_payment_legacy FOREIGN KEY (legacy_transaction_id) REFERENCES finance_transaction (id) ON DELETE CASCADE,
    CONSTRAINT chk_occ_payment_source CHECK (recurring_id IS NOT NULL OR legacy_transaction_id IS NOT NULL)
);

CREATE UNIQUE INDEX uk_occ_payment_recurring_date
    ON finance_recurring_occurrence_payment (user_id, recurring_id, occurrence_date)
    WHERE recurring_id IS NOT NULL;

CREATE UNIQUE INDEX uk_occ_payment_legacy_date
    ON finance_recurring_occurrence_payment (user_id, legacy_transaction_id, occurrence_date)
    WHERE legacy_transaction_id IS NOT NULL;

CREATE INDEX idx_occ_payment_user_date ON finance_recurring_occurrence_payment (user_id, occurrence_date);
