CREATE TABLE artist
(
    id   INTEGER auto_increment PRIMARY KEY,
    name VARCHAR,
    icon VARBINARY
);

CREATE INDEX artist_name_index ON artist (name);

CREATE TABLE album
(
    id        INTEGER auto_increment PRIMARY KEY,
    fk_artist INTEGER,
    name      VARCHAR,
    image     VARBINARY,
    image_type TINYINT
);

CREATE INDEX album_name_index ON album (name);
CREATE INDEX album_artist_index ON album (fk_artist);

CREATE TABLE track
(
    id                INTEGER auto_increment PRIMARY KEY,
    fk_album          INTEGER,
    title             VARCHAR,
    track_number      INTEGER,
    release_date      VARCHAR,
    disk_number       INTEGER,
    audio_sample_rate INTEGER,
    genre             VARCHAR,
    composer          VARCHAR,
    file_location     VARCHAR,
    duration          BIGINT,
    status            VARCHAR,
    lyrics            VARCHAR
);

CREATE INDEX track_title_index ON track (title);
CREATE INDEX track_album_index ON track (fk_album);

-- STATUS = ACTIVE, IGNORED, DELETED
CREATE TABLE playback_history
(
    id       INTEGER auto_increment PRIMARY KEY,
    datetime TIMESTAMP WITH TIME ZONE,
    fk_track INTEGER
);

CREATE INDEX playback_history_track_index ON playback_history (fk_track);
