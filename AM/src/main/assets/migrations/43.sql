ALTER TABLE items ADD COLUMN premium_download TEXT;
ALTER TABLE items ADD COLUMN download_date DATE;
ALTER TABLE items ADD COLUMN frozen BOOLEAN DEFAULT 0;
ALTER TABLE bookmarked_items ADD COLUMN premium_download TEXT;
ALTER TABLE bookmarked_items ADD COLUMN download_date DATE;
ALTER TABLE bookmarked_items ADD COLUMN frozen BOOLEAN DEFAULT 0;
