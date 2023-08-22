package com.audiomack.ui.album

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMGenre
import com.audiomack.model.AMResultItem
import com.audiomack.ui.player.maxi.uploader.TagsAdapter
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.buttonFollow
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.imageViewAvatar
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.recyclerViewTags
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.tagsSeparator
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.tvFollowers
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.tvTags
import kotlinx.android.synthetic.main.fragment_player_uploader_tags.view.tvUploader
import kotlinx.android.synthetic.main.row_collection_album_footer.view.layoutComments
import kotlinx.android.synthetic.main.row_collection_album_footer.view.layoutGenre
import kotlinx.android.synthetic.main.row_collection_album_footer.view.layoutTotalPlays
import kotlinx.android.synthetic.main.row_collection_album_footer.view.playerUploaderTagsLayout
import kotlinx.android.synthetic.main.row_collection_album_footer.view.tvComments
import kotlinx.android.synthetic.main.row_collection_album_footer.view.tvDate
import kotlinx.android.synthetic.main.row_collection_album_footer.view.tvDatePrefix
import kotlinx.android.synthetic.main.row_collection_album_footer.view.tvGenre
import kotlinx.android.synthetic.main.row_collection_album_footer.view.tvRuntime
import kotlinx.android.synthetic.main.row_collection_album_footer.view.tvTotalPlays
import kotlinx.android.synthetic.main.row_collection_album_footer.view.viewDate

class AlbumCollectionFooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    interface Listener {
        fun onCommentsClickListener()
        fun onFollowClickListener()
        fun onUploaderClickListener()
        fun onTagClickListener(tag: String)
    }

    fun setup(
        collection: AMResultItem,
        isArtistFollowed: Boolean,
        isFollowVisible: Boolean,
        listener: Listener
    ) {
        with(itemView) {
            tvComments.text = tvComments.context.getString(
                R.string.comments_count_template,
                collection.commentsShort
            )
            tvDatePrefix.text = context.getString(R.string.musicinfo_addedon)
            tvDate.text = collection.released
            viewDate.isVisible = tvDate.text.isNotBlank()
            tvGenre.text = AMGenre.fromApiValue(collection.genre).humanValue(itemView.context)
            tvTotalPlays.text = collection.playsExtended
            tvRuntime.text = getRuntimeDataString(collection)

            val isNotLocal = !collection.isLocal
            layoutComments.isVisible = isNotLocal
            layoutGenre.isVisible = isNotLocal
            layoutTotalPlays.isVisible = isNotLocal

            with(playerUploaderTagsLayout) {
                isVisible = isNotLocal
                tvFollowers.text = collection.uploaderFollowersExtended

                val avatar = collection.uploaderTinyImage
                if (avatar.isNullOrBlank()) {
                    imageViewAvatar.setImageResource(R.drawable.profile_placeholder)
                } else {
                    PicassoImageLoader.load(context, avatar, imageViewAvatar)
                }

                when {
                    collection.isUploaderVerified -> tvUploader.text =
                        tvUploader.spannableStringWithImageAtTheEnd(
                            collection.uploaderName,
                            R.drawable.ic_verified,
                            12
                        )
                    collection.isUploaderTastemaker -> tvUploader.text =
                        tvUploader.spannableStringWithImageAtTheEnd(
                            collection.uploaderName,
                            R.drawable.ic_tastemaker,
                            12
                        )
                    collection.isUploaderAuthenticated -> tvUploader.text =
                        tvUploader.spannableStringWithImageAtTheEnd(
                            collection.uploaderName,
                            R.drawable.ic_authenticated,
                            12
                        )
                    else -> tvUploader.text = collection.uploaderName
                }

                buttonFollow.isVisible = isFollowVisible
                buttonFollow.background = context.drawableCompat(
                    if (isArtistFollowed) R.drawable.profile_header_following_bg else R.drawable.profile_header_follow_bg
                )
                buttonFollow.text =
                    context.getString(if (isArtistFollowed) R.string.artistinfo_unfollow else R.string.artistinfo_follow)

                val tagsList = collection.tags.toList().filterNot { it == collection.genre }
                        .toMutableList()
                collection.genre?.let {
                    tagsList.add(0, it)
                }
                tvTags.isVisible = tagsList.isNotEmpty()
                tagsSeparator.isVisible = tagsList.isNotEmpty()
                with(recyclerViewTags) {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = TagsAdapter { listener.onTagClickListener(it) }.apply {
                        submitList(tagsList)
                    }
                    isVisible = tagsList.isNotEmpty()
                }
            }

            buttonFollow.setOnClickListener { listener.onFollowClickListener() }
            tvUploader.setOnClickListener { listener.onUploaderClickListener() }
            imageViewAvatar.setOnClickListener { listener.onUploaderClickListener() }
            tvFollowers.setOnClickListener { listener.onUploaderClickListener() }
            layoutComments.setOnClickListener { listener.onCommentsClickListener() }
        }
    }

    private fun getRuntimeDataString(collection: AMResultItem): String {
        val runtimeResId = R.string.musicinfo_runtime_value
        val totalInMinutes = buildString {
            append(totalDurationInMinutes(collection.tracks))
            if (hasPotentialExcessDuration(collection.tracks)) append("+")
        }
        return itemView.context.getString(
            runtimeResId,
            totalInMinutes,
            collection.tracks?.size ?: 0
        )
    }

    private fun hasPotentialExcessDuration(tracks: List<AMResultItem>?): Boolean {
        return tracks?.any { it.duration == 0L } ?: false
    }

    private fun totalDurationInMinutes(tracks: List<AMResultItem>?): Int {
        if (tracks.isNullOrEmpty()) {
            return 0
        }
        return (tracks.map { it.duration }.sum() / 60).toInt() // convert seconds to minutes
    }
}
