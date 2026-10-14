--liquibase formatted sql

--changeset user-service:003-create-queue-tasks
CREATE TABLE queue_tasks (
    id                bigserial PRIMARY KEY,
    queue_name        text NOT NULL,
    payload           text,
    created_at        timestamptz NOT NULL DEFAULT now(),
    next_process_at   timestamptz NOT NULL DEFAULT now(),
    attempt           integer NOT NULL DEFAULT 0,
    reenqueue_attempt integer NOT NULL DEFAULT 0,
    total_attempt     integer NOT NULL DEFAULT 0
) WITH (
    fillfactor = 30,
    autovacuum_vacuum_cost_delay = 5,
    autovacuum_vacuum_cost_limit = 500,
    autovacuum_vacuum_scale_factor = 0.0001
);

CREATE INDEX queue_tasks_name_time_desc_idx
    ON queue_tasks USING btree (queue_name, next_process_at, id DESC)
    WITH (fillfactor = 30);
