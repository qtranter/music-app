package com.audiomack.data.housekeeping

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.audiomack.rx.SchedulersProvider
import com.audiomack.rx.TestSchedulersProvider
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class HousekeepingUseCaseTest {

    private lateinit var useCase: HousekeepingUseCaseImpl

    @Mock private lateinit var context: Context
    @Mock private lateinit var housekeepingDataSource: HousekeepingDataSource
    private val schedulersProvider: SchedulersProvider = TestSchedulersProvider()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = HousekeepingUseCaseImpl(
            context,
            housekeepingDataSource,
            schedulersProvider
        )
    }

    @After
    fun clearMocks() {
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `run housekeeping, all succeeds`() {
        whenever(housekeepingDataSource.createNoMediaFiles(anyOrNull())).thenReturn(Completable.complete())
        whenever(housekeepingDataSource.houseekping).thenReturn(Completable.complete())

        useCase.runHousekeeping()
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(housekeepingDataSource).createNoMediaFiles(anyOrNull())
        verify(housekeepingDataSource).houseekping
    }

    @Test
    fun `run housekeeping, with errors but still completes`() {
        whenever(housekeepingDataSource.createNoMediaFiles(anyOrNull())).thenReturn(Completable.error(Exception("Unknown error for tests")))
        whenever(housekeepingDataSource.houseekping).thenReturn(Completable.error(Exception("Unknown error for tests")))

        useCase.runHousekeeping()
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(housekeepingDataSource).createNoMediaFiles(anyOrNull())
        verify(housekeepingDataSource).houseekping
    }
}
