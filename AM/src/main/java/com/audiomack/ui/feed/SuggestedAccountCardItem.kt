package com.audiomack.ui.feed

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMArtist
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.drawableCompat
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import java.lang.NumberFormatException
import kotlinx.android.synthetic.main.item_account.view.buttonFollow
import kotlinx.android.synthetic.main.item_account.view.imageView
import kotlinx.android.synthetic.main.item_account.view.imageViewBadge
import kotlinx.android.synthetic.main.item_account.view.tvArtist
import kotlinx.android.synthetic.main.item_account.view.tvFollowers

enum class LayoutType {
    Horizontal, Grid
}

class SuggestedAccountCardItem(
    val artist: AMArtist,
    private val layoutType: LayoutType,
    private val onFollowTapped: (artist: AMArtist) -> Unit,
    private val onItemTapped: (artist: AMArtist) -> Unit
) : Item() {

    override fun getLayout() = R.layout.item_account

    override fun getId(): Long {
        return try {
            artist.artistId?.toLong() ?: 0
        } catch (exception: NumberFormatException) {
            0
        }
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

        with(viewHolder.itemView) {
            when (layoutType) {
                LayoutType.Horizontal -> {
                    layoutParams.width = RecyclerView.LayoutParams.WRAP_CONTENT
                    (layoutParams as RecyclerView.LayoutParams).apply {
                        bottomMargin = 0
                        marginEnd = context.convertDpToPixel(16F)
                    }
                }
                LayoutType.Grid -> {
                    layoutParams.width = RecyclerView.LayoutParams.MATCH_PARENT
                    (layoutParams as RecyclerView.LayoutParams).apply {
                        bottomMargin = context.convertDpToPixel(20F)
                        marginEnd = 0
                    }
                }
            }
            tvArtist.text = artist.name
            when {
                artist.isVerified -> {
                    imageViewBadge.setImageDrawable(imageViewBadge.context.drawableCompat(R.drawable.ic_verified))
                    imageViewBadge.isVisible = true
                }
                artist.isTastemaker -> {
                    imageViewBadge.setImageDrawable(imageViewBadge.context.drawableCompat(R.drawable.ic_tastemaker))
                    imageViewBadge.isVisible = true
                }
                artist.isAuthenticated -> {
                    imageViewBadge.setImageDrawable(imageViewBadge.context.drawableCompat(R.drawable.ic_authenticated))
                    imageViewBadge.isVisible = true
                }
                else -> {
                    imageViewBadge.setImageDrawable(null)
                    imageViewBadge.isVisible = false
                }
            }
            tvFollowers.text = "${artist.followersShort} ${tvFollowers.context.getString(R.string.artist_followers)}"
            PicassoImageLoader.load(
                imageView.context,
                artist.smallImage,
                imageView
            )
            buttonFollow.setOnClickListener {
                onFollowTapped(artist)
            }
            setOnClickListener {
                onItemTapped(artist)
            }
        }
    }
}
