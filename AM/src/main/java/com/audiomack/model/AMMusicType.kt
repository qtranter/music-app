package com.audiomack.model

import android.content.Context
import com.audiomack.R

enum class AMMusicType {
    All {
        override fun humanValue(context: Context?): String = context?.getString(R.string.favorites_filter_allmusic) ?: ""
    }, Songs {
        override fun humanValue(context: Context?): String = context?.getString(R.string.favorites_filter_songs) ?: ""
    }, Albums {
        override fun humanValue(context: Context?): String = context?.getString(R.string.favorites_filter_albums) ?: ""
    }, Playlists {
        override fun humanValue(context: Context?): String = context?.getString(R.string.favorites_filter_playlists) ?: ""
    };

    abstract fun humanValue(context: Context?): String
}
