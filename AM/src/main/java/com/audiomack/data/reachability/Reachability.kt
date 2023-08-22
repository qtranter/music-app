package com.audiomack.data.reachability

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.core.content.getSystemService
import com.audiomack.MainApplication
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

interface ReachabilityDataSource {
    val networkAvailable: Boolean
    val connectedToWiFi: Boolean
    fun subscribe(context: Context?)
    fun unsubscribe()
}

class Reachability private constructor(
    context: Context
) : ReachabilityDataSource {

    private var connectivity: Connectivity? = null
    private var subscription: Disposable? = null

    private val connectivityManager: ConnectivityManager? = context.getSystemService()

    override val networkAvailable: Boolean
        get() = connectivity?.let {
            it.state() == NetworkInfo.State.CONNECTED
        } ?: connectivityManager?.activeNetworkInfo?.isConnected == true

    override val connectedToWiFi: Boolean
        get() = connectivity?.let {
            networkAvailable && it.type() == ConnectivityManager.TYPE_WIFI
        } ?: connectivityManager?.isActiveNetworkMetered == false

    override fun subscribe(context: Context?) {
        Timber.tag(TAG).d("Subscribing")

        subscription = ReactiveNetwork.observeNetworkConnectivity(context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ connectivity ->
                Timber.tag(TAG).d("Network connectivity changed: $connectivity")
                this.connectivity = connectivity
            }, { throwable ->
                Timber.tag(TAG).e(throwable, "Error while listening for network connectivity")
                connectivity = null
            }, {
                connectivity = null
            })
    }

    override fun unsubscribe() {
        Timber.tag(TAG).d("Unsubscribing")
        subscription?.dispose()
    }

    companion object {
        private const val TAG = "Reachability"

        @Volatile
        private var instance: Reachability? = null

        @JvmStatic
        fun init(context: Context): Reachability = instance ?: synchronized(this) {
            instance ?: Reachability(context).also { instance = it }
        }

        @JvmStatic
        fun getInstance(): Reachability =
            instance ?: MainApplication.context?.let { init(it) }
            ?: throw IllegalStateException("Reachability was not initialized")
    }
}
