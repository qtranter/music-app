package com.audiomack.ui.notifications.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.audiomack.R
import com.audiomack.model.NotificationPreferenceType
import com.audiomack.model.NotificationPreferenceTypeValue
import com.audiomack.utils.intentForNotificationSettings
import com.audiomack.views.AMCustomSwitch
import kotlinx.android.synthetic.main.activity_notifications_preferences.*

class NotificationsPreferencesActivity : AppCompatActivity() {

    private val viewModel: NotificationsPreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications_preferences)

        val switchesList = listOf(
            SwitchData(switchNewSongAlbum, NotificationPreferenceType.NewSongAlbum, viewModel.newSongAlbumEnabled),
            SwitchData(switchWeeklyArtistReports, NotificationPreferenceType.WeeklyArtistReport, viewModel.weeklyArtistReportsEnabled),
            SwitchData(switchPlayBenchmark, NotificationPreferenceType.PlayMilestones, viewModel.playMilestonesEnabled),
            SwitchData(switchCommentReplies, NotificationPreferenceType.CommentReplies, viewModel.commentRepliesEnabled),
            SwitchData(switchUpvoteMilestones, NotificationPreferenceType.UpvoteMilestones, viewModel.upvoteMilestonesEnabled),
            SwitchData(switchVerifiedPlaylistAdds, NotificationPreferenceType.VerifiedPlaylistAdds, viewModel.verifiedPlaylistAddsEnabled)
        )
        initObservers(switchesList)
        initActions(switchesList)
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchNotificationsEnabledStatus()
    }

    private fun initObservers(switchesList: List<SwitchData>) {
        viewModel.apply {
            closeEvent.observe(this@NotificationsPreferencesActivity) { finish() }
            openOSNotificationSettingsEvent.observe(this@NotificationsPreferencesActivity) {
                startActivity(intentForNotificationSettings())
            }
            notificationsDisabledVisible.observe(this@NotificationsPreferencesActivity) {
                layoutNotificationsOff.visibility = if (it) View.VISIBLE else View.GONE
            }
            switchesList.forEach { (switchView, _, liveData) ->
                liveData.observe(this@NotificationsPreferencesActivity) { enabled ->
                    switchView.setCheckedProgrammatically(enabled)
                }
            }
        }
    }

    private fun initActions(switchesList: List<SwitchData>) {
        buttonBack.setOnClickListener { viewModel.onBackClicked() }
        layoutNotificationsOff.setOnClickListener { viewModel.onNotificationsDisabledClicked() }
        switchesList.forEach {
            initSwitchListener(it.switchView, it.type)
        }
    }

    private fun initSwitchListener(switchView: AMCustomSwitch, type: NotificationPreferenceType) {
        switchView.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onPreferenceChanged(NotificationPreferenceTypeValue(type, isChecked))
        }
    }

    companion object {
        fun getLaunchIntent(context: Context): Intent =
            Intent(context, NotificationsPreferencesActivity::class.java)
    }
}

data class SwitchData(
    val switchView: AMCustomSwitch,
    val type: NotificationPreferenceType,
    val liveData: LiveData<Boolean>
)
