package com.audiomack.data.comment

import com.audiomack.data.premium.PremiumRepository
import com.audiomack.model.AMComment
import com.audiomack.model.CommentSort
import com.audiomack.network.APIInterface
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class CommentRepositoryTest {

    @Mock
    private lateinit var api: APIInterface.CommentsInterface

    private lateinit var repository: CommentRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        repository = CommentRepository(api)
    }

    @After
    fun tearDown() {
        PremiumRepository.destroy()
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun getSingleComments() {
        val kind = "song"
        val id = "123"
        val uuid = "123asd"
        val threadId = "890iop"
        repository.getSingleComments(kind, id, uuid, threadId)
        verify(api, times(1)).getSingleComments(kind, id, uuid, threadId)
    }

    @Test
    fun getComments() {
        val kind = "song"
        val id = "123"
        val limit = "20"
        val offset = "0"
        val sort = CommentSort.Top.stringValue()
        repository.getComments(kind, id, limit, offset, sort)
        verify(api, times(1)).getComments(kind, id, limit, offset, sort)
    }

    @Test
    fun postComment() {
        val content = "Hello"
        val kind = "song"
        val id = "123"
        val thread = "890iop"
        repository.postComment(content, kind, id, thread)
        verify(api, times(1)).postComment(content, kind, id, thread)
    }

    @Test
    fun reportComment() {
        val kind = "song"
        val id = "123"
        val uuid = "123asd"
        val thread = "890iop"
        repository.reportComment(kind, id, uuid, thread)
        verify(api, times(1)).reportComment(kind, id, uuid, thread)
    }

    @Test
    fun deleteComment() {
        val kind = "song"
        val id = "123"
        val uuid = "123asd"
        val thread = "890iop"
        repository.deleteComment(kind, id, uuid, thread)
        verify(api, times(1)).deleteComment(kind, id, uuid, thread)
    }

    @Test
    fun getVoteStatus() {
        val kind = "song"
        val id = "123"
        repository.getVoteStatus(kind, id)
        verify(api, times(1)).getVoteStatus(kind, id)
    }

    @Test
    fun `voteComment - upvote and was already upvoted`() {
        val comment = AMComment().apply {
            upVoted = true
        }
        val isUpVote = true
        val kind = "song"
        val id = "123"
        repository.voteComment(comment, isUpVote, kind, id)
        verify(api, times(1)).voteComment(comment, null, kind, id)
    }

    @Test
    fun `voteComment - downvote and was already downvoted`() {
        val comment = AMComment().apply {
            downVoted = true
        }
        val isUpVote = false
        val kind = "song"
        val id = "123"
        repository.voteComment(comment, isUpVote, kind, id)
        verify(api, times(1)).voteComment(comment, null, kind, id)
    }

    @Test
    fun `voteComment - downvote and was upvoted`() {
        val comment = AMComment().apply {
            upVoted = true
        }
        val isUpVote = false
        val kind = "song"
        val id = "123"
        repository.voteComment(comment, isUpVote, kind, id)
        verify(api, times(1)).voteComment(comment, isUpVote, kind, id)
    }

    @Test
    fun `voteComment - upvote and was downvoted`() {
        val comment = AMComment().apply {
            downVoted = true
        }
        val isUpVote = true
        val kind = "song"
        val id = "123"
        repository.voteComment(comment, isUpVote, kind, id)
        verify(api, times(1)).voteComment(comment, isUpVote, kind, id)
    }
}
