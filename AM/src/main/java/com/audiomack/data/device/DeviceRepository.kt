package com.audiomack.data.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.audiomack.BuildConfig
import com.audiomack.MainApplication

object DeviceRepository : DeviceDataSource {

    override fun hasRuntimePermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    override fun hasDoNotKeepActivitiesFlag(): Boolean {
        val result = Settings.Global.getInt(MainApplication.context!!.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0)
        return result == 1
    }

    override fun hasStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(MainApplication.context!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun getModel(): String {
        return Build.MODEL
    }

    override fun getManufacturer(): String {
        return Build.MANUFACTURER ?: ""
    }

    override fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    override fun getAppVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getAppVersionCode(): String {
        return BuildConfig.VERSION_CODE.toString()
    }

    override fun getAppVersionFull(): String {
        return getAppVersionName() + " (" + getAppVersionCode() + ")"
    }

    override fun isMobileDataEnabled(): Boolean {
        val connectivityManager = MainApplication.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return ConnectivityManager.TYPE_MOBILE == connectivityManager?.activeNetworkInfo?.type
    }

    override fun getCarrierName(): String? {
        val telephonyManager = MainApplication.context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager?.networkOperatorName
    }

    override fun isRunningLowOnMemory(): Boolean {
        val activityManager = MainApplication.context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
            return memoryInfo.lowMemory
        }
    }

    override fun hasEqualizer(): Boolean {
        val context = MainApplication.context ?: return false
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        return intent.resolveActivity(context.packageManager) != null
    }

    override var castAvailable: Boolean = true

    override val runningEspressoTest: Boolean
        get() {
            try {
                Class.forName("android.support.test.espresso.Espresso")
                return true
            } catch (e: ClassNotFoundException) { }
            return false
        }
}
