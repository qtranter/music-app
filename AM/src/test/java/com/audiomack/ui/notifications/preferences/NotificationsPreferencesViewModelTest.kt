package com.audiomack.ui.notifications.preferences

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.audiomack.data.api.NotificationSettingsDataSource
import com.audiomack.data.api.NotificationsEnabledResult
import com.audiomack.model.NotificationPreferenceType
import com.audiomack.model.NotificationPreferenceTypeValue
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class NotificationsPreferencesViewModelTest {

    // Dependencies
    @Mock private lateinit var notificationSettingsDataSource: NotificationSettingsDataSource
    private lateinit var schedulersProvider: SchedulersProvider

    // System under test
    private lateinit var viewModel: NotificationsPreferencesViewModel

    // Observers
    @Mock private lateinit var observerClose: Observer<Void>
    @Mock private lateinit var observerOpenOSNotificationSettingsEvent: Observer<Void>
    @Mock private lateinit var observerNotificationsDisabledVisible: Observer<Boolean>
    @Mock private lateinit var observerNewSongAlbumEnabled: Observer<Boolean>
    @Mock private lateinit var observerWeeklyArtistReportsEnabled: Observer<Boolean>
    @Mock private lateinit var observerPlayMilestonesEnabled: Observer<Boolean>
    @Mock private lateinit var observerCommentRepliesEnabled: Observer<Boolean>
    @Mock private lateinit var observerUpvoteMilestonesEnabled: Observer<Boolean>
    @Mock private lateinit var observerVerifiedPlaylistAddsEnabled: Observer<Boolean>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        schedulersProvider = TestSchedulersProvider()
        whenever(notificationSettingsDataSource.areNotificationsEnabledForNewMusic()).thenReturn(Single.just(NotificationsEnabledResult.Enabled))
        whenever(notificationSettingsDataSource.getNotificationPreferences()).thenReturn(Single.just(
            listOf(
                NotificationPreferenceTypeValue(NotificationPreferenceType.NewSongAlbum, true),
                NotificationPreferenceTypeValue(NotificationPreferenceType.WeeklyArtistReport, false),
                NotificationPreferenceTypeValue(NotificationPreferenceType.PlayMilestones, true),
                NotificationPreferenceTypeValue(NotificationPreferenceType.CommentReplies, false)
            )
        ))
        viewModel = NotificationsPreferencesViewModel(notificationSettingsDataSource, schedulersProvider).apply {
            closeEvent.observeForever(observerClose)
            openOSNotificationSettingsEvent.observeForever(observerOpenOSNotificationSettingsEvent)
            notificationsDisabledVisible.observeForever(observerNotificationsDisabledVisible)
            newSongAlbumEnabled.observeForever(observerNewSongAlbumEnabled)
            weeklyArtistReportsEnabled.observeForever(observerWeeklyArtistReportsEnabled)
            playMilestonesEnabled.observeForever(observerPlayMilestonesEnabled)
            commentRepliesEnabled.observeForever(observerCommentRepliesEnabled)
            upvoteMilestonesEnabled.observeForever(observerUpvoteMilestonesEnabled)
            verifiedPlaylistAddsEnabled.observeForever(observerVerifiedPlaylistAddsEnabled)
        }
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `subscription on init`() {
        verify(observerNotificationsDisabledVisible).onChanged(false)
        verify(notificationSettingsDataSource, times(1)).getNotificationPreferences()
        // True
        verify(observerNewSongAlbumEnabled, times(1)).onChanged(true)
        verify(observerPlayMilestonesEnabled, times(1)).onChanged(true)
        // False
        verify(observerWeeklyArtistReportsEnabled, times(1)).onChanged(false)
        verify(observerCommentRepliesEnabled, times(1)).onChanged(false)
        // Missing from response, defaults to false
        verify(observerUpvoteMilestonesEnabled, times(1)).onChanged(false)
        verify(observerVerifiedPlaylistAddsEnabled, times(1)).onChanged(false)
    }

    @Test
    fun `back button clicked`() {
        viewModel.onBackClicked()
        verify(observerClose).onChanged(null)
    }

    @Test
    fun `observe click on notifications disabled view`() {
        viewModel.onNotificationsDisabledClicked()
        verify(observerOpenOSNotificationSettingsEvent).onChanged(null)
    }

    @Test
    fun `observe notifications enabled`() {
        whenever(notificationSettingsDataSource.areNotificationsEnabledForNewMusic())
            .thenReturn(Single.just(NotificationsEnabledResult.Enabled))
        viewModel.fetchNotificationsEnabledStatus()
        verify(observerNotificationsDisabledVisible, times(2)).onChanged(false) // First on init then on fetchNotificationsEnabledStatus()
    }

    @Test
    fun `observe notifications disabled at OS level`() {
        whenever(notificationSettingsDataSource.areNotificationsEnabledForNewMusic())
            .thenReturn(Single.just(NotificationsEnabledResult.DisabledAtOSLevel))
        viewModel.fetchNotificationsEnabledStatus()
        verify(observerNotificationsDisabledVisible).onChanged(true)
    }

    @Test
    fun `on new song or album changed, success`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(true))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.NewSongAlbum, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerNewSongAlbumEnabled, atLeast(1)).onChanged(true)
    }

    @Test
    fun `on new song or album changed, failure`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(false))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.NewSongAlbum, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerNewSongAlbumEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on new song or album changed, exception`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.error(Exception("Test exception")))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.NewSongAlbum, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerNewSongAlbumEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on weekly artist reports changed, success`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(true))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.WeeklyArtistReport, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerWeeklyArtistReportsEnabled, atLeast(1)).onChanged(true)
    }

    @Test
    fun `on weekly artist reports changed, failure`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(false))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.WeeklyArtistReport, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerWeeklyArtistReportsEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on weekly artist reports changed, exception`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.error(Exception("Test exception")))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.WeeklyArtistReport, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerWeeklyArtistReportsEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on play milestones changed, success`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(true))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.PlayMilestones, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerPlayMilestonesEnabled, atLeast(1)).onChanged(true)
    }

    @Test
    fun `on play milestones changed, failure`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(false))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.PlayMilestones, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerPlayMilestonesEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on play milestones changed, exception`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.error(Exception("Test exception")))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.PlayMilestones, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerPlayMilestonesEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on comment replies changed, success`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(true))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.CommentReplies, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerCommentRepliesEnabled, atLeast(1)).onChanged(true)
    }

    @Test
    fun `on comment replies changed, failure`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(false))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.CommentReplies, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerCommentRepliesEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on comment replies changed, exception`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.error(Exception("Test exception")))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.CommentReplies, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerCommentRepliesEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on upvote milestones changed, success`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(true))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.UpvoteMilestones, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerUpvoteMilestonesEnabled, atLeast(1)).onChanged(true)
    }

    @Test
    fun `on upvote milestones changed, failure`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(false))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.UpvoteMilestones, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerUpvoteMilestonesEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on upvote milestones changed, exception`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.error(Exception("Test exception")))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.UpvoteMilestones, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerUpvoteMilestonesEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on verified playlist adds changed, success`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(true))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.VerifiedPlaylistAdds, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerVerifiedPlaylistAddsEnabled, atLeast(1)).onChanged(true)
    }

    @Test
    fun `on verified playlist adds changed, failure`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.just(false))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.VerifiedPlaylistAdds, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerVerifiedPlaylistAddsEnabled, atLeast(1)).onChanged(false)
    }

    @Test
    fun `on verified playlist adds changed, exception`() {
        whenever(notificationSettingsDataSource.setNotificationPreference(anyOrNull())).thenReturn(Single.error(Exception("Test exception")))
        val input = NotificationPreferenceTypeValue(NotificationPreferenceType.VerifiedPlaylistAdds, true)
        viewModel.onPreferenceChanged(input)
        verify(notificationSettingsDataSource).setNotificationPreference(eq(input))
        verify(observerVerifiedPlaylistAddsEnabled, atLeast(1)).onChanged(false)
    }
}
