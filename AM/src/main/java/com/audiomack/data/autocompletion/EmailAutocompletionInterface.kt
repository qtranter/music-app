package com.audiomack.data.autocompletion

interface EmailAutocompletionInterface {

    fun getCompletionForPrefix(prefix: String, ignoreCase: Boolean): String
}
