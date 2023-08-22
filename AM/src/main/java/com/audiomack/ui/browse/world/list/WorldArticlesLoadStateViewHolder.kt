package com.audiomack.ui.browse.world.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class WorldArticlesLoadStateViewHolder(
    view: View,
    private val retry: () -> Unit
) : RecyclerView.ViewHolder(view) {

    private val progressBar: ProgressBar = view.findViewById(R.id.progressbarLoadState)
    private val retryButton: Button = view.findViewById(R.id.buttonRetry)
    private val errorMsg: TextView = view.findViewById(R.id.tvErrorLoadState)

    fun bind(loadState: LoadState) {
        errorMsg.setText(R.string.noconnection_placeholder)
        retryButton.setText(R.string.noconnection_highlighted_placeholder)
        retryButton.setOnClickListener { retry.invoke() }

        progressBar.isVisible = loadState is LoadState.Loading
        retryButton.isVisible = loadState !is LoadState.Loading
        errorMsg.isVisible = loadState !is LoadState.Loading
    }

    companion object {
        fun create(parent: ViewGroup, retry: () -> Unit): WorldArticlesLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_load_state_footer_view, parent, false)
            return WorldArticlesLoadStateViewHolder(view, retry)
        }
    }
}
