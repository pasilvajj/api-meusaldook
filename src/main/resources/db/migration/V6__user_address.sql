CREATE TABLE user_address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    street VARCHAR(200) NULL,
    number VARCHAR(32) NULL,
    complement VARCHAR(120) NULL,
    postal_code VARCHAR(16) NULL,
    city VARCHAR(120) NULL,
    state_code VARCHAR(2) NULL,
    CONSTRAINT uk_user_address_user UNIQUE (user_id),
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

INSERT INTO user_address (user_id, street, number, complement, postal_code, city, state_code)
SELECT id,
       NULLIF(TRIM(address_street), ''),
       NULLIF(TRIM(address_number), ''),
       NULLIF(TRIM(address_complement), ''),
       NULLIF(TRIM(postal_code), ''),
       NULLIF(TRIM(city), ''),
       NULLIF(TRIM(state_code), '')
FROM app_user
WHERE (address_street IS NOT NULL AND TRIM(address_street) <> '')
   OR (address_number IS NOT NULL AND TRIM(address_number) <> '')
   OR (address_complement IS NOT NULL AND TRIM(address_complement) <> '')
   OR (postal_code IS NOT NULL AND TRIM(postal_code) <> '')
   OR (city IS NOT NULL AND TRIM(city) <> '')
   OR (state_code IS NOT NULL AND TRIM(state_code) <> '');

ALTER TABLE app_user
    DROP COLUMN address_street,
    DROP COLUMN address_number,
    DROP COLUMN address_complement,
    DROP COLUMN postal_code,
    DROP COLUMN city,
    DROP COLUMN state_code;
