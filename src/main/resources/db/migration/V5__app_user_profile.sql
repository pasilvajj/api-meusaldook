ALTER TABLE app_user
    ADD COLUMN gender CHAR(1) NULL,
    ADD COLUMN cpf VARCHAR(14) NULL,
    ADD COLUMN birth_date DATE NULL,
    ADD COLUMN address_street VARCHAR(200) NULL,
    ADD COLUMN address_number VARCHAR(32) NULL,
    ADD COLUMN address_complement VARCHAR(120) NULL,
    ADD COLUMN postal_code VARCHAR(16) NULL,
    ADD COLUMN city VARCHAR(120) NULL,
    ADD COLUMN state_code VARCHAR(2) NULL,
    ADD COLUMN phone VARCHAR(40) NULL;
