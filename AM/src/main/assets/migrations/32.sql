ALTER TABLE items ADD COLUMN mixpanel_source TEXT;
ALTER TABLE bookmarked_items ADD COLUMN mixpanel_source TEXT;
ALTER TABLE artists ADD COLUMN reupsCount INTEGER DEFAULT 0;
ALTER TABLE artists ADD COLUMN pinnedCount INTEGER DEFAULT 0;