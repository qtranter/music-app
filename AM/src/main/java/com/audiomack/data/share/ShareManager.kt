package com.audiomack.data.share

import android.app.Activity
import android.content.Context
import com.audiomack.model.AMArtist
import com.audiomack.model.AMComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.MixpanelSource
import com.audiomack.model.ShareMethod
import io.reactivex.disposables.CompositeDisposable

interface ShareManager {

    fun copyMusicLink(activity: Activity?, item: AMResultItem?, mixpanelSource: MixpanelSource, mixpanelButton: String)

    fun copyArtistink(activity: Activity?, artist: AMArtist?, mixpanelSource: MixpanelSource, mixpanelButton: String)

    fun shareArtist(activity: Activity?, artist: AMArtist?, method: ShareMethod, mixpanelSource: MixpanelSource, mixpanelButton: String)

    fun shareMusic(
        activity: Activity?,
        item: AMResultItem?,
        method: ShareMethod,
        mixpanelSource: MixpanelSource,
        mixpanelButton: String,
        compositeDisposable: CompositeDisposable
    )

    fun shareScreenshot(activity: Activity?, music: AMResultItem?, artist: AMArtist?, method: ShareMethod, benchmark: BenchmarkModel?, mixpanelSource: MixpanelSource, mixpanelButton: String)

    fun shareStory(activity: Activity?, music: AMResultItem?, artist: AMArtist?, method: ShareMethod, mixpanelSource: MixpanelSource, mixpanelButton: String, compositeDisposable: CompositeDisposable)

    fun shareCommentLink(activity: Activity?, comment: AMComment, item: AMResultItem, mixpanelSource: MixpanelSource, mixpanelButton: String)

    fun shareLink(activity: Activity?, music: AMResultItem?, artist: AMArtist?, method: ShareMethod, mixpanelSource: MixpanelSource, mixpanelButton: String)

    fun openSupport(context: Context)
}
