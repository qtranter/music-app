package com.audiomack.ui.player.full.view

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.audiomack.DISABLED_ALPHA
import com.audiomack.R
import com.audiomack.model.AMResultItem.MusicDownloadType.Limited
import com.audiomack.model.AMResultItem.MusicDownloadType.Premium
import com.audiomack.playback.ActionState.ACTIVE
import com.audiomack.playback.ActionState.DEFAULT
import com.audiomack.playback.ActionState.DISABLED
import com.audiomack.playback.ActionState.FROZEN
import com.audiomack.playback.ActionState.LOADING
import com.audiomack.playback.ActionState.QUEUED
import com.audiomack.playback.SongAction
import com.audiomack.playback.SongAction.AddToPlaylist
import com.audiomack.playback.SongAction.Download
import com.audiomack.playback.SongAction.Edit
import com.audiomack.playback.SongAction.Favorite
import com.audiomack.playback.SongAction.RePost
import com.audiomack.playback.SongAction.Share
import com.audiomack.utils.addTo
import com.audiomack.utils.extensions.colorCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlinx.android.synthetic.main.view_player_action.view.playerActionBtnBadge
import kotlinx.android.synthetic.main.view_player_action.view.playerActionBtnContent
import kotlinx.android.synthetic.main.view_player_action.view.playerActionBtnContentImage
import kotlinx.android.synthetic.main.view_player_action.view.playerActionBtnContentText
import kotlinx.android.synthetic.main.view_player_action.view.playerActionBtnProgress

class SongActionButton(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var actionSubject = BehaviorSubject.create<SongAction>()
    private var actionDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    var action: SongAction? = null
        set(value) {
            field = value
            value?.let {
                val state = it.state
                isEnabled = state != LOADING && state != DISABLED
                alpha = if (state == DISABLED) DISABLED_ALPHA else 1f
                actionSubject.onNext(it)
            }
        }

    private var showCaption: Boolean = true

    init {
        View.inflate(context, R.layout.view_player_action, this)
        context.theme.obtainStyledAttributes(attrs, R.styleable.SongActionButton, 0, 0).apply {
            try {
                val index = getInt(R.styleable.SongActionButton_type, -1)
                action = when (index) {
                    0 -> Favorite()
                    1 -> AddToPlaylist()
                    2 -> RePost()
                    3 -> Download()
                    4 -> Share()
                    5 -> Edit()
                    else -> throw IllegalStateException("Invalid action type")
                }
                showCaption = getBoolean(R.styleable.SongActionButton_showCaption, true)
            } finally {
                recycle()
            }
        }

        if (!isInEditMode) {
            layoutTransition = LayoutTransition()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        actionDisposable?.let { disposables.remove(it) }
        actionDisposable = actionSubject
            .throttleLatest(250L, MILLISECONDS, true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                updateViews()
                invalidate()
            }
            .addTo(disposables)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }

    private fun updateViews() {
        val state = action?.state ?: DEFAULT

        when (action) {
            is Favorite -> {
                playerActionBtnProgress.isVisible = state == LOADING
                playerActionBtnContentText.isInvisible = state == LOADING
                playerActionBtnContentImage.isInvisible = state == LOADING
                playerActionBtnContentImage.setImageResource(
                    if (ACTIVE == state) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_empty
                )
                playerActionBtnContentText.text = resources.getString(R.string.options_favorite)
            }
            is AddToPlaylist -> {
                playerActionBtnContentText.isInvisible = state == LOADING
                playerActionBtnContentImage.setImageResource(R.drawable.ic_add)
                playerActionBtnContentText.text =
                    resources.getString(R.string.options_add_to_playlist_short)
            }
            is RePost -> {
                playerActionBtnProgress.isVisible = state == LOADING
                playerActionBtnContentText.isInvisible = state == LOADING
                playerActionBtnContentImage.isInvisible = state == LOADING
                playerActionBtnContentImage.setImageResource(
                    if (ACTIVE == state) R.drawable.ic_reup_active
                    else R.drawable.ic_reup
                )
                playerActionBtnContentText.text = resources.getString(R.string.options_repost)
            }
            is Download -> {
                playerActionBtnProgress.isVisible = state == LOADING || state == QUEUED
                if (state == LOADING) {
                    playerActionBtnProgress.applyColor(R.color.orange)
                } else if (state == QUEUED) {
                    playerActionBtnProgress.applyColor(R.color.gray_text)
                }
                playerActionBtnContent.isInvisible = state == LOADING || state == QUEUED
                playerActionBtnContentImage.isInvisible = state == LOADING || state == QUEUED
                playerActionBtnContentText.text = resources.getString(R.string.options_download)
                configureDownloadButton()
            }
            is Share -> {
                playerActionBtnContentImage.setImageResource(R.drawable.ic_share)
                playerActionBtnContentText.text = resources.getString(R.string.options_share)
            }
            is Edit -> {
                playerActionBtnContentImage.setImageResource(R.drawable.ic_edit)
                playerActionBtnContentText.text = resources.getString(R.string.options_edit)
            }
        }

        playerActionBtnContentText.setTextColor(playerActionBtnContentText.context.colorCompat(
                if (state == ACTIVE) R.color.action_button_selected else R.color.action_button_deselected
            )
        )

        if (!showCaption) {
            playerActionBtnContentText.visibility = View.GONE
        }
    }

    private fun configureDownloadButton() {
        val state = action?.state ?: DEFAULT

        val isPremiumLimited = state.downloadType == Limited
        val isPremiumOnly = state.downloadType == Premium
        val isPremium = state.isPremium
        playerActionBtnContentImage.alpha = if (isPremiumOnly && !isPremium) DISABLED_ALPHA else 1F

        playerActionBtnContentImage.setImageResource(
            when {
                FROZEN == state -> if (isPremiumLimited) R.drawable.ic_download_frozen_unlocked else R.drawable.ic_download_frozen_locked
                ACTIVE == state -> if (isPremiumLimited || (isPremium && isPremiumOnly)) R.drawable.ic_premium_downloaded else R.drawable.ic_downloaded
                else -> if (isPremiumLimited || (isPremium && isPremiumOnly)) R.drawable.ic_premium_download else R.drawable.ic_download
            }
        )

        val frozenDownloadCount = state.frozenDownloadsCount ?: 0
        val frozenDownloadsTotal = state.frozenDownloadsTotal ?: 0
        playerActionBtnBadge.isVisible = frozenDownloadCount in 1 until frozenDownloadsTotal
        if (frozenDownloadsTotal > frozenDownloadCount) playerActionBtnBadge.text = "$frozenDownloadCount"
    }
}
