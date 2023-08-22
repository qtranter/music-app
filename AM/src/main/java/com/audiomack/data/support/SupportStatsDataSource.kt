package com.audiomack.data.support

interface SupportStatsDataSource {

    fun unreadTicketReplies(): Int

    fun setUnreadTicketReplies(count: Int)
}
