package com.audiomack.data.sleeptimer

import androidx.annotation.VisibleForTesting
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerCleared
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerSet
import com.audiomack.data.sleeptimer.SleepTimerEvent.TimerTriggered
import com.audiomack.rx.AMSchedulersProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.utils.Second
import com.audiomack.utils.addTo
import com.audiomack.utils.toSeconds
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.Calendar
import java.util.concurrent.TimeUnit.SECONDS
import timber.log.Timber

class SleepTimerManager(
    private val preferences: PreferencesDataSource = PreferencesRepository(),
    private val schedulers: SchedulersProvider = AMSchedulersProvider(),
    private val disposables: CompositeDisposable = CompositeDisposable()
) : SleepTimer {

    override val sleepEvent = BehaviorSubject.create<SleepTimerEvent>()

    private var timerDisposable: Disposable? = null

    private var sleepTimestamp: Long
        get() = preferences.sleepTimerTimestamp
        set(value) { preferences.sleepTimerTimestamp = value }

    init {
        restoreTimer()
        saveTimestampOnSleepEvent()
    }

    override fun set(seconds: Second) {
        timerDisposable?.dispose()
        timerDisposable = Completable.timer(seconds, SECONDS, schedulers.interval)
            .observeOn(schedulers.main)
            .subscribe {
                sleepEvent.onNext(TimerTriggered)
                sleepEvent.onNext(TimerCleared)
            }

        val date = Calendar.getInstance().run {
            add(Calendar.SECOND, seconds.toInt())
            time
        }
        sleepEvent.onNext(TimerSet(date))
    }

    override fun clear() {
        timerDisposable?.dispose()
        sleepEvent.onNext(TimerCleared)
    }

    private fun restoreTimer() {
        Single.timer(RESTORE_DELAY, SECONDS, schedulers.io)
            .map { sleepTimestamp - System.currentTimeMillis() }
            .filter { it > 0L }
            .observeOn(schedulers.main)
            .subscribe { set(it.toSeconds()) }
            .addTo(disposables)
    }

    private fun saveTimestampOnSleepEvent() {
        sleepEvent.subscribeOn(schedulers.io)
            .doOnNext(::logEvent)
            .map {
                when (it) {
                    is TimerSet -> it.date.time
                    else -> 0
                }
            }
            .subscribe { sleepTimestamp = it }
            .addTo(disposables)
    }

    private fun logEvent(event: SleepTimerEvent) {
        if (event is TimerSet) {
            Timber.tag(TAG).i("Sleep timer event: $event")
        } else {
            Timber.tag(TAG).i("Sleep timer event: ${event.javaClass.simpleName}")
        }
    }

    companion object {

        private const val TAG = "SleepTimerManager"

        /**
         * Delay in seconds to postpone restoring the sleep timer, to avoid crowding app init
         */
        private const val RESTORE_DELAY = 5L

        @Volatile
        private var instance: SleepTimerManager? = null

        fun getInstance(
            preferences: PreferencesDataSource = PreferencesRepository(),
            schedulers: SchedulersProvider = AMSchedulersProvider(),
            disposables: CompositeDisposable = CompositeDisposable()
        ): SleepTimerManager =
            instance ?: synchronized(this) {
                instance ?: SleepTimerManager(
                    preferences,
                    schedulers,
                    disposables
                ).also { instance = it }
            }

        @VisibleForTesting
        internal fun destroy() {
            instance = null
        }
    }
}
