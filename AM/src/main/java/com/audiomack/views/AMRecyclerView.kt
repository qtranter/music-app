package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/** [RecyclerView] subclass with the ability to listen to scroll changes **/
class AMRecyclerView : RecyclerView {

    var listener: ScrollListener? = null
        set(value) {
            offsetY = computeVerticalScrollOffset()
            field = value
        }

    var offsetY = 0
        private set

    fun reduceOffsetYBy(reducedSize: Int) {
        offsetY -= reducedSize
    }

    interface ScrollListener {
        fun onScroll()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        offsetY += dy
        listener?.onScroll()
    }
}
