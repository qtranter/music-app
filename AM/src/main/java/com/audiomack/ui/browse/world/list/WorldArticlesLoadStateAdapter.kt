package com.audiomack.ui.browse.world.list

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter

class WorldArticlesLoadStateAdapter(private val retry: () -> Unit) :
        LoadStateAdapter<WorldArticlesLoadStateViewHolder>() {

    override fun onBindViewHolder(holder: WorldArticlesLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ) = WorldArticlesLoadStateViewHolder.create(parent, retry)
}
