package com.audiomack.model

import com.audiomack.TestApplication
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestApplication::class
)
class MixpanelSourceTest {

    @Test
    fun testSerializationDeserialization() {
        val sourceA = MixpanelSource("A", "B", listOf(Pair("C", "aaa"), Pair("D", "123")))
        val sourceAString = sourceA.toJSON()
        val sourceB = MixpanelSource.fromJSON(sourceAString)
        val sourceBString = sourceB?.toJSON()
        Assert.assertEquals(sourceAString, sourceBString)
        Assert.assertEquals(sourceA.tab, sourceB?.tab)
        Assert.assertEquals(sourceA.page, sourceB?.page)
        Assert.assertTrue(sourceA.extraParams?.size == sourceB?.extraParams?.size)
        Assert.assertTrue(sourceB?.extraParams?.size == 2)
    }

    @Test
    fun testSerializationDeserializationEmptyList() {
        val sourceA = MixpanelSource("A", "B", emptyList())
        val sourceAString = sourceA.toJSON()
        val sourceB = MixpanelSource.fromJSON(sourceAString)
        val sourceBString = sourceB?.toJSON()
        Assert.assertEquals(sourceAString, sourceBString)
        Assert.assertEquals(sourceA.tab, sourceB?.tab)
        Assert.assertEquals(sourceA.page, sourceB?.page)
        Assert.assertEquals(sourceA.extraParams?.size, sourceB?.extraParams?.size)
        Assert.assertTrue(sourceB?.extraParams?.size == 0)
    }

    @Test
    fun testEdgeCases() {
        val jsonA: String? = "{asdas}"
        val sourceA = MixpanelSource.fromJSON(jsonA)
        Assert.assertNull(sourceA)

        val jsonB: String? = "{}"
        val sourceB = MixpanelSource.fromJSON(jsonB)
        Assert.assertNull(sourceB)

        val jsonC: String? = "{\"tab\":\"aaa\"}"
        val sourceC = MixpanelSource.fromJSON(jsonC)
        Assert.assertEquals(sourceC?.tab, "aaa")
        Assert.assertEquals(sourceC?.page, "")
        Assert.assertEquals(sourceC?.extraParams, null)
    }
}
