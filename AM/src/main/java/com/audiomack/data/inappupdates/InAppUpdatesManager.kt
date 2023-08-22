package com.audiomack.data.inappupdates

import android.app.Activity
import com.audiomack.BuildConfig
import com.audiomack.MainApplication
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.data.remotevariables.RemoteVariablesProviderImpl
import com.audiomack.utils.isVersionLowerThan
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.lang.Exception

interface InAppUpdatesManager {
    /** Used to check for the presence of in-app-updates **/
    fun checkForUpdates(): Single<InAppUpdateAvailabilityResult>
    /** Used to start downloading the available in-app-update **/
    fun triggerUpdate(activity: Activity): Observable<InAppUpdateResult>
    /** Used to start installing the in-app-update, only used for the flexible ones **/
    fun applyUpdate()
}

class InAppUpdatesManagerImpl(
    private val remoteVariablesProvider: RemoteVariablesProvider = RemoteVariablesProviderImpl()
) : InAppUpdatesManager, InstallStateUpdatedListener {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(MainApplication.context!!)

    private var update: InAppUpdateAvailability ? = null

    private val subject = BehaviorSubject.create<InAppUpdateResult>()

    private var flexibleUpdateDownloadStartedNotified = false

    private val inAppUpdateMode: InAppUpdatesMode
        get() {
            val minImmediate = remoteVariablesProvider.inAppUpdatesMinImmediateVersion
            val minFlexible = remoteVariablesProvider.inAppUpdatesMinFlexibleVersion
            val currentVersion = BuildConfig.VERSION_NAME
            return when {
                currentVersion.isVersionLowerThan(minImmediate) -> InAppUpdatesMode.Immediate
                currentVersion.isVersionLowerThan(minFlexible) -> InAppUpdatesMode.Flexible
                else -> InAppUpdatesMode.Disabled
            }
        }

    override fun checkForUpdates(): Single<InAppUpdateAvailabilityResult> {
        return Single.create { emitter ->
            val mode = inAppUpdateMode
            val appUpdateType = when (mode) {
                InAppUpdatesMode.Flexible -> AppUpdateType.FLEXIBLE
                InAppUpdatesMode.Immediate -> AppUpdateType.IMMEDIATE
                else -> {
                    emitter.onError(InAppUpdateAvailabilityException.InAppUpdatesNotCheckedAvailable)
                    return@create
                }
            }
            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.isUpdateTypeAllowed(appUpdateType)) {
                        when {
                            appUpdateType == AppUpdateType.IMMEDIATE && info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                                update = InAppUpdateAvailability(info, appUpdateType)
                                emitter.onSuccess(InAppUpdateAvailabilityResult.NeedToResumeDownload)
                            }
                            info.installStatus() == InstallStatus.DOWNLOADED -> {
                                emitter.onSuccess(InAppUpdateAvailabilityResult.ReadyToInstall)
                            }
                            info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                                update = InAppUpdateAvailability(info, appUpdateType)
                                emitter.onSuccess(InAppUpdateAvailabilityResult.ReadyToDownload(mode))
                            }
                            else -> {
                                emitter.onError(InAppUpdateAvailabilityException.NoInAppUpdateAvailable)
                            }
                        }
                    } else {
                        emitter.onError(InAppUpdateAvailabilityException.NoInAppUpdateAvailable)
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    emitter.onError(InAppUpdateAvailabilityException.FailedToCheckInAppUpdates)
                }
        }
    }

    override fun triggerUpdate(activity: Activity): Observable<InAppUpdateResult> {
        val update = update ?: return Observable.error(InAppUpdateException.InvalidUpdateInfo)
        if (update.type == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(this)
        }
        appUpdateManager.startUpdateFlow(
            update.info,
            activity,
            AppUpdateOptions.defaultOptions(update.type)
        )
        return subject
    }

    override fun applyUpdate() {
        appUpdateManager.completeUpdate()
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADING && update?.type == AppUpdateType.FLEXIBLE && !flexibleUpdateDownloadStartedNotified) {
            flexibleUpdateDownloadStartedNotified = true
            subject.onNext(InAppUpdateResult.FlexibleDownloadStarted)
        }
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            subject.onNext(InAppUpdateResult.Downloaded)
        }
        if ((state.installStatus() == InstallStatus.DOWNLOADED || state.installStatus() == InstallStatus.FAILED) && update?.type == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(this)
        }
    }
}

data class InAppUpdateAvailability(
    val info: AppUpdateInfo,
    val type: Int
)

sealed class InAppUpdateAvailabilityResult {
    /** Update (flexible) is ready to be installed **/
    object ReadyToInstall : InAppUpdateAvailabilityResult()
    /** Update is ready to be downloaded **/
    data class ReadyToDownload(val mode: InAppUpdatesMode) : InAppUpdateAvailabilityResult()
    /** Update (immediate) needs to be resumed **/
    object NeedToResumeDownload : InAppUpdateAvailabilityResult()
}

sealed class InAppUpdateResult {
    /** (Flexible) update's download was successful, need to restart the app **/
    object Downloaded : InAppUpdateResult()
    /** (Flexible) update's download started **/
    object FlexibleDownloadStarted : InAppUpdateResult()
}

sealed class InAppUpdateAvailabilityException : Exception() {
    object InAppUpdatesNotCheckedAvailable : InAppUpdateAvailabilityException()
    object NoInAppUpdateAvailable : InAppUpdateAvailabilityException()
    object FailedToCheckInAppUpdates : InAppUpdateAvailabilityException()
}

sealed class InAppUpdateException : Exception() {
    object InvalidUpdateInfo : InAppUpdateException()
}

enum class InAppUpdatesMode {
    Disabled, Flexible, Immediate
}
