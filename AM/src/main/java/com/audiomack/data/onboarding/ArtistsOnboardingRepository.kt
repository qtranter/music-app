package com.audiomack.data.onboarding

import com.audiomack.model.OnboardingArtist
import com.audiomack.network.API
import com.audiomack.network.onboardingItems
import io.reactivex.Observable

class ArtistsOnboardingRepository : ArtistsOnboardingDataSource {

    override fun onboardingItems(): Observable<List<OnboardingArtist>> {
        return API.getInstance().onboardingItems()
    }
}
