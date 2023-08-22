package com.audiomack.ui.comments.add

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.audiomack.data.comment.CommentDataSource
import com.audiomack.data.comment.CommentRepository
import com.audiomack.data.imageloader.ImageLoader
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.user.UserRepository
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventCommentAdded
import com.audiomack.model.EventCommentIntroDismissed
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.ui.base.BaseViewModel
import com.audiomack.utils.SingleLiveEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AddCommentViewModel(
    private val entity: AMResultItem?,
    private var threadId: String?,
    private val commentDataSource: CommentDataSource = CommentRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository(),
    private val userDataSource: UserDataSource = UserRepository.getInstance(),
    val imageLoader: ImageLoader = PicassoImageLoader,
    preferencesDataSource: PreferencesDataSource = PreferencesRepository(),
    private val eventBus: EventBus = EventBus.getDefault(),
    private val schedulersProvider: SchedulersProvider = AMSchedulersProvider()
) : BaseViewModel() {

    val close = SingleLiveEvent<Void>()

    val buttonSendEvent = SingleLiveEvent<Void>()
    val showKeyboardEvent = SingleLiveEvent<Void>()
    val hideKeyboardEvent = SingleLiveEvent<Void>()
    val showLoadingEvent = SingleLiveEvent<Void>()
    val hideLoadingEvent = SingleLiveEvent<Void>()
    val showErrorMessageEvent = SingleLiveEvent<Void>()
    val showCommentIntroEvent = SingleLiveEvent<Void>()

    private var _avatar = MutableLiveData<String?>()
    val avatar: LiveData<String?> = _avatar

    private var _songName = MutableLiveData<String>()
    val songName: LiveData<String> = _songName

    init {
        if (preferencesDataSource.needToShowCommentTooltip) {
            showCommentIntroEvent.call()
            preferencesDataSource.needToShowCommentTooltip = false
        } else {
            showKeyboardEvent.call()
        }
        _avatar.postValue(userDataSource.avatar)
        _songName.postValue(entity?.title ?: "")
        eventBus.register(this)
    }

    @VisibleForTesting
    public override fun onCleared() {
        super.onCleared()
        eventBus.unregister(this)
    }

    fun onSendTapped() {
        buttonSendEvent.call()
    }

    fun buttonSendTapped(content: String?) {
        val id = entity!!.itemId
        val kind = entity.typeForHighlightingAPI
        val thread = threadId

        if (content.isNullOrEmpty() || !userDataSource.canComment()) {
            hideKeyboardEvent.call()
            return
        }

        showLoadingEvent.call()

        compositeDisposable.add(
                        commentDataSource.postComment(content, kind, id, thread)
                        .subscribeOn(schedulersProvider.io)
                        .observeOn(schedulersProvider.main)
                        .subscribe(
                                { comment ->
                                    hideLoadingEvent.call()
                                    hideKeyboardEvent.call()
                                    close.call()
                                    mixpanelDataSource.trackAddComment(comment, entity)
                                    EventBus.getDefault().post(EventCommentAdded(comment))
                                },
                                {
                                    hideLoadingEvent.call()
                                    showErrorMessageEvent.call()
                                }
                        )
        )
    }

    fun onBackgroundTapped() {
        hideKeyboardEvent.call()
        close.call()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventCommentIntroDismissed) {
        showKeyboardEvent.call()
    }
}
