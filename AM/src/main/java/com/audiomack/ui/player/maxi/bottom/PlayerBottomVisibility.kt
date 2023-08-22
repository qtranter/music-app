package com.audiomack.ui.player.maxi.bottom

import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import io.reactivex.Observer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

const val playerTabNotVisible = -1
const val playerTabCommentsIndex = 0
const val playerTabInfoIndex = 1
const val playerTabMoreFromArtistIndex = 2

interface PlayerBottomVisibility {
    var tabIndex: Int
    var tabsVisible: Boolean
    var reachedBottom: Boolean
    fun subscribe(observer: Observer<PlayerBottomVisibilityData>)
}

class PlayerBottomVisibilityImpl private constructor(
    private val scheuldersProvider: SchedulersProvider
) : PlayerBottomVisibility {

    private val subject: Subject<PlayerBottomVisibilityData> = BehaviorSubject.create()

    override var tabIndex: Int = playerTabCommentsIndex
        set(value) {
            field = value
            updateSubject()
        }

    override var tabsVisible: Boolean = false
        set(value) {
            field = value
            updateSubject()
        }

    override var reachedBottom: Boolean = false
        set(value) {
            field = value
            updateSubject()
        }

    private fun updateSubject() {
        subject.onNext(PlayerBottomVisibilityData(
            if (tabsVisible) tabIndex else playerTabNotVisible, reachedBottom))
    }

    override fun subscribe(observer: Observer<PlayerBottomVisibilityData>) {
        subject.observeOn(scheuldersProvider.main).subscribe(observer)
    }

    companion object {
        private var INSTANCE: PlayerBottomVisibilityImpl? = null

        fun getInstance(
            schedulersProvider: SchedulersProvider = AMSchedulersProvider()
        ): PlayerBottomVisibilityImpl = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PlayerBottomVisibilityImpl(schedulersProvider).also { INSTANCE = it }
        }

        fun destroy() {
            INSTANCE = null
        }
    }
}

data class PlayerBottomVisibilityData(
    val visibleTabIndex: Int,
    val reachedBottom: Boolean
)
