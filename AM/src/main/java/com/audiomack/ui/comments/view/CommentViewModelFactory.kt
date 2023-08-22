package com.audiomack.ui.comments.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.MixpanelSource

class CommentViewModelFactory(
    private val mode: CommentsFragment.Mode,
    private val mixpanelSource: MixpanelSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CommentsViewModel(mode, mixpanelSource) as T
    }
}
