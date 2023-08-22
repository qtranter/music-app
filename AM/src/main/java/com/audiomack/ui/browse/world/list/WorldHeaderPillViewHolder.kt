package com.audiomack.ui.browse.world.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.views.AMCustomFontTextView

class WorldHeaderPillViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val pillTextView: AMCustomFontTextView = view.findViewById(R.id.tvPill)

    fun bind(item: WorldFilterItem) {
        pillTextView.text = item.page.title
        pillTextView.background = pillTextView.context.drawableCompat(
                if (item.selected) R.drawable.browse_genre_pill_selected else R.drawable.browse_genre_pill_normal
        )
    }

    companion object {
        fun create(parent: ViewGroup) =
            WorldHeaderPillViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_brose_genre_pill, parent, false))
    }
}
