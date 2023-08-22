package com.audiomack.ui.filter

import android.os.Parcelable
import com.audiomack.model.AMGenre
import com.audiomack.model.AMMusicType
import com.audiomack.model.AMPeriod
import com.audiomack.model.AMResultItemSort
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FilterData(
    val fragmentClassName: String,
    val title: String,
    val sections: List<FilterSection>,
    val selection: FilterSelection,
    val excludedGenres: List<AMGenre> = emptyList(),
    val excludedTypes: List<AMMusicType> = emptyList()
) : Parcelable

enum class FilterSection {
    Genre, Type, Sort, Local
}

@Parcelize
data class FilterSelection(
    var genre: AMGenre? = null,
    var period: AMPeriod? = null,
    var type: AMMusicType? = null,
    var sort: AMResultItemSort? = null
) : Parcelable
