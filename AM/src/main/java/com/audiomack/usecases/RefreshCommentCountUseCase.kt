package com.audiomack.usecases

import com.audiomack.data.comment.CommentDataSource
import com.audiomack.data.comment.CommentRepository
import com.audiomack.model.AMResultItem
import io.reactivex.Completable

interface RefreshCommentCountUseCase {
    fun refresh(music: AMResultItem): Completable
}

class RefreshCommentCountUseCaseImpl(
    private val commentDataSource: CommentDataSource = CommentRepository()
) : RefreshCommentCountUseCase {

    override fun refresh(music: AMResultItem): Completable =
        if (music.isLocal) {
            Completable.never()
        } else {
            commentDataSource.getComments(music.typeForHighlightingAPI, music.itemId, "", "", "")
                .flatMapCompletable { response ->
                    Completable.create { emitter ->
                        val count = response.count
                        music.commentCount = count
                        music.persistCommentCount(count)
                        emitter.onComplete()
                    }
                }
        }
}
