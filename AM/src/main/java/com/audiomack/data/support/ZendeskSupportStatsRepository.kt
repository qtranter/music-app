package com.audiomack.data.support

import com.audiomack.GENERAL_PREFERENCES_UNREAD_TICKET_REPLIES
import com.audiomack.MainApplication
import com.audiomack.utils.SecureSharedPreferences
import java.lang.NumberFormatException

class ZendeskSupportStatsRepository : SupportStatsDataSource {

    private val sharedPreferences = SecureSharedPreferences(MainApplication.context)
    private var cachedCount: Int? = null

    override fun unreadTicketReplies(): Int {
        cachedCount?.let {
            return it
        } ?: run {
            var countString = sharedPreferences.getString(GENERAL_PREFERENCES_UNREAD_TICKET_REPLIES)
            if (countString.isNullOrBlank()) {
                countString = "0"
            }
            val count = try { Integer.parseInt(countString) } catch (e: NumberFormatException) { 0 }
            cachedCount = count
            return count
        }
    }

    override fun setUnreadTicketReplies(count: Int) {
        cachedCount = count
        sharedPreferences.put(GENERAL_PREFERENCES_UNREAD_TICKET_REPLIES, count.toString())
    }
}
