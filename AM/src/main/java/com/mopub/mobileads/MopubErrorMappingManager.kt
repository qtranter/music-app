package com.mopub.mobileads

import com.google.android.gms.ads.AdRequest

object MopubErrorMappingManager {

    fun getErrorCodeFromAdmob(error: Int): MoPubErrorCode {
        return when (error) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> MoPubErrorCode.INTERNAL_ERROR
            AdRequest.ERROR_CODE_INVALID_REQUEST -> MoPubErrorCode.UNSPECIFIED
            AdRequest.ERROR_CODE_NETWORK_ERROR -> MoPubErrorCode.NO_CONNECTION
            AdRequest.ERROR_CODE_NO_FILL -> MoPubErrorCode.NO_FILL
            else -> MoPubErrorCode.UNSPECIFIED
        }
    }
}
