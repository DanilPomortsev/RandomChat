--liquibase formatted sql

--changeset user-service:002-create-user-avatars
CREATE TABLE user_avatars (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    url text NOT NULL,
    status text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_avatars_url_not_blank CHECK (char_length(btrim(url)) > 0),
    CONSTRAINT user_avatars_status_check CHECK (status IN ('validated', 'validation_in_progress', 'discarded'))
);

CREATE INDEX user_avatars_user_id_idx ON user_avatars (user_id);
