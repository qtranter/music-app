package com.audiomack.model

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Column.ConflictAction
import com.activeandroid.annotation.Table
import com.activeandroid.query.Delete
import com.activeandroid.query.Select

@Table(name = "playlist_tracks")
class AMPlaylistTracks : Model {

    @Column(
        name = "playlist_id",
        uniqueGroups = ["group1"],
        onUniqueConflicts = [ConflictAction.REPLACE]
    )
    private var playlistId: String? = null

    @Column(
        name = "track_id",
        uniqueGroups = ["group1"],
        onUniqueConflicts = [ConflictAction.REPLACE]
    )
    var trackId: String? = null

    @Column(name = "number")
    private var number: Int = 0

    constructor()

    constructor(playlistId: String, trackId: String, number: Int) {
        this.playlistId = playlistId
        this.trackId = trackId
        this.number = number
    }

    companion object {

        @JvmStatic
        fun deletePlaylist(playlistId: String) {
            Delete()
                .from(AMPlaylistTracks::class.java)
                .where("playlist_id = ?", playlistId)
                .execute<AMPlaylistTracks>()
        }

        fun playlistsThatContain(trackId: String): List<String?> {
            return Select()
                .from(AMPlaylistTracks::class.java)
                .where("track_id = ?", trackId)
                .execute<AMPlaylistTracks>()
                .map { it.playlistId }
        }

        @JvmStatic
        fun tracksForPlaylist(playlistId: String): List<AMPlaylistTracks> {
            return Select()
                .from(AMPlaylistTracks::class.java)
                .where("playlist_id = ?", playlistId)
                .orderBy("number ASC")
                .execute()
        }

        fun savePlaylist(playlist: AMResultItem) {
            deletePlaylist(playlist.getItemId())
            val index = 0
            for (track in playlist.getTracks() ?: emptyList()) {
                AMPlaylistTracks(playlist.getItemId(), track.getItemId(), index).save()
            }
        }
    }
}
