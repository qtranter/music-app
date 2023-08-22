CREATE INDEX IF NOT EXISTS index_items_parent_id ON items(parent_id);
CREATE INDEX IF NOT EXISTS index_items_album_track_downloaded_as_single ON items(album_track_downloaded_as_single);