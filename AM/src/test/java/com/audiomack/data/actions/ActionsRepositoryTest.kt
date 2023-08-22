package com.audiomack.data.actions

import com.audiomack.data.ads.AdsDataSource
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.api.MusicDataSource
import com.audiomack.data.api.NotificationSettingsDataSource
import com.audiomack.data.api.NotificationsEnabledResult
import com.audiomack.data.inapprating.InAppRating
import com.audiomack.data.premium.PremiumDataSource
import com.audiomack.data.premiumdownload.PremiumDownloadDataSource
import com.audiomack.data.reachability.ReachabilityDataSource
import com.audiomack.data.tracking.TrackingDataSource
import com.audiomack.data.tracking.appsflyer.AppsFlyerDataSource
import com.audiomack.data.tracking.mixpanel.MixpanelDataSource
import com.audiomack.data.user.UserDataSource
import com.audiomack.data.widget.WidgetDataSource
import com.audiomack.download.MusicDownloader
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventDownload
import com.audiomack.model.EventDownloadsEdited
import com.audiomack.model.EventFavoriteStatusChanged
import com.audiomack.model.EventFollowChange
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.MixpanelSource
import com.audiomack.model.PremiumLimitedDownloadAlertViewType
import com.audiomack.model.PremiumOnlyDownloadAlertViewType
import com.audiomack.network.APIInterface
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.lang.IllegalArgumentException
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class ActionsRepositoryTest {

    @Mock
    private lateinit var reachabilityDataSource: ReachabilityDataSource

    @Mock
    private lateinit var userDataSource: UserDataSource

    @Mock
    private lateinit var premiumDataSource: PremiumDataSource

    @Mock
    private lateinit var artistsDataSource: ArtistsDataSource

    @Mock
    private lateinit var musicDataSource: MusicDataSource

    @Mock
    private lateinit var widgetDataSource: WidgetDataSource

    @Mock
    private lateinit var inAppRating: InAppRating

    @Mock
    private lateinit var mixpanelDataSource: MixpanelDataSource

    @Mock
    private lateinit var appsFlyerDataSource: AppsFlyerDataSource

    @Mock
    private lateinit var trackingDataSource: TrackingDataSource

    @Mock
    private lateinit var musicDownloader: MusicDownloader

    @Mock
    private lateinit var apiDownloads: APIInterface.DownloadsInterface

    @Mock
    private lateinit var premiumDownloadDataSource: PremiumDownloadDataSource

    @Mock
    private lateinit var notificationSettings: NotificationSettingsDataSource

    @Mock
    private lateinit var adsManager: AdsDataSource

    @Mock
    private lateinit var eventBus: EventBus

    private val mixpanelButton = ""

    private val mixpanelSource = MixpanelSource.empty

    private lateinit var sut: ActionsRepository

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = ActionsRepository(
            reachabilityDataSource,
            userDataSource,
            premiumDataSource,
            artistsDataSource,
            musicDataSource,
            widgetDataSource,
            inAppRating,
            mixpanelDataSource,
            appsFlyerDataSource,
            trackingDataSource,
            musicDownloader,
            apiDownloads,
            premiumDownloadDataSource,
            notificationSettings,
            adsManager,
            eventBus
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    // Favorite

    @Test
    fun `favorite, not reachable`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        sut.toggleFavorite(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleFavoriteException.Offline }
            .dispose()
    }

    @Test
    fun `favorite, not logged in`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        sut.toggleFavorite(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleFavoriteException.LoggedOut }
            .dispose()
    }

    @Test
    fun `favorite playlist, API success`() {
        val isSuccessful = true
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicFavorited(any())).thenReturn(false)
        whenever(musicDataSource.favorite(any(), any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleFavorite(mock {
            on { this.isPlaylist } doReturn true
            on { this.itemId } doReturn "123"
            on { this.isFavorited } doReturn false
            on { this.title } doReturn "title"
            on { this.artist } doReturn "artist"
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFavoriteResult.Notify(isSuccessful, true, true, false, false, "title", "artist"))
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(1)).updateFavoriteStatus(true)
        verify(widgetDataSource, times(0)).updateFavoriteStatus(false)
        verify(musicDataSource).favorite(any(), any())
        verify(mixpanelDataSource).trackAddToFavorites(any(), any(), any())
        verify(appsFlyerDataSource).trackAddToFavorites()
        verify(trackingDataSource).trackGA(any(), any(), any())
        verify(inAppRating).incrementFavoriteCount()
        verify(inAppRating).request()
        verify(eventBus, times(1)).post(argWhere { it is EventFavoriteStatusChanged })
        verify(userDataSource).addMusicToFavorites(any())
        verify(userDataSource, times(0)).removeMusicFromFavorites(any())
    }

    @Test
    fun `favorite playlist, API failure`() {
        val isSuccessful = false
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicFavorited(any())).thenReturn(false)
        whenever(musicDataSource.favorite(any(), any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleFavorite(mock {
            on { this.isPlaylist } doReturn true
            on { this.itemId } doReturn "123"
            on { this.isFavorited } doReturn false
            on { this.title } doReturn "title"
            on { this.artist } doReturn "artist"
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFavoriteResult.Notify(isSuccessful, true, true, false, false, "title", "artist"))
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(1)).updateFavoriteStatus(true)
        verify(widgetDataSource, times(1)).updateFavoriteStatus(false)
        verify(musicDataSource).favorite(any(), any())
        verifyZeroInteractions(mixpanelDataSource)
        verifyZeroInteractions(appsFlyerDataSource)
        verifyZeroInteractions(trackingDataSource)
        verify(inAppRating).incrementFavoriteCount()
        verify(inAppRating).request()
        verify(eventBus, times(2)).post(argWhere { it is EventFavoriteStatusChanged })
        verify(userDataSource).addMusicToFavorites(any())
        verify(userDataSource).removeMusicFromFavorites(any())
    }

    @Test
    fun `unfavorite song, API success`() {
        val isSuccessful = true
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicFavorited(any())).thenReturn(true)
        whenever(musicDataSource.unfavorite(any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleFavorite(mock {
            on { this.isSong } doReturn true
            on { this.itemId } doReturn "123"
            on { this.isFavorited } doReturn true
            on { this.title } doReturn "title"
            on { this.artist } doReturn "artist"
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFavoriteResult.Notify(isSuccessful, false, false, false, true, "title", "artist"))
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(0)).updateFavoriteStatus(true)
        verify(widgetDataSource, times(1)).updateFavoriteStatus(false)
        verify(musicDataSource).unfavorite(any())
        verifyZeroInteractions(mixpanelDataSource)
        verifyZeroInteractions(appsFlyerDataSource)
        verifyZeroInteractions(trackingDataSource)
        verifyZeroInteractions(inAppRating)
        verify(eventBus, times(1)).post(argWhere { it is EventFavoriteStatusChanged })
        verify(userDataSource).removeMusicFromFavorites(any())
        verify(userDataSource, times(0)).addMusicToFavorites(any())
    }

    @Test
    fun `unfavorite album, API failure`() {
        val isSuccessful = false
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicFavorited(any())).thenReturn(true)
        whenever(musicDataSource.unfavorite(any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleFavorite(mock {
            on { this.isAlbum } doReturn true
            on { this.itemId } doReturn "123"
            on { this.isFavorited } doReturn true
            on { this.title } doReturn "title"
            on { this.artist } doReturn "artist"
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFavoriteResult.Notify(isSuccessful, false, false, true, false, "title", "artist"))
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(1)).updateFavoriteStatus(true)
        verify(widgetDataSource, times(1)).updateFavoriteStatus(false)
        verify(musicDataSource).unfavorite(any())
        verifyZeroInteractions(mixpanelDataSource)
        verifyZeroInteractions(appsFlyerDataSource)
        verifyZeroInteractions(trackingDataSource)
        verifyZeroInteractions(inAppRating)
        verify(eventBus, times(2)).post(argWhere { it is EventFavoriteStatusChanged })
        verify(userDataSource).removeMusicFromFavorites(any())
        verify(userDataSource).addMusicToFavorites(any())
    }

    // Repost

    @Test
    fun `repost, not reachable`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        sut.toggleRepost(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleRepostException.Offline }
            .dispose()
    }

    @Test
    fun `repost, not logged in`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        sut.toggleRepost(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleRepostException.LoggedOut }
            .dispose()
    }

    @Test
    fun `repost, API success`() {
        val isSuccessful = true
        val isAlbum = true
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(musicDataSource.repost(any(), any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleRepost(mock {
            on { this.isAlbum } doReturn isAlbum
            on { this.title } doReturn "title"
            on { this.artist } doReturn "artist"
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleRepostResult.Notify(isSuccessful, isAlbum, "title", "artist"))
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(1)).updateRepostStatus(true)
        verify(widgetDataSource, times(0)).updateRepostStatus(false)
        verify(musicDataSource).repost(any(), any())
        verify(mixpanelDataSource).trackReUp(any(), any(), any())
        verify(inAppRating).request()
    }

    @Test
    fun `repost, API failure`() {
        val isSuccessful = false
        val isAlbum = false
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(musicDataSource.repost(any(), any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleRepost(mock {
            on { this.isAlbumTrack } doReturn isAlbum
            on { this.title } doReturn "title"
            on { this.artist } doReturn "artist"
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleRepostResult.Notify(isSuccessful, isAlbum, "title", "artist"))
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(1)).updateRepostStatus(true)
        verify(widgetDataSource, times(1)).updateRepostStatus(false)
        verify(musicDataSource).repost(any(), any())
        verify(mixpanelDataSource).trackReUp(any(), any(), any())
        verify(inAppRating).request()
    }

    @Test
    fun `unrepost, API success`() {
        val isSuccessful = true
        val isAlbum = true
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(musicDataSource.unrepost(any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleRepost(mock {
            on { this.isAlbum } doReturn isAlbum
            on { this.isReposted } doReturn true
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertNoValues()
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(0)).updateRepostStatus(true)
        verify(widgetDataSource, times(1)).updateRepostStatus(false)
        verify(musicDataSource).unrepost(any())
    }

    @Test
    fun `unrepost, API failure`() {
        val isSuccessful = false
        val isAlbum = false
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(musicDataSource.unrepost(any())).thenReturn(Observable.just(isSuccessful))
        sut.toggleRepost(mock {
            on { this.isAlbum } doReturn isAlbum
            on { this.isReposted } doReturn true
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertNoValues()
            .assertNoErrors()
            .dispose()
        verify(widgetDataSource, times(1)).updateRepostStatus(true)
        verify(widgetDataSource, times(1)).updateRepostStatus(false)
        verify(musicDataSource).unrepost(any())
    }

    // Follow

    @Test
    fun `follow, null inputs`() {
        sut.toggleFollow(null, null, mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is IllegalArgumentException }
            .dispose()
    }

    @Test
    fun `follow music, not reachable`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        sut.toggleFollow(mock(), null, mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleFollowException.Offline }
            .dispose()
    }

    @Test
    fun `follow music, not logged in`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        sut.toggleFollow(mock(), null, mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleFollowException.LoggedOut }
            .dispose()
    }

    @Test
    fun `follow music, API failure`() {
        val artistId = "123"
        val uploaderName = "Matteo"
        val uploaderSlug = "slug"
        val uploaderImage = "image"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isArtistFollowed(artistId)).thenReturn(false)
        whenever(artistsDataSource.follow(anyOrNull())).thenReturn(Observable.just(false))
        sut.toggleFollow(mock {
            on { this.uploaderId } doReturn artistId
            on { this.uploaderName } doReturn uploaderName
            on { this.uploaderSlug } doReturn uploaderSlug
            on { this.uploaderTinyImage } doReturn uploaderImage
        }, null, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFollowResult.Finished(true), ToggleFollowResult.Notify(true, uploaderName, uploaderSlug, uploaderImage), ToggleFollowResult.Finished(false))
            .assertNoErrors()
            .dispose()
        verify(eventBus).post(argWhere { it is EventFollowChange })
    }

    @Test
    fun `follow music, API success, notifications enabled`() {
        val artistId = "123"
        val uploaderName = "Matteo"
        val uploaderSlug = "slug"
        val uploaderImage = "image"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isArtistFollowed(artistId)).thenReturn(false)
        whenever(artistsDataSource.follow(anyOrNull())).thenReturn(Observable.just(true))
        whenever(notificationSettings.areNotificationsEnabledForNewMusic()).thenReturn(Single.just(NotificationsEnabledResult.Enabled))
        sut.toggleFollow(mock {
            on { this.uploaderId } doReturn artistId
            on { this.uploaderName } doReturn uploaderName
            on { this.uploaderSlug } doReturn uploaderSlug
            on { this.uploaderTinyImage } doReturn uploaderImage
        }, null, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFollowResult.Finished(true), ToggleFollowResult.Notify(true, uploaderName, uploaderSlug, uploaderImage), ToggleFollowResult.Finished(true))
            .assertNoErrors()
            .dispose()
        verify(userDataSource).addArtistToFollowing(eq(artistId))
        verify(mixpanelDataSource).trackFollowAccount(any(), eq(artistId), any(), any())
        verify(eventBus).post(argWhere { it is EventFollowChange })
    }

    @Test
    fun `follow music, API success, notifications disabled at OS level`() {
        val artistId = "123"
        val uploaderName = "Matteo"
        val uploaderSlug = "slug"
        val uploaderImage = "image"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isArtistFollowed(artistId)).thenReturn(false)
        whenever(artistsDataSource.follow(anyOrNull())).thenReturn(Observable.just(true))
        whenever(notificationSettings.areNotificationsEnabledForNewMusic()).thenReturn(Single.just(NotificationsEnabledResult.DisabledAtOSLevel))
        sut.toggleFollow(mock {
            on { this.uploaderId } doReturn artistId
            on { this.uploaderName } doReturn uploaderName
            on { this.uploaderSlug } doReturn uploaderSlug
            on { this.uploaderTinyImage } doReturn uploaderImage
        }, null, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFollowResult.Finished(true), ToggleFollowResult.Notify(true, uploaderName, uploaderSlug, uploaderImage), ToggleFollowResult.Finished(true), ToggleFollowResult.AskForPermission(PermissionRedirect.Settings))
            .assertNoErrors()
            .dispose()
        verify(userDataSource).addArtistToFollowing(eq(artistId))
        verify(mixpanelDataSource).trackFollowAccount(any(), eq(artistId), any(), any())
        verify(eventBus).post(argWhere { it is EventFollowChange })
    }

    @Test
    fun `follow music, API success, notifications disabled at app level`() {
        val artistId = "123"
        val uploaderName = "Matteo"
        val uploaderSlug = "slug"
        val uploaderImage = "image"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isArtistFollowed(artistId)).thenReturn(false)
        whenever(artistsDataSource.follow(anyOrNull())).thenReturn(Observable.just(true))
        whenever(notificationSettings.areNotificationsEnabledForNewMusic()).thenReturn(Single.just(NotificationsEnabledResult.DisabledAtAppLevel))
        sut.toggleFollow(mock {
            on { this.uploaderId } doReturn artistId
            on { this.uploaderName } doReturn uploaderName
            on { this.uploaderSlug } doReturn uploaderSlug
            on { this.uploaderTinyImage } doReturn uploaderImage
        }, null, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFollowResult.Finished(true), ToggleFollowResult.Notify(true, uploaderName, uploaderSlug, uploaderImage), ToggleFollowResult.Finished(true), ToggleFollowResult.AskForPermission(PermissionRedirect.NotificationsManager))
            .assertNoErrors()
            .dispose()
        verify(userDataSource).addArtistToFollowing(eq(artistId))
        verify(mixpanelDataSource).trackFollowAccount(any(), eq(artistId), any(), any())
        verify(eventBus).post(argWhere { it is EventFollowChange })
    }

    @Test
    fun `unfollow artist, API failure`() {
        val artistId = "123"
        val uploaderName = "Matteo"
        val uploaderSlug = "slug"
        val uploaderImage = "image"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isArtistFollowed(artistId)).thenReturn(true)
        whenever(artistsDataSource.unfollow(anyOrNull())).thenReturn(Observable.just(true))
        sut.toggleFollow(null, mock {
            on { this.artistId } doReturn artistId
            on { this.name } doReturn uploaderName
            on { this.urlSlug } doReturn uploaderSlug
            on { this.smallImage } doReturn uploaderImage
        }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFollowResult.Finished(false), ToggleFollowResult.Notify(false, uploaderName, uploaderSlug, uploaderImage), ToggleFollowResult.Finished(true))
            .assertNoErrors()
            .dispose()
        verify(eventBus).post(argWhere { it is EventFollowChange })
    }

    @Test
    fun `unfollow artist, API success`() {
        val artistId = "123"
        val uploaderName = "Matteo"
        val uploaderSlug = "slug"
        val uploaderImage = "image"
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isArtistFollowed(artistId)).thenReturn(true)
        whenever(artistsDataSource.unfollow(anyOrNull())).thenReturn(Observable.just(false))
        sut.toggleFollow(null,
            mock {
                on { this.artistId } doReturn artistId
                on { this.name } doReturn uploaderName
                on { this.urlSlug } doReturn uploaderSlug
                on { this.smallImage } doReturn uploaderImage
            }, mixpanelButton, mixpanelSource)
            .test()
            .assertValues(ToggleFollowResult.Finished(false), ToggleFollowResult.Notify(false, uploaderName, uploaderSlug, uploaderImage), ToggleFollowResult.Finished(false))
            .assertNoErrors()
            .dispose()
        verify(userDataSource).removeArtistFromFollowing(eq(artistId))
        verify(mixpanelDataSource).trackUnfollowAccount(any(), eq(artistId), any(), any())
        verify(eventBus).post(argWhere { it is EventFollowChange })
    }

    // Add to playlist

    @Test
    fun `add to playlist, logged out`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        sut.addToPlaylist(mock())
            .test()
            .assertError { it is AddToPlaylistException.LoggedOut }
            .dispose()
    }

    @Test
    fun `add to playlist, logged in`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        sut.addToPlaylist(mock())
            .test()
            .assertValue(true)
            .assertNoErrors()
            .dispose()
    }

    // Highlight

    @Test
    fun `highlight, not reachable`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(false)
        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleHighlightException.Offline }
            .dispose()
    }

    @Test
    fun `highlight, not logged in`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleHighlightException.LoggedOut }
            .dispose()
    }

    @Test
    fun `highlight, remove, success`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicHighlighted(any())).thenReturn(true)
        whenever(musicDataSource.removeFromHighlights(any())).thenReturn(Observable.just(true))

        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertNoErrors()
            .assertValues(ToggleHighlightResult.Removed)
            .dispose()

        verify(userDataSource).removeFromHighlights(any())
    }

    @Test
    fun `highlight, remove, failure`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicHighlighted(any())).thenReturn(true)
        whenever(musicDataSource.removeFromHighlights(any())).thenReturn(Observable.just(false))

        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleHighlightException.Failure }
            .dispose()

        verify(userDataSource, never()).removeFromHighlights(any())
    }

    @Test
    fun `highlight, add, reached limit`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicHighlighted(any())).thenReturn(false)
        whenever(userDataSource.highlightsCount).thenReturn(4)

        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleHighlightException.ReachedLimit }
            .dispose()

        verify(userDataSource, never()).addToHighlights(any())
        verify(mixpanelDataSource, never()).trackHighlight(any(), any(), any())
    }

    @Test
    fun `highlight, add, success`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicHighlighted(any())).thenReturn(false)
        whenever(userDataSource.highlightsCount).thenReturn(0)
        whenever(musicDataSource.addToHighlights(any(), any())).thenReturn(Observable.just(true))

        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertNoErrors()
            .assertValue { it is ToggleHighlightResult.Added }
            .dispose()

        verify(userDataSource).addToHighlights(any())
        verify(mixpanelDataSource).trackHighlight(any(), any(), any())
    }

    @Test
    fun `highlight, add, failure`() {
        whenever(reachabilityDataSource.networkAvailable).thenReturn(true)
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(userDataSource.isMusicHighlighted(any())).thenReturn(false)
        whenever(userDataSource.highlightsCount).thenReturn(0)
        whenever(musicDataSource.addToHighlights(any(), any())).thenReturn(Observable.just(false))

        sut.toggleHighlight(mock(), mixpanelButton, mixpanelSource)
            .test()
            .assertError { it is ToggleHighlightException.Failure }
            .dispose()

        verify(userDataSource, never()).addToHighlights(any())
        verify(mixpanelDataSource, never()).trackHighlight(any(), any(), any())
    }

    // Download

    @Test
    fun `download, not logged in`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(false)
        sut.toggleDownload(mock(), mixpanelButton, mixpanelSource, false)
            .test()
            .assertError { it is ToggleDownloadException.LoggedOut }
            .dispose()
    }

    @Test
    fun `download frozen premium-only song`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(false)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { isSong } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypePremium == PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline }
            .dispose()
    }

    @Test
    fun `download frozen limited song, unfreezes song if there is room`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).doReturn(5)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).doReturn(20)
        whenever(musicDataSource.markFrozenDownloads(any(), any())).thenReturn(Completable.complete())
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { title } doReturn "title"
            on { isSong } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isDownloadFrozen } doReturn true
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertValue(ToggleDownloadResult.ShowUnlockedToast("title"))
            .assertNoErrors()
            .dispose()
        verify(eventBus, times(1)).post(argWhere { it is EventDownload })
        verify(eventBus, times(1)).post(argWhere { it is EventDownloadsEdited })
    }

    @Test
    fun `download frozen limited song`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).doReturn(20)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).doReturn(20)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isSong } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isDownloadFrozen } doReturn true
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.DownloadFrozen }
            .dispose()
    }

    @Test
    fun `download premium-only song`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).doReturn(20)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).doReturn(20)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isSong } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypePremium == PremiumOnlyDownloadAlertViewType.Download }
            .dispose()
    }

    @Test
    fun `download limited song, no more downloads available`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(any())).doReturn(false)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isSong } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.ReachedLimit }
            .dispose()
    }

    @Test
    fun `download song, ask for deletion confirmation`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        val music = mock<AMResultItem> {
            on { isSong } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn true
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValue(ToggleDownloadResult.ConfirmMusicDeletion)
            .assertNoErrors()
            .dispose()
    }

    @Test
    fun `download song, complete`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        val music = mock<AMResultItem> {
            on { isSong } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn false
        }
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(music)).thenReturn(true)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValues(ToggleDownloadResult.DownloadStarted)
            .assertNoErrors()
            .assertComplete()
            .dispose()
        verify(mixpanelDataSource).trackDownloadToOffline(any(), any(), any())
        verify(musicDownloader).enqueueDownload(any())
        verify(inAppRating).incrementDownloadCount()
        verify(inAppRating).request()
        verify(adsManager).showInterstitial()
    }

    @Test
    fun `download album, no tracks`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        val music = mock<AMResultItem> {
            on { tracks } doReturn null
            on { isAlbum } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn false
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertNoValues()
            .assertNoErrors()
            .dispose()
    }

    @Test
    fun `download album, ask for deletion confirmation`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        val music = mock<AMResultItem> {
            on { isAlbum } doReturn true
            on { isDownloadCompleted } doReturn true
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValue(ToggleDownloadResult.ConfirmMusicDeletion)
            .assertNoErrors()
            .dispose()
    }

    @Test
    fun `download frozen premium-only album`() {
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(false)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "123"
            on { tracks } doReturn listOf(track)
            on { isAlbum } doReturn true
            on { isDownloadCompleted } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        whenever(premiumDownloadDataSource.getToBeDownloadedPremiumLimitedCount(music)).thenReturn(1)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypePremium == PremiumOnlyDownloadAlertViewType.DownloadFrozenOrPlayFrozenOffline }
            .dispose()
    }

    @Test
    fun `download frozen limited album, unfreezes tracks if there is room`() {
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).doReturn(5)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).doReturn(20)
        whenever(musicDataSource.markFrozenDownloads(any(), any())).thenReturn(Completable.complete())
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { title } doReturn "title"
            on { tracks } doReturn listOf(track)
            on { isAlbum } doReturn true
            on { isDownloadCompleted } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        whenever(premiumDownloadDataSource.getFrozenCount(music)).thenReturn(1)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertValue(ToggleDownloadResult.ShowUnlockedToast("title"))
            .assertNoErrors()
            .dispose()
        verify(eventBus).post(argWhere { it is EventDownload })
    }

    @Test
    fun `download frozen limited album`() {
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).doReturn(20)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).doReturn(20)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { tracks } doReturn listOf(track)
            on { isAlbum } doReturn true
            on { isDownloadCompleted } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        whenever(premiumDownloadDataSource.getFrozenCount(music)).thenReturn(1)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.DownloadFrozen }
            .dispose()
    }

    @Test
    fun `download premium-only album`() {
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).doReturn(20)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).doReturn(20)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { tracks } doReturn listOf(track)
            on { isAlbum } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Premium
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypePremium == PremiumOnlyDownloadAlertViewType.Download }
            .dispose()
    }

    @Test
    fun `download frozen limited album, has more than the allowed limit for premium-limited downloads`() {
        val limit = 20
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(any())).doReturn(false)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(limit)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { tracks } doReturn (0 until limit + 1).map { track }
            on { isAlbum } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
            on { isDownloaded } doReturn true
        }
        whenever(premiumDownloadDataSource.getFrozenCount(music)).thenReturn(1)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimitAlreadyDownloaded }
            .dispose()
    }

    @Test
    fun `download limited album, has more than the allowed limit for premium-limited downloads`() {
        val limit = 20
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(any())).doReturn(false)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(limit)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { tracks } doReturn (0 until limit + 1).map { track }
            on { isAlbum } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.DownloadAlbumLargerThanLimit }
            .dispose()
    }

    @Test
    fun `download limited album, no more downloads available`() {
        val limit = 20
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(any())).doReturn(false)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(limit)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(limit)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { tracks } doReturn listOf(track)
            on { isAlbum } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.ReachedLimit }
            .dispose()
    }

    @Test
    fun `download album, complete`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        val track = mock<AMResultItem> {
            on { itemId } doReturn "12"
        }
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isAlbum } doReturn true
            on { isDownloadCompletedIndependentlyFromType } doReturn false
            on { tracks } doReturn listOf(track, track, track)
        }
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(music)).thenReturn(true)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValues(ToggleDownloadResult.DownloadStarted)
            .assertNoErrors()
            .assertComplete()
            .dispose()
        verify(musicDownloader).cacheImages(any())
        verify(apiDownloads).addDownload(any(), any())
        verify(mixpanelDataSource).trackDownloadToOffline(any(), any(), any())
        verify(musicDownloader, times(music.tracks!!.size)).enqueueDownload(any())
        verify(trackingDataSource).trackGA(any(), any(), any())
        verify(inAppRating).incrementDownloadCount()
        verify(inAppRating).request()
        verify(adsManager).showInterstitial()
    }

    @Test
    fun `download playlist, unsubscribed`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(false)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isPlaylist } doReturn true
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertNoValues()
            .assertError(ToggleDownloadException.Unsubscribed(InAppPurchaseMode.PlaylistDownload))
            .dispose()
    }

    @Test
    fun `download playlist, ask for deletion confirmation`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isPlaylist } doReturn true
            on { tracks } doReturn null
            on { isDownloaded } doReturn true
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValue(ToggleDownloadResult.ConfirmPlaylistDeletion)
            .assertNoErrors()
            .dispose()
    }

    @Test
    fun `download limited playlist, no more downloads available`() {
        val limit = 20
        val track = mock<AMResultItem> {
            on { itemId } doReturn "1"
        }
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(any())).doReturn(false)
        whenever(premiumDownloadDataSource.premiumLimitedUnfrozenDownloadCount).thenReturn(limit)
        whenever(premiumDownloadDataSource.premiumDownloadLimit).thenReturn(limit)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { tracks } doReturn listOf(track)
            on { isPlaylist } doReturn true
            on { downloadType } doReturn AMResultItem.MusicDownloadType.Limited
        }
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, retry = false, skipFrozenCheck = false)
            .test()
            .assertNoValues()
            .assertError { (it as ToggleDownloadException.ShowPremiumDownload).model.alertTypeLimited == PremiumLimitedDownloadAlertViewType.ReachedLimit }
            .dispose()
    }

    @Test
    fun `download playlist, API call succeeds`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isPlaylist } doReturn true
            on { tracks } doReturn null
            on { isDownloaded } doReturn false
        }
        val track = mock<AMResultItem> {
            on { isGeoRestricted } doReturn false
        }
        val apiPlaylist = mock<AMResultItem> {
            on { tracks } doReturn listOf(track, track, track)
        }
        whenever(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.just(apiPlaylist))
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValues(ToggleDownloadResult.StartedBlockingAPICall, ToggleDownloadResult.EndedBlockingAPICall, ToggleDownloadResult.ConfirmPlaylistDownload(0)) // I wanted to see that there are 3 tracks but can't figure out how to simulate that with the mocked AMResultItem
            .assertNoErrors()
            .dispose()
        verify(musicDataSource).getPlaylistInfo(any())
    }

    @Test
    fun `download playlist, API call fails`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isPlaylist } doReturn true
            on { tracks } doReturn null
            on { isDownloaded } doReturn false
        }
        whenever(musicDataSource.getPlaylistInfo(any())).thenReturn(Observable.error(Exception("")))
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValues(ToggleDownloadResult.StartedBlockingAPICall, ToggleDownloadResult.EndedBlockingAPICall)
            .assertError(ToggleDownloadException.FailedDownloadingPlaylist)
            .dispose()
        verify(musicDataSource).getPlaylistInfo(any())
    }

    @Test
    fun `download playlist, succesful`() {
        whenever(userDataSource.isLoggedIn()).thenReturn(true)
        whenever(premiumDataSource.isPremium).thenReturn(true)
        val track = mock<AMResultItem> {
            on { itemId } doReturn "12"
        }
        val music = mock<AMResultItem> {
            on { itemId } doReturn "1"
            on { isPlaylist } doReturn true
            on { tracks } doReturn listOf(track, track, track)
            on { isDownloaded } doReturn false
        }
        whenever(premiumDownloadDataSource.canDownloadMusicBasedOnPremiumLimitedCount(music)).thenReturn(true)
        sut.toggleDownload(music, mixpanelButton, mixpanelSource, false)
            .test()
            .assertValues(ToggleDownloadResult.DownloadStarted)
            .assertNoErrors()
            .assertComplete()
            .dispose()
        verify(mixpanelDataSource).trackDownloadToOffline(any(), any(), any())
        verify(musicDownloader).cacheImages(any())
        verify(trackingDataSource).trackGA(any(), any(), any())
        verify(inAppRating).incrementDownloadCount()
        verify(inAppRating).request()
        verify(adsManager).showInterstitial()
    }
}
