CREATE TABLE IF NOT EXISTS user_preferences
(
    pref_key   VARCHAR(255) NOT NULL PRIMARY KEY,
    pref_value VARCHAR(255)
);

MERGE INTO user_preferences (pref_key, pref_value) KEY (pref_key) VALUES ('window.x', '100'),
                                                                         ('window.y', '100'),
                                                                         ('window.width', '1920'),
                                                                         ('window.height', '1080'),
                                                                         ('window.maximized', 'false'),
                                                                         ('playback.volume', '30'),
                                                                         ('ui.split.artists', '0.26'),
                                                                         ('ui.split.playlist', '0.8');