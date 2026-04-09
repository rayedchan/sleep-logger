ALTER TABLE sleep_logs
ADD CONSTRAINT check_sleep_duration_limit
CHECK (wake_time > bed_time AND (wake_time - bed_time) <= INTERVAL '24 hours');