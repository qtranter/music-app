package com.audiomack.ui.browse

import android.os.Bundle
import android.view.View
import com.audiomack.R.string
import com.audiomack.fragments.DataFragment
import com.audiomack.model.AMGenre
import com.audiomack.model.CellType
import com.audiomack.ui.filter.FilterData
import com.audiomack.ui.filter.FilterSection
import com.audiomack.ui.filter.FilterSelection

abstract class BrowseTabFragment(logTag: String) : DataFragment(logTag) {

    private lateinit var filterData: FilterData
    protected var header: BrowseHeader? = null

    open fun getFilterData(): FilterData = FilterData(
        this::class.java.simpleName,
        getString(string.filters_title_chart),
        listOf(FilterSection.Genre),
        FilterSelection(
            AMGenre.fromApiValue(genre),
            null
        )
    )

    override fun getCellType(): CellType = CellType.MUSIC_BROWSE_SMALL

    override fun recyclerViewHeader(): View? = context?.let { ctx ->
        BrowseHeader(ctx).apply {
            filter = filterData
            onGenreClick {
                filterData = it
                genre = filterData.selection.genre?.apiValue()
                changedSettings()
            }
        }.also { header = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.genre = it.getString("genre")
        }
        filterData = getFilterData()
    }
}
