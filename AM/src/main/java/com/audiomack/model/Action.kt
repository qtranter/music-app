package com.audiomack.model

import android.view.View

data class Action(
    val title: String?,
    val isSelected: Boolean,
    val listener: ActionListener?,
    var view: View? = null,
    var drawableLeft: Int? = -1
) {

    constructor(title: String?, listener: ActionListener?) : this (title, false, listener, null)

    constructor(title: String?, isSelected: Boolean, listener: ActionListener?) : this (title, isSelected, listener, null)
    constructor(title: String?, isSelected: Boolean, drawableLeft: Int, listener: ActionListener?) : this (title, isSelected, listener, null, drawableLeft)

    interface ActionListener {
        fun onActionExecuted()
    }
}
