package com.audiomack.data.device

interface DeviceDataSource {

    fun hasRuntimePermissions(): Boolean

    fun hasDoNotKeepActivitiesFlag(): Boolean

    fun hasStoragePermissionGranted(): Boolean

    fun getModel(): String

    fun getManufacturer(): String

    fun getOsVersion(): String

    fun getAppVersionName(): String

    fun getAppVersionCode(): String

    fun getAppVersionFull(): String

    fun isMobileDataEnabled(): Boolean

    fun getCarrierName(): String?

    fun isRunningLowOnMemory(): Boolean

    fun hasEqualizer(): Boolean

    var castAvailable: Boolean

    val runningEspressoTest: Boolean
}
