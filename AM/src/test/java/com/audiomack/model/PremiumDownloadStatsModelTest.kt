package com.audiomack.model

import org.junit.Test

class PremiumDownloadStatsModelTest {

    @Test
    fun `available count`() {
        PremiumDownloadStatsModel("", MixpanelSource.empty, 20, 5).also {
            assert(it.availableCount == 15)
        }
    }

    @Test
    fun `replace count`() {
        PremiumDownloadStatsModel("", MixpanelSource.empty, 20, 5).also {
            assert(it.replaceCount(27) == 12)
        }
    }
}
