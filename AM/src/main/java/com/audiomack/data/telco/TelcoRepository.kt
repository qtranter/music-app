package com.audiomack.data.telco

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.PHONE_TYPE_CDMA
import android.telephony.TelephonyManager.PHONE_TYPE_GSM
import android.telephony.TelephonyManager.PHONE_TYPE_SIP
import com.audiomack.MainApplication

class TelcoRepository : TelcoDataSource {

    override fun isWifi(): Boolean {
        val connectivityManager = MainApplication.context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        } else {
            ConnectivityManager.TYPE_WIFI == connectivityManager.activeNetworkInfo?.type
        }
    }

    override fun getPhoneCount(): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getTelephonyManager().phoneCount
        } else {
            null
        }
    }

    override fun getPhoneType(): String? {
        return when (getTelephonyManager().phoneType) {
            PHONE_TYPE_GSM -> "GSM"
            PHONE_TYPE_CDMA -> "CDMA"
            PHONE_TYPE_SIP -> "SIP"
            else -> null
        }
    }

    override fun getSimOperator(): String? {
        return getTelephonyManager().simOperator
    }

    override fun getSimOperatorName(): String? {
        return getTelephonyManager().simOperatorName
    }

    override fun getSimCarrierId(): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getTelephonyManager().simCarrierId
        } else {
            null
        }
    }

    override fun getSimCarrierIdName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getTelephonyManager().simCarrierIdName?.toString()
        } else {
            null
        }
    }

    override fun getMobileCountryCode(): String? {
        val operator = getTelephonyManager().networkOperator
        return if (!operator.isNullOrBlank()) operator.substring(0, 3) else null
    }

    override fun getMobileNetworkCode(): String? {
        val operator = getTelephonyManager().networkOperator
        return if (!operator.isNullOrBlank()) operator.substring(3) else null
    }

    private fun getTelephonyManager(): TelephonyManager {
        return MainApplication.context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
}
