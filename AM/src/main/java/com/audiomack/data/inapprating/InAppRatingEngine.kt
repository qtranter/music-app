package com.audiomack.data.inapprating

import android.app.Activity
import com.audiomack.utils.Utils
import com.google.android.play.core.review.ReviewManagerFactory

interface InAppRatingEngine {
    fun show(activity: Activity)
}

class PlayStoreInAppRatingEngine : InAppRatingEngine {

    override fun show(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener {
            if (it.isSuccessful) {
                manager.launchReviewFlow(activity, it.result)
            } else {
                Utils.openAppRating(activity)
            }
        }
    }
}
