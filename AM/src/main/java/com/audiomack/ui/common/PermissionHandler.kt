package com.audiomack.ui.common

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.audiomack.model.PermissionType
import io.reactivex.Observable

sealed class Permission(val key: String, val type: PermissionType, val reqCode: Int) {
    object Location : Permission(ACCESS_COARSE_LOCATION, PermissionType.Location, 11220)
    object Storage : Permission(WRITE_EXTERNAL_STORAGE, PermissionType.Storage, 11221)
    object Camera : Permission(CAMERA, PermissionType.Camera, 11222)
}

interface PermissionHandler<T : Permission> {

    val hasPermissionObservable: Observable<Boolean>

    val hasPermission: Boolean

    /**
     * Checks to see if the permission is already granted. If not, start the request, or show a
     * rationale if needed.
     *
     * Callers should override [androidx.fragment.app.Fragment.onRequestPermissionsResult] or
     * [androidx.fragment.app.FragmentActivity.onRequestPermissionsResult] and call
     * [onRequestPermissionsResult] with the results.
     */
    fun checkPermissions(
        activity: Activity,
        onRequested: (Permission) -> Unit = {},
        onAlreadyGranted: (Permission) -> Unit = {},
        showRationale: (Permission) -> Unit = {}
    )

    /**
     * Checks to see if the permission is already granted. If not, start the request, or calls
     * [com.audiomack.utils.ExtensionsKt.showPermissionRationaleDialog] to show a rationale if needed.
     *
     * Callers should override [androidx.fragment.app.Fragment.onRequestPermissionsResult] or
     * [androidx.fragment.app.FragmentActivity.onRequestPermissionsResult] and call
     * [onRequestPermissionsResult] with the results.
     */
    fun checkPermissions(
        fragment: Fragment,
        onRequested: (Permission) -> Unit = {},
        onAlreadyGranted: (Permission) -> Unit = {}
    )

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: (Permission) -> Unit = {},
        onDenied: (Permission) -> Unit = {}
    )
}

fun Fragment.requestPermissions(permission: Permission) =
    requestPermissions(arrayOf(permission.key), permission.reqCode)

fun IntArray.permissionGranted() = isNotEmpty() && get(0) == PermissionChecker.PERMISSION_GRANTED
