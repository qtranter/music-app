package com.audiomack.data.user

import com.audiomack.data.database.ArtistDAO
import com.audiomack.data.database.MusicDAO
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.model.AMArtist
import com.audiomack.model.AMResultItem
import com.audiomack.network.APIInterface
import com.audiomack.onesignal.OneSignalDataSource
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Date
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class UserRepositoryTest {

    @Mock private lateinit var userData: UserDataInterface
    @Mock private lateinit var userAPI: APIInterface.UserInterface
    @Mock private lateinit var authAPI: APIInterface.AuthenticationInterface
    @Mock private lateinit var settingsAPI: APIInterface.SettingsInterface
    @Mock private lateinit var artistDAO: ArtistDAO
    @Mock private lateinit var musicDAO: MusicDAO
    @Mock private lateinit var remoteVariablesProvider: RemoteVariablesProvider
    @Mock private lateinit var eventBus: EventBus
    @Mock private lateinit var oneSignalDataSource: OneSignalDataSource

    private lateinit var sut: UserRepository

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = UserRepository.getInstance(
            userData,
            userAPI,
            authAPI,
            settingsAPI,
            artistDAO,
            musicDAO,
            remoteVariablesProvider,
            eventBus,
            oneSignalDataSource
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
        UserRepository.destroy()
    }

    @Test
    fun `is tester hits remote vars`() {
        val tester = true
        whenever(remoteVariablesProvider.tester).thenReturn(tester)
        assert(sut.isTester() == tester)
        verify(remoteVariablesProvider).tester
    }

    @Test
    fun `can comment hits DAO`() {
        val can = true
        val artist = mock<AMArtist> {
            on { canComment } doReturn can
        }
        whenever(artistDAO.findSync()).thenReturn(artist)
        assert(sut.canComment() == can)
        verify(artistDAO).findSync()
    }

    @Test
    fun `avatar hits DAO`() {
        val image = "https://"
        val artist = mock<AMArtist> {
            on { smallImage } doReturn image
        }
        whenever(artistDAO.findSync()).thenReturn(artist)
        assert(sut.avatar == image)
        verify(artistDAO).findSync()
    }

    @Test
    fun `get user hits DAO`() {
        val artist = mock<AMArtist>()
        whenever(artistDAO.findSync()).thenReturn(artist)
        assert(sut.getUser() == artist)
        verify(artistDAO).findSync()
    }

    @Test
    fun `get user async hits DAO`() {
        val artist = mock<AMArtist>()
        whenever(artistDAO.find()).thenReturn(Observable.just(artist))
        sut.getUserAsync()
            .test()
            .assertValue(artist)
            .assertNoErrors()
        verify(artistDAO).find()
    }

    @Test
    fun `get offline downloads count hits DAO`() {
        val count = 10
        whenever(musicDAO.downloadsCount()).thenReturn(count)
        assert(sut.getOfflineDownloadsCount() == count)
        verify(musicDAO).downloadsCount()
    }

    @Test
    fun `get premium limited downloads count hits DAO`() {
        val count = 1
        whenever(musicDAO.premiumLimitedDownloadCount()).thenReturn(count)
        assert(sut.getPremiumLimitedDownloadsCount() == count)
        verify(musicDAO).premiumLimitedDownloadCount()
    }

    @Test
    fun `get premium only downloads count hits DAO`() {
        val count = 2
        whenever(musicDAO.premiumOnlyDownloadCount()).thenReturn(count)
        assert(sut.getPremiumOnlyDownloadsCount() == count)
        verify(musicDAO).premiumOnlyDownloadCount()
    }

    @Test
    fun `save account hits API`() {
        val slug = "matteinn"
        val artist = mock<AMArtist> {
            on { urlSlug } doReturn slug
        }
        whenever(userAPI.editUserAccountInfo(artist)).thenReturn(Single.just(artist))
        whenever(userAPI.editUserUrlSlug(artist)).thenReturn(Single.just(artist))
        sut.saveAccount(artist)
            .test()
            .assertValue(artist)
            .assertNoErrors()
        verify(userAPI).editUserAccountInfo(artist)
        verify(userAPI).editUserUrlSlug(artist)
    }

    @Test
    fun `is artist followed hits UserData`() {
        val artistId = "14lsakngfer"
        val followed = true
        whenever(userData.isArtistFollowed(artistId)).thenReturn(followed)
        assert(sut.isArtistFollowed(artistId) == followed)
        verify(userData).isArtistFollowed(artistId)
    }

    @Test
    fun `add artist to following hits UserData`() {
        val artistId = "14lsakngfer"
        sut.addArtistToFollowing(artistId)
        verify(userData).addArtistToFollowing(artistId)
    }

    @Test
    fun `remove artist from following hits UserData`() {
        val artistId = "14lsakngfer"
        sut.removeArtistFromFollowing(artistId)
        verify(userData).removeArtistFromFollowing(artistId)
    }

    @Test
    fun `is music favorited hits UserData`() {
        val music = mock<AMResultItem>()
        val highlighted = true
        whenever(userData.isItemFavorited(music)).thenReturn(highlighted)
        assert(sut.isMusicFavorited(music) == highlighted)
        verify(userData).isItemFavorited(music)
    }

    @Test
    fun `add to favorites hits UserData`() {
        val music = mock<AMResultItem>()
        sut.addMusicToFavorites(music)
        verify(userData).addItemToFavorites(music)
    }

    @Test
    fun `remove from favorites hits UserData`() {
        val music = mock<AMResultItem>()
        sut.removeMusicFromFavorites(music)
        verify(userData).removeItemFromFavorites(music)
    }

    @Test
    fun `has favorites hits UserData`() {
        val count = 3
        whenever(userData.favoritedItemsCount).thenReturn(count)
        assert(sut.hasFavorites == count > 0)
        verify(userData).favoritedItemsCount
    }

    @Test
    fun `is creator hits DAO`() {
        val count = 3
        val artist = mock<AMArtist> {
            on { uploadsCount } doReturn count
        }
        whenever(artistDAO.findSync()).thenReturn(artist)
        assert(sut.isContentCreator == count > 0)
        verify(artistDAO).findSync()
    }

    @Test
    fun `refresh user data hits API`() {
        val artist = mock<AMArtist>()
        whenever(userAPI.userData).thenReturn(Observable.just(artist))
        sut.refreshUserData()
            .test()
            .assertValue(artist)
            .assertNoErrors()
        verify(userAPI).userData
    }

    @Test
    fun `highlights count hits UserData`() {
        val music = mock<AMResultItem>()
        val count = 3
        whenever(userData.getHighlights()).thenReturn((0 until count).map { music })
        assert(sut.highlightsCount == count)
        verify(userData).getHighlights()
    }

    @Test
    fun `is music highlighted hits UserData`() {
        val music = mock<AMResultItem>()
        val highlighted = true
        whenever(userData.isItemHighlighted(music)).thenReturn(highlighted)
        assert(sut.isMusicHighlighted(music) == highlighted)
        verify(userData).isItemHighlighted(music)
    }

    @Test
    fun `add to highligths hits UserData`() {
        val music = mock<AMResultItem>()
        sut.addToHighlights(music)
        verify(userData).addItemToHighlights(music)
    }

    @Test
    fun `remove from highligths hits UserData`() {
        val music = mock<AMResultItem>()
        sut.removeFromHighlights(music)
        verify(userData).removeItemFromHighlights(music)
    }

    @Test
    fun `complete profile with age and gender hits API`() {
        val name = "Matteo"
        val birthday = Date()
        val gender = AMArtist.Gender.MALE
        whenever(userAPI.completeProfile(name, birthday, gender)).doReturn(Completable.complete())
        sut.completeProfile(name, birthday, gender)
            .test()
            .assertComplete()
            .assertNoErrors()
        verify(userAPI).completeProfile(name, birthday, gender)
    }

    @Test
    fun `save local artist hits DAO`() {
        val artist = mock<AMArtist>()
        whenever(artistDAO.save(artist)).doReturn(Single.just(artist))
        sut.saveLocalArtist(artist)
            .test()
            .assertValue(artist)
            .assertNoErrors()
        verify(artistDAO).save(artist)
    }

    @Test
    fun `oneSignalId fetched from oneSignalDataSource`() {
        val id = "123"
        whenever(oneSignalDataSource.playerId).doReturn(id)
        assert(sut.oneSignalId == id)
        verify(oneSignalDataSource).playerId
    }
}
