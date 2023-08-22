package com.audiomack.ui.authentication

import com.audiomack.model.AMArtist.Gender
import java.util.Date

data class SignupCredentials(
    val username: String,
    val email: String,
    val password: String,
    val advertisingId: String?,
    var birthday: Date? = null,
    var gender: Gender? = null
) {
    companion object {
        const val MIN_AGE = 13
        const val MAX_AGE = 100
    }
}
