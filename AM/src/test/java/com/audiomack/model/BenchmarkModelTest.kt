package com.audiomack.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BenchmarkModelTest {

    @Test
    fun testSampling() {
        assertEquals(BenchmarkModel(milestone = 100).sampledValue(), 100)
        assertEquals(BenchmarkModel(milestone = 200).sampledValue(), 100)
        assertEquals(BenchmarkModel(milestone = 900).sampledValue(), 100)
        assertEquals(BenchmarkModel(milestone = 1_000).sampledValue(), 1_000)
        assertEquals(BenchmarkModel(milestone = 1_100).sampledValue(), 1_000)
        assertEquals(BenchmarkModel(milestone = 4_999).sampledValue(), 1_000)
        assertEquals(BenchmarkModel(milestone = 5_020).sampledValue(), 5_000)
        assertEquals(BenchmarkModel(milestone = 10_000).sampledValue(), 10_000)
        assertEquals(BenchmarkModel(milestone = 60_000).sampledValue(), 50_000)
        assertEquals(BenchmarkModel(milestone = 110_000).sampledValue(), 100_000)
        assertEquals(BenchmarkModel(milestone = 490_000).sampledValue(), 250_000)
        assertEquals(BenchmarkModel(milestone = 600_000).sampledValue(), 500_000)
        assertEquals(BenchmarkModel(milestone = 1_000_001).sampledValue(), 1_000_000)
        assertEquals(BenchmarkModel(milestone = 1_000_000_000).sampledValue(), 1_000_000_000)
        assertEquals(BenchmarkModel(milestone = 9_000_000_000_000).sampledValue(), 1_000_000_000)
    }

    @Test
    fun testNextMilestone() {
        assertEquals(BenchmarkModel(BenchmarkType.NONE).nextMilestone(), null)
        assertEquals(BenchmarkModel(BenchmarkType.VERIFIED).nextMilestone(), null)
        assertEquals(BenchmarkModel(BenchmarkType.TASTEMAKER).nextMilestone(), null)
        assertEquals(BenchmarkModel(BenchmarkType.AUTHENTICATED).nextMilestone(), null)
        assertEquals(BenchmarkModel(BenchmarkType.ON_AUDIOMACK).nextMilestone(), null)
        assertEquals(BenchmarkModel(BenchmarkType.PLAY, milestone = 70).nextMilestone(), "100")
        assertEquals(BenchmarkModel(BenchmarkType.PLAY, milestone = 900).nextMilestone(), "1K")
        assertEquals(BenchmarkModel(BenchmarkType.PLAY, milestone = 1_100).nextMilestone(), "5K")
        assertEquals(BenchmarkModel(BenchmarkType.PLAY, milestone = 5_020).nextMilestone(), "10K")
        assertEquals(BenchmarkModel(BenchmarkType.FAVORITE, milestone = 10_000).nextMilestone(), "50K")
        assertEquals(BenchmarkModel(BenchmarkType.FAVORITE, milestone = 60_000).nextMilestone(), "100K")
        assertEquals(BenchmarkModel(BenchmarkType.FAVORITE, milestone = 110_000).nextMilestone(), "250K")
        assertEquals(BenchmarkModel(BenchmarkType.FAVORITE, milestone = 490_000).nextMilestone(), "500K")
        assertEquals(BenchmarkModel(BenchmarkType.REPOST, milestone = 600_000).nextMilestone(), "1M")
        assertEquals(BenchmarkModel(BenchmarkType.REPOST, milestone = 1_000_001).nextMilestone(), "5M")
        assertEquals(BenchmarkModel(BenchmarkType.REPOST, milestone = 6_000_000).nextMilestone(), "10M")
        assertEquals(BenchmarkModel(BenchmarkType.REPOST, milestone = 20_000_000).nextMilestone(), "50M")
        assertEquals(BenchmarkModel(BenchmarkType.PLAYLIST, milestone = 70_000_000).nextMilestone(), "100M")
        assertEquals(BenchmarkModel(BenchmarkType.PLAYLIST, milestone = 490_000_000).nextMilestone(), "500M")
        assertEquals(BenchmarkModel(BenchmarkType.PLAYLIST, milestone = 999_999_999).nextMilestone(), "1B")
        assertEquals(BenchmarkModel(BenchmarkType.PLAYLIST, milestone = 1_000_000_000).nextMilestone(), null)
        assertEquals(BenchmarkModel(BenchmarkType.PLAYLIST, milestone = 9_000_000_000_000).nextMilestone(), null)
    }
}
