package com.audiomack.ui.highlights

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomack.R
import com.audiomack.data.preferences.PreferencesRepository
import com.audiomack.data.user.UserData
import com.audiomack.model.AMResultItem
import kotlinx.android.synthetic.main.header_uploads.view.*

class HighlightsHeaderView : ConstraintLayout {

    private lateinit var adapter: HighlightsAdapter

    private val preferencesDataSource = PreferencesRepository()

    var highlightsInterface: HighlightsInterface? = null

    var myAccount: Boolean = false
        set(value) {
            field = value
            adapter.myAccount = value
            adapter.notifyDataSetChanged()
        }

    var highlights: List<AMResultItem> = emptyList()

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        commonInit()
    }

    private fun commonInit() {
        inflate(context, R.layout.header_uploads, this)

        adapter = HighlightsAdapter(
            myAccount,
            mutableListOf(),
            { music -> highlightsInterface?.didTapOnMusic(music, highlights) },
            { music, position -> highlightsInterface?.didTapOnMusicMenu(music, position) }
        )

        recyclerView.layoutManager = LinearLayoutManager(
            recyclerView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        buttonEdit.setOnClickListener { highlightsInterface?.didTapOnEdit() }

        buttonClose.setOnClickListener {
            preferencesDataSource.needToShowHighlightsPlaceholder = false
            updateRecyclerView()
        }

        if (myAccount) {
            highlights = UserData.getHighlights()
        }

        updateRecyclerView()
    }

    fun updateHighlights(highlights: List<AMResultItem>) {
        this.highlights = highlights
        updateRecyclerView()
    }

    fun updateRecyclerView() {
        adapter.reload(highlights)
        recyclerView.visibility = if (highlights.isNotEmpty()) View.VISIBLE else View.GONE
        placeholderLayout.visibility = if (myAccount && preferencesDataSource.needToShowHighlightsPlaceholder && UserData.getHighlights().isEmpty()) View.VISIBLE else View.GONE
        headerHighlights.visibility = if (highlights.isEmpty() && (!myAccount || !preferencesDataSource.needToShowHighlightsPlaceholder)) View.GONE else View.VISIBLE
        tvRecent.visibility = headerHighlights.visibility
        buttonEdit.visibility = if (myAccount && highlights.isNotEmpty()) View.VISIBLE else View.GONE
    }

    fun removeItem(position: Int) {
        highlights = highlights.toMutableList().also { it.removeAt(position) }
        adapter.removeItem(position)
        if (adapter.items.isEmpty()) {
            updateRecyclerView()
        }
    }
}

interface HighlightsInterface {
    fun didTapOnEdit()
    fun didTapOnMusic(music: AMResultItem, highlights: List<AMResultItem>)
    fun didTapOnMusicMenu(music: AMResultItem, position: Int)
}
