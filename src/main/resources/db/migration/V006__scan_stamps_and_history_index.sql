-- Library scan: a file whose size and last-modified time haven't changed since the last scan is not read again
-- (doc/performance-review.md, item 5.1). Null until the file's next scan.
ALTER TABLE track ADD COLUMN file_size BIGINT;
ALTER TABLE track ADD COLUMN file_modified BIGINT;      -- milliseconds since 1970-01-01 UTC

-- The History tab filters and orders plays by time (item 6.3).
CREATE INDEX playback_history_datetime_index ON playback_history (datetime);
