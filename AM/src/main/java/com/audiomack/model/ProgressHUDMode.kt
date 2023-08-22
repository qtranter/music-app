package com.audiomack.model

sealed class ProgressHUDMode {
    object Loading : ProgressHUDMode()
    object Dismiss : ProgressHUDMode()
    data class Failure(val message: String, val stringResId: Int? = null) : ProgressHUDMode()
}
