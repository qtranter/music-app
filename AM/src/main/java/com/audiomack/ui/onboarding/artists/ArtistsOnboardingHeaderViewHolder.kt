package com.audiomack.ui.onboarding.artists

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString

class ArtistsOnboardingHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvMessage = itemView.findViewById<TextView>(R.id.tvMessage)

    fun setup() {
        tvMessage.text = tvMessage.context.spannableString(
            fullString = tvMessage.context.getString(R.string.artists_onboarding_message),
            highlightedStrings = listOf(tvMessage.context.getString(R.string.artists_onboarding_message_highlighted)),
            highlightedColor = tvMessage.context.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_bold
        )
    }
}
