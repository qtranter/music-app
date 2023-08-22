package com.audiomack.model

import android.content.Context
import com.audiomack.R

enum class AMPeriod {
    Today {
        override fun apiValue(): String = "daily"
        override fun humanValue(context: Context): String = context.getString(R.string.filters_section_timeframe_today)
    }, Week {
        override fun apiValue(): String = "weekly"
        override fun humanValue(context: Context): String = context.getString(R.string.filters_section_timeframe_week)
    }, Month {
        override fun apiValue(): String = "monthly"
        override fun humanValue(context: Context): String = context.getString(R.string.filters_section_timeframe_month)
    }, Year {
        override fun apiValue(): String = "yearly"
        override fun humanValue(context: Context): String = context.getString(R.string.filters_section_timeframe_year)
    }, AllTime {
        override fun apiValue(): String = "total"
        override fun humanValue(context: Context): String = context.getString(R.string.filters_section_timeframe_alltime)
    };

    abstract fun apiValue(): String
    abstract fun humanValue(context: Context): String

    companion object {
        fun fromApiValue(apiValue: String?): AMPeriod {
            return values().firstOrNull { it.apiValue() == apiValue } ?: Week
        }
    }
}
