package com.audiomack.ui.mylibrary

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import com.audiomack.R
import com.audiomack.model.AMMusicType
import com.audiomack.ui.filter.FilterData

class FavoritesHeader : LinearLayout {

    private lateinit var buttonShuffle: ImageButton
    private lateinit var radioGroup: RadioGroup

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
        View.inflate(context, R.layout.header_favorites, this)
        this.buttonShuffle = findViewById(R.id.buttonShuffle)
        this.radioGroup = findViewById(R.id.radioGroup)
    }

    private fun updateView() {
        radioGroup.check(radioGroup.getChildAt(filter?.selection?.type?.ordinal ?: 0).id)
    }

    fun onCheckChanged(listener: (AMMusicType) -> Unit) {
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val index =
                (0 until group.childCount).indexOfFirst { checkedId == group.getChildAt(it).id }
                    .takeIf { it != -1 } ?: 0
            val selectedFilter = AMMusicType.values()[index]
            listener(selectedFilter)
        }
    }

    fun onShuffleButtonClick(listener: () -> Unit) {
        buttonShuffle.setOnClickListener {
            listener.invoke()
        }
    }
}
