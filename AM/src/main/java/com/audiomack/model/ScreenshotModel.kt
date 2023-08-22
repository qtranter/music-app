package com.audiomack.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ScreenshotModel(
    var benchmark: BenchmarkModel = BenchmarkModel(),
    var mixpanelSource: MixpanelSource,
    var mixpanelButton: String,
    var music: Music?,
    var artist: Artist?
) : Parcelable
