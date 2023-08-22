package com.audiomack.model

import android.widget.TextView
import com.audiomack.data.preferences.DefaultGenre

class GenreModel(
    val button: TextView,
    val genreKey: DefaultGenre,
    val googleAnalyticsKey: String,
    val leanplumKey: String,
    val selected: Boolean
)
