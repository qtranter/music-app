package com.audiomack.utils

import android.annotation.SuppressLint
import android.content.Context
import com.audiomack.MainApplication

interface GeneralPreferences {
    fun needToShowPlayerPlaylistTooltip(): Boolean
    fun needToShowPlayerQueueTooltip(): Boolean
    fun needToShowPlayerScrollTooltip(): Boolean
    fun needToShowPlayerEqTooltip(): Boolean
    fun needToShowDownloadInAppMessage(): Boolean
    fun needToShowLimitedDownloadInAppMessage(): Boolean
    fun needToShowMiniplayerTooltip(): Boolean
    fun needToShowAlbumDownloadTooltip(): Boolean
    fun needToShowPlaylistFavoriteTooltip(): Boolean
    fun needToShowPlaylistDownloadTooltip(): Boolean
    val playCount: Long
    fun setPlayerPlaylistTooltipShown()
    fun setPlayerQueueTooltipShown()
    fun setPlayerScrollTooltipShown()
    fun setPlayerEqTooltipShown()
    fun setDownloadInAppMessageShown()
    fun setLimitedDownloadInAppMessageShown()
    fun setMiniplayerTooltipShown()
    fun setAlbumDownloadTooltipShown()
    fun setPlaylistFavoriteTooltipShown()
    fun setPlaylistDownloadTooltipShown()
    fun incrementPlayCount()
}

class GeneralPreferencesImpl : GeneralPreferences {

    private var context: Context = MainApplication.context ?: throw IllegalStateException()

    private var helper: GeneralPreferencesHelper = GeneralPreferencesHelper.getInstance(context)

    override fun needToShowPlayerPlaylistTooltip(): Boolean =
        helper.needToShowPlayerPlaylistTooltip(context)

    override fun needToShowPlayerQueueTooltip(): Boolean =
        helper.needToShowPlayerQueueTooltip(context)

    override fun needToShowPlayerScrollTooltip(): Boolean =
        helper.needToShowPlayerScrollTooltip(context)

    override fun needToShowPlayerEqTooltip(): Boolean =
        helper.needToShowPlayerEqTooltip(context)

    override fun needToShowDownloadInAppMessage(): Boolean =
        helper.needToShowDownloadInAppMessage(context)

    override fun needToShowLimitedDownloadInAppMessage(): Boolean =
        helper.needToShowLimitedDownloadInAppMessage(context)

    override fun needToShowMiniplayerTooltip(): Boolean =
        helper.needToShowMiniplayerTooltip(context)

    override fun needToShowAlbumDownloadTooltip(): Boolean =
        helper.needToShowAlbumFavoriteTooltip(context)

    override fun needToShowPlaylistFavoriteTooltip(): Boolean =
        helper.needToShowPlaylistShuffleTooltip(context)

    override fun needToShowPlaylistDownloadTooltip(): Boolean =
        helper.needToShowPlaylistDownloadTooltip(context)

    override fun setPlayerPlaylistTooltipShown() {
        helper.setPlayerPlaylistTooltipShown(context)
    }

    override fun setPlayerQueueTooltipShown() {
        helper.setPlayerQueueTooltipShown(context)
    }

    override fun setPlayerScrollTooltipShown() {
        helper.setPlayerScrollTooltipShown(context)
    }

    override fun setPlayerEqTooltipShown() {
        helper.setPlayerEqTooltipShown(context)
    }

    override fun setDownloadInAppMessageShown() {
        helper.setDownloadInAppMessageShown(context)
    }

    override fun setLimitedDownloadInAppMessageShown() {
        helper.setLimitedDownloadInAppMessageShown(context)
    }

    override fun setMiniplayerTooltipShown() {
        helper.setMiniplayerTooltipShown(context)
    }

    override fun setAlbumDownloadTooltipShown() {
        helper.setAlbumFavoriteTooltipShown(context)
    }

    override fun setPlaylistFavoriteTooltipShown() {
        helper.setPlaylistShuffleTooltipShown(context)
    }

    override fun setPlaylistDownloadTooltipShown() {
        helper.setPlaylistDownloadTooltipShown(context)
    }

    override val playCount: Long = helper.getPlayCount(context)

    override fun incrementPlayCount() = helper.incrementPlayCount(context)

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: GeneralPreferencesImpl? = null

        @JvmStatic
        fun getInstance(): GeneralPreferencesImpl =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GeneralPreferencesImpl().also { INSTANCE = it }
            }
    }
}
