package com.audiomack.model

import android.content.Context
import com.audiomack.R

enum class BenchmarkType(val stringCode: String) {
    NONE("none"),
    PLAY("plays"),
    FAVORITE("favorites"),
    PLAYLIST("playlists"),
    REPOST("reposts"),
    VERIFIED("verified"),
    TASTEMAKER("tastemaker"),
    AUTHENTICATED("authenticated"),
    ON_AUDIOMACK("onAudiomack");

    fun getTitle(context: Context): String {
        return when (this) {
            NONE -> context.getString(R.string.benchmark_playing)
            PLAY -> context.getString(R.string.benchmark_play)
            FAVORITE -> context.getString(R.string.benchmark_favorite)
            PLAYLIST -> context.getString(R.string.benchmark_playlist)
            REPOST -> context.getString(R.string.benchmark_repost)
            VERIFIED, TASTEMAKER, AUTHENTICATED -> context.getString(R.string.benchmark_artist)
            ON_AUDIOMACK -> context.getString(R.string.benchmark_on_audiomack)
        }
    }

    companion object {
        fun fromString(stringCode: String): BenchmarkType {
            return values().firstOrNull { it.stringCode == stringCode } ?: NONE
        }
    }
}
