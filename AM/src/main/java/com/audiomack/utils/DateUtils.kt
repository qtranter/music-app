package com.audiomack.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import timber.log.Timber

class DateUtils {

    fun getNotificationDate(string: String): Date? {
        try {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000000'", Locale.US).parse(string)
        } catch (e: Exception) {
            Timber.w(e)
        }
        return null
    }

    fun getArtistCreatedAsString(date: Date): String {
        val month = SimpleDateFormat("MMMM", Locale.US).format(date).take(3)
        val year = SimpleDateFormat("yy", Locale.US).format(date)
        return "$month '$year"
    }

    companion object {

        private var instance: DateUtils? = null

        @JvmStatic
        fun getItemDateAsString(milliseconds: Long): String {
            return DateFormat.getDateInstance(DateFormat.LONG, Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(milliseconds))
        }

        fun getAge(date: Date): Int {
            val (birthYear, birthDayOfYear) = Calendar.getInstance().let { cal ->
                cal.time = date
                Pair(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
            }
            val (thisYear, thisDayOfYear) = Calendar.getInstance().let { cal ->
                cal.time = Date()
                Pair(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
            }
            return thisYear - birthYear - (if (thisDayOfYear < birthDayOfYear) 1 else 0)
        }

        fun getYOB(date: Date): Int {
            return Calendar.getInstance().let { cal ->
                cal.time = date
                cal.get(Calendar.YEAR)
            }
        }

        @JvmStatic
        fun getInstance(): DateUtils {
            if (instance == null) {
                instance = DateUtils()
            }
            return instance!!
        }
    }
}
