package com.audiomack.usecases.download

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.audiomack.model.AMResultItem
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.utils.confirmDownloadDeletion
import com.audiomack.utils.confirmPlaylistDownloadDeletion
import com.audiomack.utils.confirmPlaylistSync
import com.audiomack.utils.showDownloadUnlockedToast
import com.audiomack.utils.showFailedPlaylistDownload

class DownloadEventsHandler(
    private val fragment: Fragment,
    private val events: DownloadEvents,
    private val onPlaylistSyncConfirmed: (playlist: AMResultItem) -> Unit
) {

    private val viewLifecycleOwner: LifecycleOwner
        get() = fragment.viewLifecycleOwner

    init {
        initNavigationEventObservers()
    }

    private fun initNavigationEventObservers() {
        with(events) {

            showConfirmDownloadDeletionEvent.observe(viewLifecycleOwner, { music ->
                fragment.confirmDownloadDeletion(music, null)
            })

            showConfirmPlaylistDownloadDeletionEvent.observe(
                viewLifecycleOwner,
                { music: AMResultItem ->
                    fragment.confirmPlaylistDownloadDeletion(music)
                })

            showFailedPlaylistDownloadEvent.observe(viewLifecycleOwner, {
                fragment.showFailedPlaylistDownload()
            })

            showConfirmPlaylistSyncEvent.observe(viewLifecycleOwner, { (playlist, tracksCount) ->
                fragment.confirmPlaylistSync(tracksCount) {
                    onPlaylistSyncConfirmed(playlist)
                }
            })

            showPremiumDownloadEvent.observe(viewLifecycleOwner, { model ->
                (fragment.activity as? HomeActivity?)?.requestPremiumDownloads(model)
            })

            showUnlockedToastEvent.observe(viewLifecycleOwner, { musicName ->
                fragment.showDownloadUnlockedToast(musicName)
            })

            showPremiumEvent.observe(viewLifecycleOwner, { mode ->
                InAppPurchaseActivity.show(fragment.activity, mode)
            })
        }
    }
}

fun Fragment.setupDownloadHandler(
    events: DownloadEvents,
    onPlaylistSyncConfirmed: (playlist: AMResultItem) -> Unit
): DownloadEventsHandler {
    return DownloadEventsHandler(this, events, onPlaylistSyncConfirmed)
}
