package com.audiomack.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.MixpanelPageNotifications
import com.audiomack.fragments.DataFragment
import com.audiomack.model.AMNotification
import com.audiomack.model.AMResultItem
import com.audiomack.model.APIRequestData
import com.audiomack.model.CellType
import com.audiomack.model.MixpanelSource
import com.audiomack.network.API
import com.audiomack.utils.convertDpToPixel

class DataNotificationsFragment : DataFragment(TAG) {

    private val notificationsViewModel by activityViewModels<NotificationsViewModel>()

    override fun apiCallObservable(): APIRequestData? {
        super.apiCallObservable()
        return API.getInstance()
            .getUserNotifications(if (currentPage == 0) null else pagingToken, true)
    }

    override fun getCellType(): CellType {
        return CellType.NOTIFICATION
    }

    override fun placeholderCustomView(): View {
        return LayoutInflater.from(context).inflate(R.layout.view_placeholder, null)
    }

    override fun configurePlaceholderView(placeholderView: View) {
        placeholderView.findViewById<ImageView>(R.id.imageView).visibility = View.GONE
        placeholderView.findViewById<TextView>(R.id.tvMessage).setText(R.string.notifications_noresults_placeholder)
        placeholderView.findViewById<Button>(R.id.cta).visibility = View.GONE
    }

    override fun additionalTopPadding(): Int {
        return context?.convertDpToPixel(5f) ?: 0
    }

    override fun onClickNotificationBundledPlaylists(
        playlists: List<AMResultItem>,
        type: AMNotification.NotificationType
    ) {
        super.onClickNotificationBundledPlaylists(playlists, type)
        notificationsViewModel.onRequestedPlaylistsGrid(playlists)
    }

    override fun getMixpanelSource(): MixpanelSource =
        MixpanelSource(MainApplication.currentTab, MixpanelPageNotifications)

    companion object {
        private const val TAG = "DataNotificationsFragment"

        fun newInstance(): DataNotificationsFragment {
            return DataNotificationsFragment()
        }
    }
}
