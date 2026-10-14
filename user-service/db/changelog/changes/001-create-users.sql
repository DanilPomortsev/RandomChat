--liquibase formatted sql

--changeset user-service:001-create-users
CREATE TABLE users (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email text NOT NULL,
    nickname varchar(40) NOT NULL,
    status text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_nickname_length CHECK (char_length(nickname) BETWEEN 1 AND 40),
    CONSTRAINT users_email_not_blank CHECK (char_length(btrim(email)) > 0),
    CONSTRAINT users_status_check CHECK (status IN ('active', 'blocked'))
);

CREATE UNIQUE INDEX users_email_lower_uidx ON users (lower(email));
