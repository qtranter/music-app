package com.audiomack.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import com.audiomack.MainApplication
import com.audiomack.utils.Foreground.Listener
import java.util.concurrent.CopyOnWriteArrayList
import timber.log.Timber

interface Foreground {
    interface Listener {
        fun onBecameForeground()
        fun onBecameBackground()
    }

    val isForeground: Boolean
    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
    fun setActivityPaused(activityName: String)
    fun setActivityResumed(activityName: String)
    fun isActivityVisible(activityName: String): Boolean
}

class ForegroundManager : Foreground, Application.ActivityLifecycleCallbacks {

    private var _isForeground = false
    override val isForeground
        get() = _isForeground

    private var paused = true
    private val handler = Handler()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private var check: Runnable? = null
    private var activitiesStates: MutableMap<String, Boolean> = mutableMapOf()

    override fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    override fun setActivityPaused(activityName: String) {
        activitiesStates[activityName] = false
    }

    override fun setActivityResumed(activityName: String) {
        activitiesStates[activityName] = true
    }

    override fun isActivityVisible(activityName: String): Boolean {
        return activitiesStates[activityName] ?: false
    }

    override fun onActivityResumed(activity: Activity?) {
        paused = false
        val wasBackground = !isForeground
        _isForeground = true

        if (check != null)
            handler.removeCallbacks(check)

        if (wasBackground) {
            Timber.tag(TAG).i("went foreground")
            for (l in listeners) {
                try {
                    l.onBecameForeground()
                } catch (exc: Exception) {
                    Timber.w(exc)
                }
            }
        } else {
            Timber.tag(TAG).i("still foreground")
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        paused = true

        if (check != null)
            handler.removeCallbacks(check)

        check = Runnable {
            if (isForeground && paused) {
                _isForeground = false
                Timber.tag(TAG).i("went background")
                for (l in listeners) {
                    try {
                        l.onBecameBackground()
                    } catch (exc: Exception) {
                        Timber.w(exc)
                    }
                }
            } else {
                Timber.tag(TAG).i("still foreground")
            }
        }

        handler.postDelayed(check, CHECK_DELAY)
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity?) {}

    override fun onActivityStopped(activity: Activity?) {}

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity?) {}

    companion object {

        private const val CHECK_DELAY: Long = 500
        private val TAG = ForegroundManager::class.java.simpleName

        private var instance: ForegroundManager? = null

        /**
         * Its not strictly necessary to use this method - _usually_ invoking
         * get with a Context gives us a path to retrieve the Application and
         * initialise, but sometimes (e.g. in test harness) the ApplicationContext
         * is != the Application, and the docs make no guarantees.
         *
         * @param application
         * @return an initialised Foreground instance
         */
        @JvmStatic
        fun init(application: Application): ForegroundManager {
            if (instance == null) {
                instance = ForegroundManager()
                application.registerActivityLifecycleCallbacks(instance)
            }
            return instance!!
        }

        @JvmStatic
        operator fun get(application: Application): ForegroundManager? {
            if (instance == null) {
                init(application)
            }
            return instance
        }

        @JvmStatic
        operator fun get(ctx: Context): ForegroundManager {
            if (instance == null) {
                val appCtx = ctx.applicationContext
                if (appCtx is Application) {
                    init(appCtx)
                }
            }
            return instance ?: throw IllegalStateException("Foreground is not initialised and cannot obtain the Application object")
        }

        @JvmStatic
        fun get(): ForegroundManager {
            return instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("Foreground is not initialised - invoke at least once with parameterised init/get")
        }
    }
}
