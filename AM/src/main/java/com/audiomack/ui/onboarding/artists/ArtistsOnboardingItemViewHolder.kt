package com.audiomack.ui.onboarding.artists

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.OnboardingArtist

class ArtistsOnboardingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageViewArtist = itemView.findViewById<ImageView>(R.id.imageViewArtist)
    private val imageViewSelected = itemView.findViewById<ImageView>(R.id.imageViewSelected)
    private val tvArtist = itemView.findViewById<TextView>(R.id.tvArtist)

    fun setup(item: OnboardingArtist, selected: Boolean) {
        tvArtist.text = item.artist?.name
        PicassoImageLoader.load(imageViewArtist.context, if (!item.imageUrl.isNullOrEmpty()) item.imageUrl else item.artist?.mediumImage, imageViewArtist)
        imageViewSelected.visibility = if (selected) View.VISIBLE else View.INVISIBLE
    }
}
