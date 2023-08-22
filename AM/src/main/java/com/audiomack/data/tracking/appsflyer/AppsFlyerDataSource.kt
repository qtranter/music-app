package com.audiomack.data.tracking.appsflyer

import com.audiomack.data.user.UserDataSource

interface AppsFlyerDataSource {

    fun trackUserSignup()

    fun trackAddToFavorites()

    fun trackCreatePlaylist()

    fun trackShareContent()

    fun trackSongPlay()

    fun trackPremiumView()

    fun trackPremiumStart()

    fun trackPremiumTrial()

    fun trackAdWatched()

    fun trackIdentity(userDataSource: UserDataSource)
}
