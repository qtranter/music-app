package com.audiomack.data.housekeeping

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.data.api.ArtistsDataSource
import com.audiomack.data.remotevariables.RemoteVariablesProvider
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class MusicSyncUseCaseTest {

    private lateinit var useCase: MusicSyncUseCaseImpl

    @Mock private lateinit var artistsDataSource: ArtistsDataSource
    @Mock private lateinit var housekeepingDataSource: HousekeepingDataSource
    @Mock private lateinit var remoteVariablesProvider: RemoteVariablesProvider
    private val schedulersProvider: SchedulersProvider = TestSchedulersProvider()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = MusicSyncUseCaseImpl(
            housekeepingDataSource,
            artistsDataSource,
            remoteVariablesProvider,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `sync music, need to run`() {
        whenever(remoteVariablesProvider.syncCheckEnabled).thenReturn(true)
        whenever(artistsDataSource.findLoggedArtist()).thenReturn(Observable.just(mock()))
        whenever(housekeepingDataSource.syncMusic).thenReturn(Completable.complete())
        useCase.syncMusic()
            ?.test()
            ?.assertComplete()
            ?.assertNoErrors()
        verify(remoteVariablesProvider, times(1)).syncCheckEnabled
        verify(artistsDataSource, times(1)).findLoggedArtist()
        verify(housekeepingDataSource, times(1)).syncMusic
    }

    @Test
    fun `sync music, remote variable off`() {
        whenever(remoteVariablesProvider.syncCheckEnabled).thenReturn(false)
        val completable = useCase.syncMusic()
        completable?.test()
        assert(completable == null)
        verify(remoteVariablesProvider, times(1)).syncCheckEnabled
        verify(artistsDataSource, never()).findLoggedArtist()
        verify(housekeepingDataSource, never()).syncMusic
    }

    @Test
    fun `sync music, user not logged in`() {
        whenever(remoteVariablesProvider.syncCheckEnabled).thenReturn(true)
        whenever(artistsDataSource.findLoggedArtist()).thenReturn(Observable.error(Exception("No user found")))
        useCase.syncMusic()
            ?.test()
            ?.assertComplete()
            ?.assertNoErrors()
        verify(remoteVariablesProvider, times(1)).syncCheckEnabled
        verify(artistsDataSource, times(1)).findLoggedArtist()
        verify(housekeepingDataSource, never()).syncMusic
    }
}
