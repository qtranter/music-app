package com.audiomack.data.support

import io.reactivex.Observable
import zendesk.configurations.Configuration

interface ZendeskDataSource {

    val cantLoginArticleId: Long

    fun getUnreadTicketsCount(): Observable<ZendeskUnreadTicketsData>

    fun getUIConfigs(): List<Configuration>

    val cachedUnreadTicketsCount: Int

    fun sendSupportTicket(whatText: String, howText: String, whenText: String, emailText: String, notesText: String): Observable<Boolean>

    fun trackIdentity()

    fun registerForPush(token: String? = null)

    fun logout()
}
