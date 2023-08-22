package com.audiomack.ui.browse.world.list

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.model.WorldArticle

class WorldAdapter(
    private val onClickListener: (String) -> Unit
) : PagingDataAdapter<WorldArticle, RecyclerView.ViewHolder>(ARTICLE_COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val article = getItem(position)
        (holder as WorldArticleViewHolder).bind(article)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return WorldArticleViewHolder.create(parent, onClickListener)
    }

    companion object {
        private val ARTICLE_COMPARATOR = object : DiffUtil.ItemCallback<WorldArticle>() {
            override fun areItemsTheSame(oldItem: WorldArticle, newItem: WorldArticle) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WorldArticle, newItem: WorldArticle) =
                    oldItem == newItem
        }
    }
}
