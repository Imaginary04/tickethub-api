CREATE TABLE users (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100),
    role           VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    provider       VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'ORGANIZER', 'ADMIN')),
    CONSTRAINT chk_users_provider
        CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_users_local_has_password
        CHECK (provider <> 'LOCAL' OR password_hash IS NOT NULL)
);

-- One account per email, case-insensitive (Rahul@x.com = rahul@x.com)
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email));

-- One account per Google user
CREATE UNIQUE INDEX uq_users_provider_id ON users (provider, provider_id)
    WHERE provider_id IS NOT NULL;