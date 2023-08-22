ALTER TABLE items ADD COLUMN uploader_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE items ADD COLUMN uploader_tastemaker BOOLEAN DEFAULT FALSE;
ALTER TABLE items ADD COLUMN uploader_image TEXT;
ALTER TABLE items ADD COLUMN uploader_followers INTEGER;
ALTER TABLE bookmarked_items ADD COLUMN uploader_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE bookmarked_items ADD COLUMN uploader_tastemaker BOOLEAN DEFAULT FALSE;
ALTER TABLE bookmarked_items ADD COLUMN uploader_image TEXT;
ALTER TABLE bookmarked_items ADD COLUMN uploader_followers INTEGER;