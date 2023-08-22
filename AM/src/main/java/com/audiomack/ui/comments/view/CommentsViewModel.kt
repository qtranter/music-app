package com.audiomack.ui.comments.view

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.ads.AdProvidersHelper
import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.comment.CommentDataSource
import com.audiomack.data.comment.CommentRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.player.PlayerDataSource
import com.audiomack.data.player.PlayerRepository
import com.audiomack.data.share.ShareManager
import com.audiomack.data.share.ShareManagerImpl
import com.audiomack.data.tracking.mixpanel.MixpanelButtonComment
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMComment
import com.audiomack.model.AMExpandComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMShowMoreComments
import com.audiomack.model.CommentMethod
import com.audiomack.model.CommentSort
import com.audiomack.model.EventCommentCountUpdated
import com.audiomack.model.EventLoginState
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.ui.common.Resource
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibility
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityData
import com.audiomack.ui.player.maxi.bottom.PlayerBottomVisibilityImpl
import com.audiomack.ui.player.maxi.bottom.playerTabCommentsIndex
import com.audiomack.utils.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.IOException
import org.greenrobot.eventbus.EventBus

class CommentsViewModel(
    private val mode: CommentsFragment.Mode,
    val mixpanelSource: MixpanelSource,
    playerDataSource: PlayerDataSource = PlayerRepository.getInstance(),
    private val userRepository: UserDataSource = UserRepository.getInstance(),
    private val commentDataSource: CommentDataSource = CommentRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val adsDataSource: AdsDataSource = AdProvidersHelper,
    val imageLoader: ImageLoader = PicassoImageLoader,
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val playerBottomVisibility: PlayerBottomVisibility = PlayerBottomVisibilityImpl.getInstance(),
    private val shareManager: ShareManager = ShareManagerImpl()
) : BaseViewModel(), CommentsAdapter.CommentsListener {

    val mixpanelButton = MixpanelButtonComment

    private var entity: AMResultItem? = null
    private var comment: AMComment? = null
    private var comments: ArrayList<AMComment> = ArrayList()

    private var lastEntityIdFetched: String? = null

    private var commentOptionSort = CommentSort.Top
    private val paginationLimit: Int = 20
    var isEndOfComments: Boolean = false

    private var pendingAction: CommentsPendingAction? = null

    val adsVisible: Boolean
        get() {
            return adsDataSource.adsVisible
        }

    val updateCommentListEvent = SingleLiveEvent<Pair<ArrayList<AMComment>, String?>>()
    val showLoadingEvent = SingleLiveEvent<Void>()
    val hideLoadingEvent = SingleLiveEvent<Void>()
    val showErrorToastEvent = SingleLiveEvent<Void>()
    val showAddCommentEvent = SingleLiveEvent<AMResultItem>()
    val showAddReplyEvent = SingleLiveEvent<Pair<AMResultItem, String>>()
    val showLoginAlertEvent = SingleLiveEvent<Void>()
    val showLoggedInEvent = SingleLiveEvent<LoginSignupSource>()
    val showReportAlertViewEvent = SingleLiveEvent<AMComment>()
    val showDeleteAlertViewEvent = SingleLiveEvent<AMComment>()
    val showSortViewEvent = SingleLiveEvent<CommentSort>()
    val showOptionsEvent = SingleLiveEvent<AMComment>()
    val showMoreCommentsEvent = SingleLiveEvent<AMShowMoreComments>()
    val showCommenterEvent = SingleLiveEvent<String>()
    val closeEvent = SingleLiveEvent<Void>()
    val closeOptionsEvent = SingleLiveEvent<Void>()
    val stopInfiniteScrollEvent = SingleLiveEvent<Void>()
    val expandCommentEvent = SingleLiveEvent<AMExpandComment>()
    val updateTitleEvent = SingleLiveEvent<Pair<String?, String?>>()
    val showViewAllEvent = SingleLiveEvent<Void>()
    val showLoadErrorToastEvent = SingleLiveEvent<Void>()
    val showConnectionErrorToastEvent = SingleLiveEvent<Void>()

    private val _commentCount = MutableLiveData<Int>()
    val commentCount: LiveData<Int> = _commentCount

    private val _avatar = MutableLiveData<String?>()
    val avatar: LiveData<String?> = _avatar

    private val _standaloneHeaderVisible = MutableLiveData<Boolean>()
    val standaloneHeaderVisible: LiveData<Boolean> = _standaloneHeaderVisible

    private val _playerHeaderVisible = MutableLiveData<Boolean>()
    val playerHeaderVisible: LiveData<Boolean> = _playerHeaderVisible

    private val _singleCommentModeVisible = MutableLiveData<Boolean>()
    val singleCommentModeVisible: LiveData<Boolean> = _singleCommentModeVisible

    private val _noDataPlaceholderVisible = MutableLiveData<Boolean>()
    val noDataPlaceholderVisible: LiveData<Boolean> = _noDataPlaceholderVisible

    private val _noConnectionPlaceholderVisible = MutableLiveData<Boolean>()
    val noConnectionPlaceholderVisible: LiveData<Boolean> = _noConnectionPlaceholderVisible

    private val _scrollViewNestedScrollEnabled = MutableLiveData<Boolean>()
    val scrollViewNestedScrollEnabled: LiveData<Boolean> get() = _scrollViewNestedScrollEnabled

    private val songObserver: Observer<Resource<AMResultItem>> = object : Observer<Resource<AMResultItem>> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(t: Resource<AMResultItem>) {
            when (t) {
                is Resource.Success -> t.data?.let {
                    entity = it
                    if (playerBottomVisibility.tabIndex == playerTabCommentsIndex && playerBottomVisibility.tabsVisible && entity?.itemId != lastEntityIdFetched) {
                        onRefreshTriggered()
                    }
                    updateHeaderVisibility()
                }
                is Resource.Loading -> {
                    t.data?.let {
                        entity = it
                    }
                    updateCommentList(ArrayList())
                    showLoadingEvent.call()
                    _noDataPlaceholderVisible.postValue(false)
                    _noConnectionPlaceholderVisible.postValue(false)
                }
                is Resource.Failure -> {
                    hideLoadingEvent.call()
                    _noConnectionPlaceholderVisible.postValue(true)
                }
            }
        }
    }

    private val visibilityObserver = object : Observer<PlayerBottomVisibilityData> {
        override fun onComplete() {}

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {}

        override fun onNext(data: PlayerBottomVisibilityData) {
            if (data.visibleTabIndex == playerTabCommentsIndex && entity?.itemId != lastEntityIdFetched) {
                onRefreshTriggered()
            }
            _scrollViewNestedScrollEnabled.postValue(data.reachedBottom)
        }
    }

    init {
        if (mode == CommentsFragment.Mode.Player) {
            playerDataSource.subscribeToSong(songObserver)
            playerBottomVisibility.subscribe(visibilityObserver)
        }
        userRepository.loginEvents
            .subscribe { onLoginStateChanged(it) }
            .composite()
        updateHeaderVisibility()
    }

    override fun onCleared() {
        super.onCleared()
        clearPendingActions()
    }

    private fun updateHeaderVisibility() {
        _standaloneHeaderVisible.postValue((mode == CommentsFragment.Mode.Standalone || mode == CommentsFragment.Mode.Single))
        _playerHeaderVisible.postValue(mode == CommentsFragment.Mode.Player)
        _singleCommentModeVisible.postValue(mode == CommentsFragment.Mode.Single)
    }

    private fun onLoginStateChanged(state: EventLoginState) {
        if (state == EventLoginState.LOGGED_IN) {
            resumePendingActions()
            _avatar.postValue(userRepository.avatar)
        } else {
            clearPendingActions()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        if (!userRepository.isLoggedIn()) {
            showLoginAlertEvent.call()
            return false
        }
        return true
    }

    fun onStartLoginTapped() {
        showLoggedInEvent.postValue(LoginSignupSource.Comment)
    }

    fun onCancelLoginTapped() {
        userRepository.onLoginCanceled()
        clearPendingActions()
    }

    private fun resumePendingActions() {
        when (val action = pendingAction ?: return) {
            is CommentsPendingAction.Add -> onWriteCommentTapped()
            is CommentsPendingAction.Report -> onCommentReportTapped(action.comment)
            is CommentsPendingAction.Delete -> onCommentDeleteTapped(action.comment)
            is CommentsPendingAction.Upvote -> onCommentUpVoteTapped(action.comment)
            is CommentsPendingAction.Downvote -> onCommentDownVoteTapped(action.comment)
            is CommentsPendingAction.Reply -> onCommentReplyTapped(action.comment)
            is CommentsPendingAction.UpvoteReply -> onReplyUpVoteTapped(action.comment, action.reply)
            is CommentsPendingAction.DownvoteReply -> onReplyDownVoteTapped(action.comment, action.reply)
        }
    }

    private fun clearPendingActions() {
        pendingAction = null
    }

    fun updateEntity(entity: AMResultItem) {
        this.entity = entity
    }

    fun updateCommentList(comments: ArrayList<AMComment>) {
        val entity = entity ?: return
        this.comments = comments
        updateCommentListEvent.postValue(Pair(comments, entity.uploaderSlug))
    }

    fun updateSingleComment(comment: AMComment) {
        val entity = entity ?: return
        this.comment = comment
        updateTitleEvent.postValue(Pair(entity.artist, entity.title))
    }

    fun updateCommentCount(count: Int) {
        val entity = entity ?: return
        eventBus.post(EventCommentCountUpdated(count, entity))
        _commentCount.postValue(count)
    }

    fun onCloseTapped() {
        closeEvent.call()
    }

    fun onRefreshTriggered() {
        if (mode == CommentsFragment.Mode.Single) loadSingleComment()
        else loadComments(0, true)
    }

    private fun loadVoteStatus() {

        val entity = entity ?: return

        if (!userRepository.isLoggedIn()) {
            hideLoadingEvent.call()
            updateCommentList(comments)
            return
        }

        compositeDisposable.add(
                commentDataSource.getVoteStatus(entity.type, entity.itemId)
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({ result ->

                            for (i in 0 until result.size) {

                                val voteStatus = result[i]

                                val filteredComments = comments.filter { filterComment ->
                                    filterComment.uuid == voteStatus.uuid
                                }

                                if (filteredComments.count() > 0) {
                                    val votedComment = filteredComments.first()
                                    val index = comments.indexOf(votedComment)
                                    if (voteStatus.isUpVote) {
                                        votedComment.upVoted = true
                                    } else {
                                        votedComment.downVoted = true
                                    }
                                    comments[index] = votedComment
                                }

                                val filteredParentComments = comments.filter { parentComment ->
                                    parentComment.uuid == voteStatus.thread
                                }

                                if (filteredParentComments.count() > 0) {

                                    val parentComment = filteredParentComments.first()

                                    val filteredChildren = parentComment.children.filter { filterComment ->
                                        filterComment.uuid == voteStatus.uuid
                                    }

                                    if (filteredChildren.count() > 0) {
                                        val childComment = filteredChildren.first()
                                        val index = comments.indexOf(parentComment)
                                        val indexReply = parentComment.children.indexOf(childComment)
                                        if (voteStatus.isUpVote) {
                                            childComment.upVoted = true
                                        } else {
                                            childComment.downVoted = true
                                        }
                                        parentComment.children[indexReply] = childComment
                                        comments[index] = parentComment
                                    }
                                }
                            }

                            hideLoadingEvent.call()
                            updateCommentList(comments)
                        }, {
                            hideLoadingEvent.call()
                            updateCommentList(comments)
                        }))
    }

    private fun loadComments(offset: Int, forcedRefresh: Boolean) {

        val entity = entity ?: run {
            updateCommentList(comments)
            updateCommentCount(comments.size)
            return
        }

        updateHeaderVisibility()

        if (offset == 0) {
            comments = ArrayList()
        }

        if (offset == 0 && forcedRefresh) {
            showLoadingEvent.call()
        }

        _noDataPlaceholderVisible.postValue(false)
        _noConnectionPlaceholderVisible.postValue(false)

        compositeDisposable.add(
                commentDataSource.getComments(entity.typeForHighlightingAPI, entity.itemId, paginationLimit.toString(), offset.toString(), commentOptionSort.stringValue())
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({ result ->

                            val filteredResults = result.list.filter {
                                (!it.deleted && it.commenter?.commentBanned != true) || it.children.isNotEmpty()
                            }

                            if (offset == 0) {
                                val count = result.count
                                updateEntityCount(count)
                                updateCommentCount(count)
                                if (count > result.list.count()) {
                                    isEndOfComments = false
                                }
                            }

                            val newResults = filteredResults.filter { !comments.contains(it) }

                            if (newResults.isNotEmpty()) {
                                comments.addAll(newResults)
                                loadVoteStatus()
                            } else {
                                hideLoadingEvent.call()
                                updateCommentList(comments)
                                isEndOfComments = offset != 0 || comments.isEmpty()
                            }

                            _noDataPlaceholderVisible.postValue(comments.isNullOrEmpty())

                            stopInfiniteScrollEvent.call()

                            lastEntityIdFetched = entity.itemId
                        }, {

                            hideLoadingEvent.call()

                            val isNetworkError = it is IOException

                            if (isNetworkError) {
                                showConnectionErrorToastEvent.call()
                            } else {
                                showLoadErrorToastEvent.call()
                            }

                            updateCommentList(comments)
                            updateCommentCount(entity.commentCount)
                            _noDataPlaceholderVisible.postValue(comments.isNullOrEmpty() && !isNetworkError)
                            _noConnectionPlaceholderVisible.postValue(isNetworkError)
                            _playerHeaderVisible.postValue(false)

                            stopInfiniteScrollEvent.call()
                        }))
    }

    private fun loadSingleComment() {

        val entity = entity ?: run {
            updateCommentList(comments)
            return
        }

        val comment = comment ?: run {
            updateCommentList(comments)
            return
        }

        val commentUuid = comment.uuid ?: run {
            updateCommentList(comments)
            return
        }

        _noDataPlaceholderVisible.postValue(false)
        _noConnectionPlaceholderVisible.postValue(false)

        compositeDisposable.add(
            commentDataSource.getSingleComments(entity.typeForHighlightingAPI, entity.itemId, commentUuid, comment.threadUuid)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ result ->

                    val filteredResults = result.list.filter {
                        (!it.deleted && it.commenter?.commentBanned != true) || it.children.isNotEmpty()
                    }

                    val newResults = filteredResults.filter { !comments.contains(it) }

                    if (newResults.isNotEmpty()) {
                        comments.addAll(newResults)
                        loadVoteStatus()
                    } else {
                        hideLoadingEvent.call()
                        updateCommentList(comments)
                    }

                    _noDataPlaceholderVisible.postValue(comments.isNullOrEmpty())

                    stopInfiniteScrollEvent.call()

                    lastEntityIdFetched = entity.itemId
                }, {

                    hideLoadingEvent.call()

                    val isNetworkError = it is IOException

                    if (isNetworkError) {
                        showConnectionErrorToastEvent.call()
                    } else {
                        showLoadErrorToastEvent.call()
                    }

                    updateCommentList(comments)
                    updateCommentCount(entity.commentCount)
                    _noDataPlaceholderVisible.postValue(comments.isNullOrEmpty() && !isNetworkError)
                    _noConnectionPlaceholderVisible.postValue(isNetworkError)
                    _playerHeaderVisible.postValue(false)

                    stopInfiniteScrollEvent.call()
                }))
    }

    private fun updateEntityCount(count: Int) {
        val entity = entity ?: return
        entity.commentCount = count
        eventBus.post(EventCommentCountUpdated(count, entity))
        entity.persistCommentCount(count)
    }

    private fun incrementEntityCountByOne() {
        val entity = entity ?: return
        val count = entity.commentCount
        updateEntityCount(count + 1)
    }

    private fun decrementEntityCountByOne() {
        val entity = entity ?: return
        val count = entity.commentCount
        updateEntityCount(count - 1)
    }

    fun updateCommentListWithComment(comment: AMComment) {
        val entity = entity ?: return

        if (comment.entityId != entity.itemId) {
            return
        }

        if (!comment.threadUuid.isNullOrEmpty()) {
            comments.firstOrNull { filterComment -> filterComment.uuid == comment.threadUuid }?.let { parentComment ->
                val index = comments.indexOf(parentComment)
                parentComment.children.add(comment)
                comments[index] = parentComment
            }
        } else {
            comments.add(0, comment)
        }

        incrementEntityCountByOne()
        updateCommentCount(entity.commentCount)
        updateCommentList(comments)
    }

    fun onWriteCommentTapped() {
        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.Add
            return
        }
        entity?.let { showAddCommentEvent.value = it }
    }

    fun onSortButtonTapped() {
        showSortViewEvent.postValue(commentOptionSort)
    }

    fun onViewAllTapped() {
        showViewAllEvent.call()
    }

    private fun showOptionsView(commentsItem: AMComment) {
        showOptionsEvent.postValue(commentsItem)
    }

    fun showReportAlertView(comment: AMComment) {
        closeOptionsEvent.call()
        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.Report(comment)
            return
        }
        showReportAlertViewEvent.postValue(comment)
    }

    fun showDeleteAlertView(comment: AMComment) {
        closeOptionsEvent.call()
        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.Delete(comment)
            return
        }
        showDeleteAlertViewEvent.postValue(comment)
    }

    fun onShareCommentTapped(activity: Activity?, comment: AMComment) {
        closeOptionsEvent.call()
        val entity = entity ?: return
        shareManager.shareCommentLink(activity, comment, entity, mixpanelSource, mixpanelButton)
    }

    fun onCommentDeleteTapped(comment: AMComment) {

        val entity = entity ?: return

        val id = comment.entityId
        val kind = comment.entityKind
        val uuid = comment.uuid
        val thread = comment.threadUuid

        if (id == null || kind == null || uuid == null) {
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
                commentDataSource.deleteComment(kind, id, uuid, thread)
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({

                            hideLoadingEvent.call()

                            if (!comment.threadUuid.isNullOrEmpty()) {
                                comments.firstOrNull { filterComment -> filterComment.uuid == comment.threadUuid }?.let { parentComment ->
                                    val parentIndex = comments.indexOf(parentComment)
                                    val index = parentComment.children.indexOf(comment)

                                    parentComment.children.removeAt(index)
                                    comments[parentIndex] = parentComment
                                }
                            } else {
                                val index = comments.indexOf(comment)
                                // Rule: show the deleted row only if the root comment having replies is deleted
                                if (comment.children.size > 0) {
                                    comment.deleted = true
                                    comments[index] = comment
                                } else {
                                    comments.removeAt(index)
                                }
                            }

                            decrementEntityCountByOne()
                            updateCommentCount(entity.commentCount)
                            updateCommentList(comments)
                        }, {

                            hideLoadingEvent.call()
                            showErrorToastEvent.call()
                        }))
    }

    fun onCommentReportTapped(comment: AMComment) {

        val id = comment.entityId
        val kind = comment.entityKind
        val uuid = comment.uuid
        val thread = comment.threadUuid

        if (id == null || kind == null || uuid == null) {
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
                commentDataSource.reportComment(kind, id, uuid, thread)
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe({

                            hideLoadingEvent.call()
                            mixpanelDataSource.trackCommentDetail(CommentMethod.Report, comment, entity)
                        }, {

                            hideLoadingEvent.call()
                            showErrorToastEvent.call()
                        }))
    }

    // CommentsListener

    override fun onCommenterTapped(comment: AMComment) {
        val slug = comment.commenter?.urlSlug?.trim()
        if (slug.isNullOrBlank()) {
            return
        }
        showCommenterEvent.postValue(slug)
    }

    override fun onCommentReplyTapped(comment: AMComment) {
        val entity = entity ?: return
        val id = comment.uuid ?: return
        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.Reply(comment)
            return
        }
        showAddReplyEvent.postValue(Pair(entity, id))
    }

    override fun onCommentUpVoteTapped(comment: AMComment) {
        val id = comment.entityId
        val kind = comment.entityKind

        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.Upvote(comment)
            return
        }

        if (id == null || kind == null) {
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
            commentDataSource.voteComment(comment, true, kind, id)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ commentVote ->

                    hideLoadingEvent.call()

                    val index = comments.indexOf(comment)

                    comment.upVotes = commentVote.upVotes
                    comment.downVotes = commentVote.downVotes
                    comment.voteTotal = commentVote.voteTotal

                    comment.downVoted = false
                    comment.upVoted = !comment.upVoted

                    comments[index] = comment
                    mixpanelDataSource.trackCommentDetail(CommentMethod.UpVote, comment, entity)
                    updateCommentList(comments)
                }, {

                    hideLoadingEvent.call()
                }))
    }

    override fun onCommentDownVoteTapped(comment: AMComment) {
        val id = comment.entityId
        val kind = comment.entityKind

        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.Downvote(comment)
            return
        }

        if (id == null || kind == null) {
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
            commentDataSource.voteComment(comment, false, kind, id)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ commentVote ->

                    hideLoadingEvent.call()

                    val index = comments.indexOf(comment)

                    comment.upVotes = commentVote.upVotes
                    comment.downVotes = commentVote.downVotes
                    comment.voteTotal = commentVote.voteTotal

                    comment.upVoted = false
                    comment.downVoted = !comment.downVoted

                    comments[index] = comment
                    mixpanelDataSource.trackCommentDetail(CommentMethod.DownVote, comment, entity)
                    updateCommentList(comments)
                }, {

                    hideLoadingEvent.call()
                }))
    }

    override fun onCommentActionTapped(comment: AMComment) {
        showOptionsView(comment)
    }

    override fun onReplyUpVoteTapped(parentComment: AMComment, reply: AMComment) {
        val id = reply.entityId
        val kind = reply.entityKind

        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.UpvoteReply(parentComment, reply)
            return
        }

        if (id == null || kind == null) {
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
            commentDataSource.voteComment(reply, true, kind, id)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ commentVote ->

                    hideLoadingEvent.call()

                    val parentIndex = comments.indexOf(parentComment)
                    if (parentIndex != -1) {
                        val index = parentComment.children.indexOf(reply)
                        if (index != -1) {
                            reply.upVotes = commentVote.upVotes
                            reply.downVotes = commentVote.downVotes
                            reply.voteTotal = commentVote.voteTotal
                            reply.upVoted = !reply.upVoted
                            reply.downVoted = false
                            parentComment.children[index] = reply
                            comments[parentIndex] = parentComment
                        }
                    }

                    mixpanelDataSource.trackCommentDetail(CommentMethod.UpVote, reply, entity)
                    updateCommentList(comments)
                }, {

                    hideLoadingEvent.call()
                    showErrorToastEvent.call()
                }))
    }

    override fun onReplyDownVoteTapped(parentComment: AMComment, reply: AMComment) {
        val id = reply.entityId
        val kind = reply.entityKind

        if (!isUserLoggedIn()) {
            pendingAction = CommentsPendingAction.DownvoteReply(parentComment, reply)
            return
        }

        if (id == null || kind == null) {
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
            commentDataSource.voteComment(reply, false, kind, id)
                .subscribeOn(schedulersProvider.io)
                .observeOn(schedulersProvider.main)
                .subscribe({ commentVote ->

                    hideLoadingEvent.call()

                    val parentIndex = comments.indexOf(parentComment)
                    if (parentIndex != -1) {
                        val index = parentComment.children.indexOf(reply)
                        if (index != -1) {
                            reply.upVotes = commentVote.upVotes
                            reply.downVotes = commentVote.downVotes
                            reply.voteTotal = commentVote.voteTotal
                            reply.upVoted = false
                            reply.downVoted = !reply.downVoted
                            parentComment.children[index] = reply
                            comments[parentIndex] = parentComment
                        }
                    }

                    mixpanelDataSource.trackCommentDetail(CommentMethod.DownVote, reply, entity)
                    updateCommentList(comments)
                }, {

                    hideLoadingEvent.call()
                    showErrorToastEvent.call()
                }))
    }

    override fun onReplyActionTapped(comment: AMComment) {
        showOptionsView(comment)
    }

    override fun onCommentViewMoreTapped(more: AMShowMoreComments) {
        showMoreCommentsEvent.postValue(more)
    }

    override fun onCommentExpandTapped(expand: AMExpandComment) {
        expandCommentEvent.postValue(expand)
    }

    fun onChangedSorting(sort: CommentSort) {
        closeOptionsEvent.call()
        commentOptionSort = sort
        loadComments(0, true)
    }

    fun onCreate() {
        if (mode == CommentsFragment.Mode.Single) loadSingleComment()
        else loadComments(0, true)
        _avatar.postValue(userRepository.avatar)
    }

    fun onLoadMore(itemCount: Int) {
        if (mode != CommentsFragment.Mode.Single) loadComments(itemCount, false)
    }

    fun onTitleClicked() {
        if (mode == CommentsFragment.Mode.Single) {
            showViewAllEvent.call()
        }
    }
}

sealed class CommentsPendingAction {
    object Add : CommentsPendingAction()
    data class Report(val comment: AMComment) : CommentsPendingAction()
    data class Delete(val comment: AMComment) : CommentsPendingAction()
    data class Upvote(val comment: AMComment) : CommentsPendingAction()
    data class Downvote(val comment: AMComment) : CommentsPendingAction()
    data class Reply(val comment: AMComment) : CommentsPendingAction()
    data class UpvoteReply(val comment: AMComment, val reply: AMComment) : CommentsPendingAction()
    data class DownvoteReply(val comment: AMComment, val reply: AMComment) : CommentsPendingAction()
}
