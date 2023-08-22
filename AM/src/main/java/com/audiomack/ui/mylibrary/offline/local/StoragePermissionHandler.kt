package com.audiomack.ui.mylibrary.offline.local

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.Fragment
import com.audiomack.MainApplication
import com.audiomack.data.preferences.PreferencesDataSource
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelRepository
import com.audiomack.ui.common.Permission
import com.audiomack.ui.common.Permission.Storage
import com.audiomack.ui.common.PermissionHandler
import com.audiomack.ui.common.permissionGranted
import com.audiomack.ui.common.requestPermissions
import com.audiomack.utils.showPermissionRationaleDialog
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

// TODO convert this into a general permissions handler
class StoragePermissionHandler constructor(
    private val context: Context,
    private val preferencesRepository: PreferencesDataSource = PreferencesRepository(),
    private val mixpanelDataSource: MixpanelDataSource = MixpanelRepository()
) : PermissionHandler<Storage> {

    private val _hasPermission = BehaviorSubject.createDefault(isPermissionGranted(context))
    override val hasPermissionObservable: Observable<Boolean> get() = _hasPermission

    override val hasPermission: Boolean
        get() = _hasPermission.value ?: isPermissionGranted(context)

    override fun checkPermissions(
        activity: Activity,
        onRequested: (Permission) -> Unit,
        onAlreadyGranted: (Permission) -> Unit,
        showRationale: (Permission) -> Unit
    ) {
        when {
            isPermissionGranted(activity) -> {
                onAlreadyGranted(Storage)
                _hasPermission.onNext(true)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                WRITE_EXTERNAL_STORAGE
            ) -> {
                showRationale(Storage)
                _hasPermission.onNext(false)
            }
            else -> {
                requestPermissions(activity)
                onRequested(Storage)
                _hasPermission.onNext(false)
            }
        }
    }

    override fun checkPermissions(
        fragment: Fragment,
        onRequested: (Permission) -> Unit,
        onAlreadyGranted: (Permission) -> Unit
    ) {
        val context = fragment.context ?: return
        when {
            isPermissionGranted(context) -> {
                onAlreadyGranted(Storage)
                _hasPermission.onNext(true)
            }
            fragment.shouldShowRequestPermissionRationale(
                WRITE_EXTERNAL_STORAGE
            ) -> {
                fragment.showPermissionRationaleDialog(Storage.type)
                _hasPermission.onNext(false)
            }
            else -> {
                fragment.requestPermissions(Storage)
                onRequested(Storage)
                _hasPermission.onNext(false)
                mixpanelDataSource.trackPromptPermissions(Storage.type)
            }
        }
    }

    private fun isPermissionGranted(context: Context) =
        PermissionChecker.checkSelfPermission(
            context,
            WRITE_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED

    private fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(WRITE_EXTERNAL_STORAGE),
            Storage.reqCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: (Permission) -> Unit,
        onDenied: (Permission) -> Unit
    ) {
        when (requestCode) {
            Storage.reqCode -> {
                if (grantResults.permissionGranted()) {
                    _hasPermission.onNext(true)
                    onGranted(Storage)
                } else {
                    _hasPermission.onNext(false)
                    onDenied(Storage)
                    toggleIncludeFilesOff()
                }
                return
            }
        }
    }

    private fun toggleIncludeFilesOff() {
        Completable.fromRunnable {
            preferencesRepository.includeLocalFiles = false
        }.subscribeOn(Schedulers.io()).subscribe()
    }

    companion object {
        @Volatile
        private var instance: StoragePermissionHandler? = null

        fun getInstance(context: Context = MainApplication.context!!): StoragePermissionHandler =
            instance ?: StoragePermissionHandler(context.applicationContext).also { instance = it }
    }
}
