CREATE TYPE sleep_quality AS ENUM ('BAD','OK','GOOD');

CREATE TABLE sleep_logs (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    log_date DATE NOT NULL DEFAULT CURRENT_DATE,
    bed_time TIMESTAMPTZ NOT NULL,
    wake_time TIMESTAMPTZ NOT NULL,
    mood sleep_quality NOT NULL,
    total_duration INTERVAL GENERATED ALWAYS AS (wake_time - bed_time) STORED,
    CONSTRAINT check_wake_bed_times CHECK (wake_time > bed_time)
);

CREATE INDEX idx_sleep_logs_user_id ON sleep_logs(user_id);