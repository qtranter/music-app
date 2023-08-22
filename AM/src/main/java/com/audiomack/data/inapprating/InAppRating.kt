package com.audiomack.data.inapprating

import android.app.Activity
import io.reactivex.subjects.Subject

enum class InAppRatingResult {
    ShowRatingPrompt,
    ShowDeclinedRatingPrompt,
    OpenRating,
    OpenSupport
}

interface InAppRating {
    /** Emits events related to in-app rating or its preliminary prompts **/
    val inAppRating: Subject<InAppRatingResult>

    /** Update stats about downloads and favorites **/
    fun incrementDownloadCount()
    fun incrementFavoriteCount()

    /** Used to trigger the in-app rating, it won't do anything if conditions are not met **/
    fun request()

    /** Shows the in-app rating prompt **/
    fun show(activity: Activity)

    /** Handlers of the preliminary alerts shown before the in-app rating prompt **/
    fun onRatingPromptAccepted()
    fun onRatingPromptDeclined()
    fun onDeclinedRatingPromptAccepted()
    fun onDeclinedRatingPromptDeclined()
}
