ALTER TABLE items ADD COLUMN album_release_date INTEGER DEFAULT 0;
ALTER TABLE items ADD COLUMN duration INTEGER DEFAULT 0;
ALTER TABLE items ADD COLUMN uploader_authenticated BOOLEAN DEFAULT FALSE;
ALTER TABLE bookmarked_items ADD COLUMN song_release_date INTEGER DEFAULT 0;
ALTER TABLE bookmarked_items ADD COLUMN duration INTEGER DEFAULT 0;
ALTER TABLE bookmarked_items ADD COLUMN uploader_authenticated BOOLEAN DEFAULT FALSE;
ALTER TABLE artists ADD COLUMN authenticated boolean DEFAULT FALSE;