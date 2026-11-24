-- A cover set by hand in the tag editor is not replaced by the covers in the files on a rescan
-- (doc/performance-review.md, item 6.5).
ALTER TABLE album ADD COLUMN image_overridden BOOLEAN DEFAULT FALSE NOT NULL;

-- Small JPEG copies of album covers for lists and grids, so the full-size cover isn't decoded for a 48 px row
-- (item 2.1). source_md5 is the MD5 of the cover a thumbnail was made from: when the cover changes, the
-- thumbnail no longer matches and is made again.
CREATE TABLE album_cover_thumb
(
    album_id   INTEGER     NOT NULL REFERENCES album (id) ON DELETE CASCADE,
    px         SMALLINT    NOT NULL,
    source_md5 VARCHAR(32) NOT NULL,
    jpeg       VARBINARY   NOT NULL,
    PRIMARY KEY (album_id, px)
);
