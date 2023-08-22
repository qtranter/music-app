package com.audiomack.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.Timer
import java.util.TimerTask
import timber.log.Timber

class LocationDetector(
    private val context: Context
) : LifecycleObserver {

    private var enabled = true
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var timeoutTimer: Timer? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        if (enabled) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                val locationListener = object : LocationListener {
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    override fun onLocationChanged(location: Location?) {
                        if (location != null) {
                            // The location should be intercepted automatically by the ads SDKs
                            stop()
                        }
                    }
                }

                val criteria = Criteria()
                criteria.isAltitudeRequired = false
                criteria.isBearingRequired = false
                val provider = locationManager.getBestProvider(criteria, true)
                if (provider != null) {
                    try {
                        locationManager.requestLocationUpdates(provider, 1000, 1f, locationListener)
                        locationListener.onLocationChanged(locationManager.getLastKnownLocation(provider))
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }

                this.locationManager = locationManager
                this.locationListener = locationListener

                this.timeoutTimer = Timer()
                timeoutTimer?.schedule(object : TimerTask() {
                        override fun run() {
                            stop()
                        }
                    }, 10000) // Stop GPS after 10 seconds even if no positions have been detected
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        enabled = false
        locationManager?.let { locationManager ->
            locationListener?.let { locationListener ->
                try {
                    locationManager.removeUpdates(locationListener)
                    timeoutTimer?.cancel()
                } catch (e: Exception) {
                    Timber.w(e)
                }
                this.locationManager = null
                this.locationListener = null
                this.timeoutTimer = null
            }
        }
    }
}
