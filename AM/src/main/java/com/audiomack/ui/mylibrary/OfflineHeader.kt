package com.audiomack.ui.mylibrary

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.audiomack.R
import com.audiomack.ui.filter.FilterData
import java.util.Locale
import kotlinx.android.synthetic.main.header_offline_filters.view.layoutTypeSort

class OfflineHeader : LinearLayout {

    private lateinit var tvType: TextView
    private lateinit var tvSort: TextView
    private lateinit var buttonShuffle: ImageButton
    private lateinit var buttonEdit: ImageButton

    var filter: FilterData? = null
        set(value) {
            field = value
            updateView()
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.header_offline_filters, this)
        this.tvType = findViewById(R.id.tvType)
        this.tvSort = findViewById(R.id.tvSort)
        this.buttonShuffle = findViewById(R.id.buttonShuffle)
        this.buttonEdit = findViewById(R.id.buttonEdit)
    }

    private fun updateView() {
        filter?.let {
            tvType.text = it.selection.type?.humanValue(tvType.context)?.toUpperCase(Locale.getDefault())
            tvSort.text = it.selection.sort?. humanValue(tvType.context)?.toUpperCase(Locale.getDefault())
        }
    }

    fun onFilterClick(listener: () -> Unit) {
        layoutTypeSort.setOnClickListener { listener.invoke() }
    }

    fun onShuffleButtonClick(listener: () -> Unit) {
        buttonShuffle.setOnClickListener { listener.invoke() }
    }

    fun onEditButtonClick(listener: () -> Unit) {
        buttonEdit.setOnClickListener { listener.invoke() }
    }
}
