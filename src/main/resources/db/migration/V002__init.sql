CREATE TABLE artist
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR,
    icon VARBINARY
);

CREATE INDEX artist_name_index ON artist (name);

CREATE TABLE album
(
    id         SERIAL PRIMARY KEY,
    fk_artist  INTEGER,
    name       VARCHAR,
    image      VARBINARY,
    image_type TINYINT
);

CREATE INDEX album_name_index ON album (name);
CREATE INDEX album_artist_index ON album (fk_artist);

CREATE TABLE track
(
    id                SERIAL PRIMARY KEY,
    fk_album          INTEGER   NOT NULL,
    title             VARCHAR   NOT NULL,
    track_number      INTEGER,
    release_date      VARCHAR,
    disk_number       INTEGER,
    audio_sample_rate INTEGER,
    genre             VARCHAR,
    composer          VARCHAR,
    file_location     VARCHAR   NOT NULL,
    file_hash         VARCHAR   NOT NULL,
    scanned_at        TIMESTAMP NOT NULL,
    duration          BIGINT,
    status            VARCHAR,
    lyrics            VARCHAR
);

CREATE TABLE track_overrides_history
(
    id          SERIAL PRIMARY KEY,
    track_id    INTEGER   NOT NULL REFERENCES track (id) ON DELETE CASCADE,
    field       TEXT      NOT NULL,
    old_value   TEXT,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_overrides_track_id ON track_overrides_history (track_id);

CREATE INDEX track_title_index ON track (title);
CREATE INDEX track_album_index ON track (fk_album);

-- STATUS = ACTIVE, IGNORED, DELETED
CREATE TABLE playback_history
(
    id       SERIAL PRIMARY KEY,
    datetime TIMESTAMP WITH TIME ZONE,
    fk_track INTEGER
);

CREATE INDEX playback_history_track_index ON playback_history (fk_track);


CREATE TABLE pending_action
(
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(255) NOT NULL,
    payload         VARBINARY,
    status          VARCHAR(30)  NOT NULL,
    retry_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL,
    last_error      TEXT
);