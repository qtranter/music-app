ALTER TABLE items ADD COLUMN repost_artist_name TEXT;
ALTER TABLE items ADD COLUMN repost_timestamp INTEGER;
ALTER TABLE items ADD COLUMN playlist TEXT;
ALTER TABLE items ADD COLUMN offline_toast_shown BOOLEAN;
ALTER TABLE bookmarked_items ADD COLUMN repost_artist_name TEXT;
ALTER TABLE bookmarked_items ADD COLUMN repost_timestamp INTEGER;
ALTER TABLE bookmarked_items ADD COLUMN playlist TEXT;
ALTER TABLE bookmarked_items ADD COLUMN offline_toast_shown BOOLEAN;