package com.audiomack.utils

import android.util.Patterns

interface RegexValidator {
    fun isValidEmailAddress(input: String): Boolean
}

class RegexValidatorImpl : RegexValidator {
    override fun isValidEmailAddress(input: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }
}
