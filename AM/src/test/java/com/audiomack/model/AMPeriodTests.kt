package com.audiomack.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AMPeriodTests {

    @Test
    fun testConversionFromApiValue() {
        AMPeriod.values().forEach {
            assertEquals(it, AMPeriod.fromApiValue(it.apiValue()))
        }
    }

    @Test
    fun testPeriodTodayConversion() {
        assertEquals(AMPeriod.Today, AMPeriod.fromApiValue("daily"))
    }

    @Test
    fun testPeriodWeekConversion() {
        assertEquals(AMPeriod.Week, AMPeriod.fromApiValue("weekly"))
    }

    @Test
    fun testPeriodMonthConversion() {
        assertEquals(AMPeriod.Month, AMPeriod.fromApiValue("monthly"))
    }

    @Test
    fun testPeriodYearConversion() {
        assertEquals(AMPeriod.Year, AMPeriod.fromApiValue("yearly"))
    }

    @Test
    fun testPeriodAllTimeConversion() {
        assertEquals(AMPeriod.AllTime, AMPeriod.fromApiValue("total"))
    }
}
