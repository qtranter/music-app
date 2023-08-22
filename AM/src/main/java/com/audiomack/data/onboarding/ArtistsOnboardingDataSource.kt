package com.audiomack.data.onboarding

import com.audiomack.model.OnboardingArtist
import io.reactivex.Observable

interface ArtistsOnboardingDataSource {

    fun onboardingItems(): Observable<List<OnboardingArtist>>
}
