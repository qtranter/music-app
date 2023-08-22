package com.audiomack.data.telco

interface TelcoDataSource {

    fun isWifi(): Boolean

    fun getPhoneCount(): Int?

    fun getPhoneType(): String?

    fun getSimOperator(): String?

    fun getSimOperatorName(): String?

    fun getSimCarrierId(): Int?

    fun getSimCarrierIdName(): String?

    fun getMobileCountryCode(): String?

    fun getMobileNetworkCode(): String?
}
