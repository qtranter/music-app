package com.audiomack.adapters.viewholders

import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.user.UserData
import com.audiomack.model.AMArtist
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.utils.spannableStringWithImageAtTheEnd

class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvFeatured: AppCompatTextView = view.findViewById(R.id.tvFeatured)
    private val tvArtist: AppCompatTextView = view.findViewById(R.id.tvArtist)
    private val tvSubtitle: AppCompatTextView = view.findViewById(R.id.tvSubtitle)
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val followButton: Button = view.findViewById(R.id.buttonFollow)
    private val imageViewChevron: ImageView = view.findViewById(R.id.imageViewChevron)
    private val divider: View = view.findViewById(R.id.divider)

    fun setup(
        account: AMArtist,
        featuredText: String?,
        featured: Boolean,
        showDivider: Boolean,
        listener: DataRecyclerViewAdapter.RecyclerViewListener,
        hideActions: Boolean = false
    ) {
        tvFeatured.setTextFuture(PrecomputedTextCompat.getTextFuture(featuredText ?: "", tvFeatured.textMetricsParamsCompat, null))
        tvFeatured.visibility = if (featuredText.isNullOrBlank()) View.GONE else View.VISIBLE
        itemView.setBackgroundColor(if (!featured) Color.TRANSPARENT else itemView.context.colorCompat(R.color.featured_music_highlight))
        val name =
            when {
                account.isVerified -> tvArtist.spannableStringWithImageAtTheEnd(
                    account.name,
                    R.drawable.ic_verified,
                    16
                )
                account.isTastemaker -> tvArtist.spannableStringWithImageAtTheEnd(
                    account.name,
                    R.drawable.ic_tastemaker,
                    16
                )
                account.isAuthenticated -> tvArtist.spannableStringWithImageAtTheEnd(
                    account.name,
                    R.drawable.ic_authenticated,
                    16
                )
                else -> account.name ?: ""
            }
        tvArtist.setTextFuture(PrecomputedTextCompat.getTextFuture(name, tvArtist.textMetricsParamsCompat, null))
        tvSubtitle.setTextFuture(PrecomputedTextCompat.getTextFuture("${account.followersExtended} ${tvSubtitle.resources.getString(R.string.artist_followers)}", tvSubtitle.textMetricsParamsCompat, null))
        PicassoImageLoader.load(imageView.context, account.smallImage, imageView)

        followButton.visibility = if (hideActions) View.GONE else View.VISIBLE
        val followed = UserData.isArtistFollowed(account.artistId)
        followButton.background = followButton.context.drawableCompat(if (followed) R.drawable.profile_header_following_bg else R.drawable.profile_header_follow_bg)
        followButton.text = followButton.context.getString(if (followed) R.string.artistinfo_unfollow else R.string.artistinfo_follow)

        imageViewChevron.visibility = if (hideActions) View.VISIBLE else View.GONE

        divider.visibility = if (showDivider) View.VISIBLE else View.GONE

        followButton.setOnClickListener { listener.onClickFollow(account) }

        itemView.setOnClickListener { listener.onClickItem(account) }
    }
}
