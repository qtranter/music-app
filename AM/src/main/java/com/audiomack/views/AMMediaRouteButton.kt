package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import androidx.mediarouter.app.MediaRouteButton
import com.audiomack.data.device.DeviceRepository

class AMMediaRouteButton : MediaRouteButton {

    interface CastAvailableClickListener {
        /**
         * @param available: whether the device supports casting
         */
        fun onCastAvailable(available: Boolean)
    }

    var castAvailableClickListener: CastAvailableClickListener? = null

    private val castAvailable: Boolean
        get() = DeviceRepository.castAvailable

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun performClick(): Boolean {
        castAvailableClickListener?.let {
            it.onCastAvailable(castAvailable)
            return false
        } ?: return super.performClick()
    }
}
