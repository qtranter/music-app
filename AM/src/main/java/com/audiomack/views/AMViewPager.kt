package com.audiomack.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.audiomack.data.tracking.TrackingRepository
import com.audiomack.utils.addOnPageSelectedListener

class AMViewPager : ViewPager {

    private var startDragX: Float = 0.toFloat()
    private var overscrollListener: ViewPagerOverscrollListener? = null
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID

    private val trackingRepository = TrackingRepository()

    interface ViewPagerOverscrollListener {
        fun didOverscrollRight()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        addOnPageSelectedListener { position ->
            (adapter as? FragmentStatePagerAdapter)?.getItem(position)?.let { fragment ->
                trackingRepository.trackBreadcrumb("${fragment.javaClass.simpleName} - tab selected")
            }
        }
    }

    fun setOverscrollListener(overscrollListener: ViewPagerOverscrollListener) {
        this.overscrollListener = overscrollListener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return try {
            overscrollListener?.let {
                if (adapter != null && currentItem == adapter!!.count - 1) {
                    val action = ev.action
                    when (action and MotionEvent.ACTION_MASK) {
                        MotionEvent.ACTION_DOWN -> {
                            activePointerId = ev.getPointerId(0)
                            startDragX = ev.getX(activePointerId)
                        }
                        MotionEvent.ACTION_UP -> {
                            if (ev.getX(activePointerId) < startDragX) {
                                it.didOverscrollRight()
                            } else {
                                startDragX = 0f
                            }
                            activePointerId = MotionEvent.INVALID_POINTER_ID
                        }
                        MotionEvent.ACTION_CANCEL -> activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                } else {
                    startDragX = 0f
                }
            }
            super.onTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
