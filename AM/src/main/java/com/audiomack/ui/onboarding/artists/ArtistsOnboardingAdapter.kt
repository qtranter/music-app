package com.audiomack.ui.onboarding.artists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.model.OnboardingArtist

class ArtistsOnboardingAdapter(val tapItem: (Int) -> Unit, val tapFooter: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeHeader = 0
    private val typeItem = 1
    private val typeFooter = 2

    private var objects: List<OnboardingArtist> = emptyList()
    private var selectedPosition: Int? = null

    fun updateData(newObjects: List<OnboardingArtist>) {
        objects = newObjects
        notifyDataSetChanged()
    }

    fun updateSelection(position: Int?) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> typeHeader
            itemCount - 1 -> typeFooter
            else -> typeItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeHeader -> ArtistsOnboardingHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.row_artist_onboarding_header,
                    parent,
                    false
                )
            )
            typeItem -> ArtistsOnboardingItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.row_artist_onboarding_item,
                    parent,
                    false
                )
            )
            else -> EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_artist_onboarding_footer, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return if (objects.isEmpty()) 1 else 2 + objects.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ArtistsOnboardingHeaderViewHolder -> {
                holder.setup()
                holder.itemView.setOnClickListener(null)
            }
            is ArtistsOnboardingItemViewHolder -> {
                holder.setup(objects[position - 1], selectedPosition == position - 1)
                holder.itemView.setOnClickListener { tapItem(holder.adapterPosition - 1) }
            }
            is EmptyViewHolder -> {
                holder.itemView.setOnClickListener { tapFooter() }
            }
        }
    }
}
