package com.audiomack.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.audiomack.utils.ForegroundManager
import com.comscore.Analytics
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.tapjoy.Tapjoy
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    open var credentialsApiClient: GoogleApiClient? = null
    open var disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        credentialsApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .enableAutoManage(this, this)
            .addApi(Auth.CREDENTIALS_API)
            .build()

        ForegroundManager.init(application)
    }

    override fun onStart() {
        super.onStart()
        Tapjoy.onActivityStart(this)
    }

    override fun onStop() {
        Tapjoy.onActivityStop(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Analytics.notifyEnterForeground()
    }

    override fun onPause() {
        super.onPause()
        Analytics.notifyExitForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    protected fun clearFragmentManager() {
        val backStackEntry = supportFragmentManager.backStackEntryCount
        if (backStackEntry > 0) {
            for (i in 0 until backStackEntry) {
                supportFragmentManager.popBackStackImmediate()
            }
        }
        if (supportFragmentManager.fragments.size > 0) {
            for (i in 0 until supportFragmentManager.fragments.size) {
                val mFragment = supportFragmentManager.fragments[i]
                if (mFragment != null) {
                    supportFragmentManager.beginTransaction().remove(mFragment).commit()
                }
            }
        }
    }

    fun closeOptionsFragment() {
        supportFragmentManager.findFragmentByTag("options")?.let { optionsFragment ->
            supportFragmentManager
                .beginTransaction()
                .remove(optionsFragment)
                .commitAllowingStateLoss()
        }
    }

    open fun openOptionsFragment(optionsMenuFragment: Fragment) {}

    open fun openImageZoomFragment(imageZoomFragment: Fragment) {}

    open fun popFragment(): Boolean {
        return false
    }

    // Google API client callbacks

    override fun onConnected(bundle: Bundle?) {
        Timber.tag("SmartLock").d(javaClass.simpleName + ": credentialsApiClient connected")
    }

    override fun onConnectionSuspended(i: Int) {
        Timber.tag("SmartLock").d(javaClass.simpleName + ": credentialsApiClient connection suspended")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Timber.tag("SmartLock").d(javaClass.simpleName + ": credentialsApiClient connection failed")
    }
}
