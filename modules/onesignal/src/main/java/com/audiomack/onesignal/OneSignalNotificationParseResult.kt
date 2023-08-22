package com.audiomack.onesignal

import android.net.Uri

data class OneSignalNotificationParseResult(
    val deeplinkUri: Uri,
    val info: TransactionalNotificationInfo
)