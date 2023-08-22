package com.audiomack.ui.comments.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audiomack.model.AMResultItem

class AddCommentViewModelFactory(
    private val entity: AMResultItem?,
    var threadId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddCommentViewModel(entity, threadId) as T
    }
}
