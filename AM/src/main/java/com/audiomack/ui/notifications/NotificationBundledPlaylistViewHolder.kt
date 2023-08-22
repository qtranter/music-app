package com.audiomack.ui.notifications

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMNotification
import com.audiomack.utils.spannableString

class NotificationBundledPlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
    private val songsRecyclerView = view.findViewById<RecyclerView>(R.id.songsRecyclerView)

    fun setup(bundle: AMNotification.NotificationType.PlaylistUpdatedBundle) {
        val numberOfSongsString = String.format(
            tvTitle.resources.getString(if (bundle.songsCount == 1) R.string.notifications_playlists_bundle_number_song_single else R.string.notifications_playlists_bundle_number_song_plural),
            bundle.songsCount
        )
        val verbString = tvTitle.resources.getString(
            if (bundle.songsCount == 1) R.string.notifications_playlists_bundle_verb_singular else R.string.notifications_playlists_bundle_verb
        )
        val numberOfPlaylistsString = String.format(
            tvTitle.resources.getString(if (bundle.playlists.size == 1) R.string.notifications_playlists_bundle_number_singular else R.string.notifications_playlists_bundle_number_playlist),
            bundle.playlists.size
        )
        val suffixString = tvTitle.resources.getString(R.string.notifications_playlists_bundle_suffix)
        val fullString = "$numberOfSongsString $verbString $numberOfPlaylistsString $suffixString"

        tvTitle.text = tvTitle.context.spannableString(
            fullString = fullString,
            highlightedStrings = listOf(numberOfSongsString, numberOfPlaylistsString),
            highlightedColor = Color.WHITE,
            highlightedFont = R.font.opensans_bold
        )

        songsRecyclerView.adapter = NotificationBundledPlaylistSongsAdapter(bundle.songsImages) { itemView.callOnClick() }
        songsRecyclerView.setHasFixedSize(true)
    }
}

class NotificationBundledPlaylistSongsAdapter(private val images: List<String>, val clickHandler: () -> Unit) : RecyclerView.Adapter<NotificationBundledPlaylistSongsAdapter.ViewHolder>() {

    override fun getItemCount(): Int = images.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_notification_playlistupdate_song, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setup(images[position])
        holder.itemView.setOnClickListener { clickHandler() }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val songImageView = view.findViewById<ImageView>(R.id.songImageView)

        fun setup(image: String) {
            PicassoImageLoader.load(songImageView.context, image, songImageView)
        }
    }
}
